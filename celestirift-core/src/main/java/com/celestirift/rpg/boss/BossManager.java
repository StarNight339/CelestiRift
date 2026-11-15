package com.celestirift.rpg.boss;

import com.celestirift.CelestiRift;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.item.ItemBuilder;
import com.celestirift.skill.load.ContentFiles;
import com.celestirift.util.Registries;
import com.celestirift.validate.Diagnostics;
import com.celestirift.validate.Suggest;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * bosses.yml: named mobs with extra health, a boss bar and skills on spawn, on a
 * timer, when hit, at health thresholds (phases) and on death. Bosses are tagged with
 * their id, so they survive chunk unloads/restarts and can be matched in drops.yml
 * with {@code boss: <id>}.
 */
public final class BossManager implements Listener, com.celestirift.util.Restorable {

    private static final List<String> KEYS = List.of("type", "name", "health", "damage", "bossbar", "glowing",
            "skills", "loot");

    /** A boss's inline {@code loot:} block, turned into a drop table by {@link com.celestirift.rpg.DropManager}. */
    public record Loot(String bossId, ContentFiles.File file, ConfigurationSection section) {}

    private final List<Loot> loot = new ArrayList<>();
    private static final List<String> SKILL_KEYS = List.of("on-spawn", "timer", "on-damaged", "phases", "on-death");

    /** A boss currently loaded in a world. */
    private static final class Active {
        final BossDef def;
        final LivingEntity entity;
        final Set<Integer> firedPhases = new HashSet<>();
        final @Nullable BossBar bar;
        final Set<UUID> viewers = new HashSet<>();
        long age;

        Active(BossDef def, LivingEntity entity) {
            this.def = def;
            this.entity = entity;
            this.bar = def.bar() == null ? null
                    : BossBar.bossBar(entity.name(), 1f, def.bar().color(), def.bar().overlay());
        }
    }

    private final CelestiRift plugin;
    private final NamespacedKey bossKey;
    private final Map<String, BossDef> defs = new LinkedHashMap<>();
    private final Map<UUID, Active> active = new HashMap<>();
    private @Nullable BukkitTask task;

    public BossManager(CelestiRift plugin) {
        this.plugin = plugin;
        this.bossKey = new NamespacedKey(plugin, "boss");
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
        for (Active a : active.values()) {
            hideBar(a);
        }
        active.clear();
    }

    public Optional<BossDef> get(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(defs.get(id.toLowerCase(Locale.ROOT)));
    }

    public Set<String> ids() {
        Set<String> out = new TreeSet<>();
        defs.values().forEach(d -> out.add(d.id()));
        return out;
    }

    /** The bosses.yml id of a boss entity, or null. */
    public @Nullable String bossId(Entity e) {
        return e.getPersistentDataContainer().get(bossKey, PersistentDataType.STRING);
    }

    /** Inline loot blocks from boss files (applied by DropManager). */
    public List<Loot> loot() {
        return List.copyOf(loot);
    }

    public int activeCount() {
        return active.size();
    }

    // ------------------------------------------------------------------ loading

    public int reload(Diagnostics diag) {
        defs.clear();
        loot.clear();
        for (ContentFiles.File file : ContentFiles.load(plugin, "bosses.yml", "bosses", diag)) {
            for (String id : file.yaml().getKeys(false)) {
                ConfigurationSection s = file.yaml().getConfigurationSection(id);
                if (s == null) {
                    diag.warn(file.path(), file.lines().key(id), id, "is not a boss (expected a block)");
                    continue;
                }
                BossDef def = parse(file, id, s, diag);
                if (def != null) {
                    if (defs.containsKey(id.toLowerCase(Locale.ROOT))) {
                        diag.warn(file.path(), file.lines().key(id), id,
                                "is defined twice - this definition replaces the earlier one");
                        loot.removeIf(l -> l.bossId().equalsIgnoreCase(id));
                    }
                    defs.put(id.toLowerCase(Locale.ROOT), def);
                    ConfigurationSection lootSection = s.getConfigurationSection("loot");
                    if (lootSection != null) {
                        loot.add(new Loot(def.id(), file, lootSection));
                    }
                }
            }
        }
        rebind();
        plugin.getLogger().info("Loaded " + defs.size() + " boss(es).");
        return defs.size();
    }

    @Override
    public Runnable snapshot() {
        var defsCopy = new LinkedHashMap<>(defs);
        var lootCopy = new ArrayList<>(loot);
        return () -> {
            defs.clear();
            defs.putAll(defsCopy);
            loot.clear();
            loot.addAll(lootCopy);
            rebind();
        };
    }

    /** Re-attach every loaded boss entity to the current definitions. */
    private void rebind() {
        for (Active a : new ArrayList<>(active.values())) {
            hideBar(a);
            active.remove(a.entity.getUniqueId());
            attach(a.entity);
        }
        // Bosses in chunks that were loaded before the plugin (or before this reload).
        for (var world : Bukkit.getWorlds()) {
            for (LivingEntity le : world.getLivingEntities()) {
                if (bossId(le) != null && !active.containsKey(le.getUniqueId())) {
                    attach(le);
                }
            }
        }
    }

    private @Nullable BossDef parse(ContentFiles.File file, String id, ConfigurationSection s, Diagnostics diag) {
        for (String key : s.getKeys(false)) {
            if (!KEYS.contains(key.toLowerCase(Locale.ROOT))) {
                diag.warn(file.path(), file.lines().find(id, key + ":"), id,
                        "unknown key '" + key + "' (ignored)", Suggest.closest(key, KEYS));
            }
        }
        String typeName = s.getString("type", "ZOMBIE");
        EntityType type = Registries.entityType(typeName)
                .filter(t -> t.isAlive() && t.isSpawnable() && t != EntityType.PLAYER).orElse(null);
        if (type == null) {
            diag.error(file.path(), file.lines().find(id, "type"), id, "unknown or unspawnable mob type '"
                    + typeName + "' - boss skipped");
            return null;
        }
        BossDef.Bar bar = null;
        ConfigurationSection b = s.getConfigurationSection("bossbar");
        if (b != null) {
            bar = new BossDef.Bar(
                    enumOr(BossBar.Color.class, b.getString("color"), BossBar.Color.RED),
                    overlay(b.getString("style")),
                    b.getDouble("radius", 48));
        }
        ConfigurationSection sk = s.getConfigurationSection("skills");
        List<String> onSpawn = List.of(), onDeath = List.of();
        List<BossDef.Timer> timers = new ArrayList<>();
        List<BossDef.Chance> onDamaged = new ArrayList<>();
        List<BossDef.Phase> phases = new ArrayList<>();
        if (sk != null) {
            for (String key : sk.getKeys(false)) {
                if (!SKILL_KEYS.contains(key.toLowerCase(Locale.ROOT))) {
                    diag.warn(file.path(), file.lines().find(id, key + ":"), id,
                            "unknown skills key '" + key + "' (ignored)", Suggest.closest(key, SKILL_KEYS));
                }
            }
            onSpawn = skillList(sk, "on-spawn");
            onDeath = skillList(sk, "on-death");
            for (Map<?, ?> m : sk.getMapList("timer")) {
                timers.add(new BossDef.Timer((int) Math.max(1, num(m.get("every"), 100)), str(m.get("skill"))));
            }
            for (Map<?, ?> m : sk.getMapList("on-damaged")) {
                onDamaged.add(new BossDef.Chance(str(m.get("skill")), num(m.get("chance"), 1)));
            }
            for (Map<?, ?> m : sk.getMapList("phases")) {
                phases.add(new BossDef.Phase(fraction(m.get("below")), str(m.get("skill"))));
            }
        }
        BossDef def = new BossDef(id, type, s.getString("name", id), Math.max(1, s.getDouble("health", 100)),
                s.contains("damage") ? s.getDouble("damage") : null, bar, s.getBoolean("glowing", false),
                onSpawn, timers, onDamaged, phases, onDeath);
        checkSkills(file, id, def, diag);
        return def;
    }

    private void checkSkills(ContentFiles.File file, String id, BossDef def, Diagnostics diag) {
        List<String> all = new ArrayList<>(def.onSpawn());
        all.addAll(def.onDeath());
        def.timers().forEach(t -> all.add(t.skill()));
        def.onDamaged().forEach(c -> all.add(c.skill()));
        def.phases().forEach(p -> all.add(p.skill()));
        for (String skill : all) {
            if (skill.isBlank() || plugin.skills().getSkill(skill).isEmpty()) {
                diag.warn(file.path(), file.lines().find(id, skill.isBlank() ? "skill" : skill), id,
                        "uses unknown skill '" + skill + "'", Suggest.closest(skill, plugin.skills().skillNames()));
            }
        }
    }

    private static List<String> skillList(ConfigurationSection s, String key) {
        if (s.isList(key)) {
            return s.getStringList(key);
        }
        String one = s.getString(key);
        return one == null ? List.of() : List.of(one);
    }

    private static String str(@Nullable Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static double num(@Nullable Object o, double def) {
        try {
            return o == null ? def : Double.parseDouble(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** "50%" or 0.5 → 0.5 */
    private static double fraction(@Nullable Object o) {
        String s = str(o).trim();
        if (s.endsWith("%")) {
            return num(s.substring(0, s.length() - 1), 50) / 100.0;
        }
        double v = num(s, 0.5);
        return v > 1 ? v / 100.0 : v;
    }

    private static <E extends Enum<E>> E enumOr(Class<E> type, @Nullable String name, E def) {
        if (name == null) return def;
        try {
            return Enum.valueOf(type, name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return def;
        }
    }

    private static BossBar.Overlay overlay(@Nullable String style) {
        if (style == null) return BossBar.Overlay.PROGRESS;
        return switch (style.trim().toUpperCase(Locale.ROOT).replace("SEGMENTED_", "NOTCHED_")) {
            case "NOTCHED_6" -> BossBar.Overlay.NOTCHED_6;
            case "NOTCHED_10" -> BossBar.Overlay.NOTCHED_10;
            case "NOTCHED_12" -> BossBar.Overlay.NOTCHED_12;
            case "NOTCHED_20" -> BossBar.Overlay.NOTCHED_20;
            default -> BossBar.Overlay.PROGRESS;
        };
    }

    // ------------------------------------------------------------------ spawning

    public @Nullable LivingEntity spawn(String id, Location location) {
        BossDef def = get(id).orElse(null);
        if (def == null || location.getWorld() == null) {
            return null;
        }
        Entity spawned = location.getWorld().spawnEntity(location, def.type());
        if (!(spawned instanceof LivingEntity boss)) {
            spawned.remove();
            return null;
        }
        boss.getPersistentDataContainer().set(bossKey, PersistentDataType.STRING, def.id());
        boss.customName(ItemBuilder.mm(def.name()));
        boss.setCustomNameVisible(true);
        keepLoaded(boss);
        boss.setPersistent(true);
        boss.setGlowing(def.glowing());
        var maxHealth = boss.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(def.health());
            boss.setHealth(def.health());
        }
        var attack = boss.getAttribute(Attribute.ATTACK_DAMAGE);
        if (def.damage() != null && attack != null) {
            attack.setBaseValue(def.damage());
        }
        Active a = attach(boss);
        if (a != null) {
            cast(a, def.onSpawn(), null);
        }
        return boss;
    }

    /** Never despawn (needs Paper 26.3 build 252+), and stay even on peaceful difficulty. */
    private static void keepLoaded(LivingEntity boss) {
        if (boss instanceof Mob mob) {
            mob.setPersistenceRequired(true);
            mob.setDespawnInPeacefulOverride(net.kyori.adventure.util.TriState.FALSE);
        }
    }

    /** Start tracking a boss entity (spawned, or loaded from disk). */
    private @Nullable Active attach(LivingEntity entity) {
        BossDef def = get(bossId(entity)).orElse(null);
        if (def == null || entity.isDead()) {
            return null;
        }
        Active a = new Active(def, entity);
        double frac = fraction(entity);
        for (int i = 0; i < def.phases().size(); i++) {
            if (frac < def.phases().get(i).below()) {
                a.firedPhases.add(i); // already past this phase (e.g. reloaded mid-fight)
            }
        }
        active.put(entity.getUniqueId(), a);
        return a;
    }

    public int killAll() {
        int n = 0;
        for (Active a : new ArrayList<>(active.values())) {
            hideBar(a);
            a.entity.remove();
            n++;
        }
        active.clear();
        return n;
    }

    // ------------------------------------------------------------------ runtime

    private void tick() {
        for (Active a : new ArrayList<>(active.values())) {
            if (!a.entity.isValid()) {
                hideBar(a);
                active.remove(a.entity.getUniqueId());
                continue;
            }
            a.age++;
            for (BossDef.Timer t : a.def.timers()) {
                if (a.age % t.every() == 0) {
                    cast(a, List.of(t.skill()), currentTarget(a.entity));
                }
            }
            if (a.age % 10 == 0) {
                updateBar(a);
            }
        }
    }

    private void updateBar(Active a) {
        if (a.bar == null || a.def.bar() == null) {
            return;
        }
        a.bar.progress((float) Math.max(0, Math.min(1, fraction(a.entity))));
        double r2 = a.def.bar().radius() * a.def.bar().radius();
        Set<UUID> now = new HashSet<>();
        for (Player p : a.entity.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(a.entity.getLocation()) <= r2) {
                now.add(p.getUniqueId());
                if (a.viewers.add(p.getUniqueId())) {
                    p.showBossBar(a.bar);
                }
            }
        }
        for (UUID id : new ArrayList<>(a.viewers)) {
            if (!now.contains(id)) {
                a.viewers.remove(id);
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    p.hideBossBar(a.bar);
                }
            }
        }
    }

    private void hideBar(Active a) {
        if (a.bar == null) {
            return;
        }
        for (UUID id : a.viewers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.hideBossBar(a.bar);
            }
        }
        a.viewers.clear();
    }

    private static double fraction(LivingEntity e) {
        var max = e.getAttribute(Attribute.MAX_HEALTH);
        double m = max == null ? e.getHealth() : max.getValue();
        return m <= 0 ? 0 : e.getHealth() / m;
    }

    private static @Nullable Entity currentTarget(LivingEntity e) {
        return e instanceof Mob mob ? mob.getTarget() : null;
    }

    private void cast(Active a, List<String> skills, @Nullable Entity trigger) {
        for (String skill : skills) {
            if (!skill.isBlank()) {
                plugin.skills().cast(skill, new SkillMetadata(a.entity, trigger, a.entity.getLocation(), 1f));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamaged(EntityDamageByEntityEvent event) {
        Active a = active.get(event.getEntity().getUniqueId());
        if (a == null) {
            return;
        }
        Entity attacker = event.getDamager();
        if (attacker instanceof Projectile p && p.getShooter() instanceof Entity shooter) {
            attacker = shooter;
        }
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (BossDef.Chance c : a.def.onDamaged()) {
            if (rnd.nextDouble() < c.chance()) {
                cast(a, List.of(c.skill()), attacker);
            }
        }
        // Health is applied after this event; check phases next tick.
        Entity finalAttacker = attacker;
        Bukkit.getScheduler().runTask(plugin, () -> checkPhases(a, finalAttacker));
    }

    private void checkPhases(Active a, @Nullable Entity attacker) {
        if (!a.entity.isValid()) {
            return;
        }
        double frac = fraction(a.entity);
        for (int i = 0; i < a.def.phases().size(); i++) {
            BossDef.Phase p = a.def.phases().get(i);
            if (frac < p.below() && a.firedPhases.add(i)) {
                cast(a, List.of(p.skill()), attacker);
            }
        }
        updateBar(a);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        Active a = active.remove(event.getEntity().getUniqueId());
        if (a == null) {
            return;
        }
        cast(a, a.def.onDeath(), event.getEntity().getKiller());
        hideBar(a);
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity e : event.getEntities()) {
            if (e instanceof LivingEntity le && bossId(le) != null && !active.containsKey(le.getUniqueId())) {
                attach(le);
            }
        }
    }
}

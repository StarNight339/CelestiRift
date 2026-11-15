package com.celestirift.aura;

import com.celestirift.CelestiRift;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.util.Registries;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Buffs, debuffs, stacks and shields on entities ({@code aura} mechanic).
 *
 * <p>Who casts the aura's skills:
 * <ul>
 *   <li>{@code ontick}, {@code onend}: the entity that applied the aura, with
 *       {@code @trigger} = the holder (damage over time, a mark that explodes on expiry)</li>
 *   <li>{@code onhit}: the holder, {@code @trigger} = what it hit (empowered attacks)</li>
 *   <li>{@code ondamaged}: the holder, {@code @trigger} = the attacker (thorns, counters)</li>
 * </ul>
 * Auras live in memory: they end on death, logout and restart.
 */
public final class AuraManager implements Listener {

    /** One aura on one entity. */
    public static final class Instance {
        final AuraSpec spec;
        final UUID source;
        final float power;
        int stacks;
        int ticksLeft;
        int age;
        double shield;
        boolean hasShield; // ends when the shield is used up

        Instance(AuraSpec spec, UUID source, float power) {
            this.spec = spec;
            this.source = source;
            this.power = power;
        }

        public int stacks() { return stacks; }
        public int ticksLeft() { return ticksLeft; }
        public double shield() { return shield; }
        public AuraSpec spec() { return spec; }
    }

    private final CelestiRift plugin;
    private final Map<UUID, Map<String, Instance>> auras = new HashMap<>();
    private @Nullable BukkitTask task;
    private boolean dispatching; // onhit/ondamaged skills must not re-trigger themselves

    public AuraManager(CelestiRift plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    /** Remove everything (speed modifiers are transient, but clean up anyway). */
    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
        for (UUID id : new ArrayList<>(auras.keySet())) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) {
                clear(e, false);
            }
        }
        auras.clear();
    }

    // ------------------------------------------------------------------ queries

    public @Nullable Instance get(Entity holder, String id) {
        Map<String, Instance> map = auras.get(holder.getUniqueId());
        return map == null ? null : map.get(id.toLowerCase(Locale.ROOT));
    }

    public int stacks(Entity holder, String id) {
        Instance i = get(holder, id);
        return i == null ? 0 : i.stacks;
    }

    public List<Instance> all(Entity holder) {
        Map<String, Instance> map = auras.get(holder.getUniqueId());
        return map == null ? List.of() : List.copyOf(map.values());
    }

    /** For expressions: {@code <target.aura.mark>} / {@code .stacks}, {@code .time} (seconds), {@code .shield}. */
    public double variable(Entity holder, String path) {
        int dot = path.indexOf('.');
        String id = dot < 0 ? path : path.substring(0, dot);
        String field = dot < 0 ? "stacks" : path.substring(dot + 1);
        Instance i = get(holder, id);
        if (i == null) {
            return 0;
        }
        return switch (field) {
            case "time" -> i.ticksLeft / 20.0;
            case "shield" -> i.shield;
            default -> i.stacks;
        };
    }

    // ------------------------------------------------------------------ apply / remove

    public void apply(AuraSpec spec, SkillMetadata meta, LivingEntity holder) {
        Map<String, Instance> map = auras.computeIfAbsent(holder.getUniqueId(), k -> new LinkedHashMap<>());
        Instance inst = map.get(spec.id());
        int duration = (int) Math.max(1, Math.round(spec.duration().get(meta, holder)));
        double shield = Math.max(0, spec.shield().get(meta, holder)) * meta.power();
        if (inst == null) {
            inst = new Instance(spec, meta.caster().getUniqueId(), meta.power());
            inst.stacks = Math.min(spec.maxStacks(), spec.stacks());
            inst.ticksLeft = duration;
            inst.shield = shield;
            map.put(spec.id(), inst);
        } else {
            inst.stacks = Math.min(spec.maxStacks(), inst.stacks + spec.stacks());
            if (spec.refresh()) {
                inst.ticksLeft = Math.max(inst.ticksLeft, duration);
            }
            inst.shield += shield;
        }
        inst.hasShield |= shield > 0;
        updateSpeed(holder, inst);
    }

    /**
     * @param id     aura id, or null for all (optionally only of {@code type})
     * @param type   "buff"/"debuff" filter, or null
     * @param stacks stacks to remove (0 = all)
     * @param runEnd cast the aura's onend skill
     * @return number of auras changed
     */
    public int remove(Entity holder, @Nullable String id, @Nullable String type, int stacks, boolean runEnd) {
        Map<String, Instance> map = auras.get(holder.getUniqueId());
        if (map == null) {
            return 0;
        }
        int n = 0;
        for (Instance inst : new ArrayList<>(map.values())) {
            if (id != null && !inst.spec.id().equals(id.toLowerCase(Locale.ROOT))) continue;
            if (type != null && !inst.spec.type().equals(type)) continue;
            n++;
            if (stacks > 0 && inst.stacks > stacks) {
                inst.stacks -= stacks;
                if (holder instanceof LivingEntity le) updateSpeed(le, inst);
            } else {
                end(holder, inst, runEnd);
            }
        }
        return n;
    }

    private void clear(Entity holder, boolean runEnd) {
        Map<String, Instance> map = auras.get(holder.getUniqueId());
        if (map != null) {
            for (Instance inst : new ArrayList<>(map.values())) {
                end(holder, inst, runEnd);
            }
        }
    }

    private void end(Entity holder, Instance inst, boolean runEnd) {
        Map<String, Instance> map = auras.get(holder.getUniqueId());
        if (map != null) {
            map.remove(inst.spec.id());
            if (map.isEmpty()) {
                auras.remove(holder.getUniqueId());
            }
        }
        inst.stacks = 0;
        if (holder instanceof LivingEntity le) {
            updateSpeed(le, inst);
        }
        if (runEnd) {
            castFromSource(inst.spec.onEnd(), inst, holder);
        }
    }

    // ------------------------------------------------------------------ ticking

    private void tick() {
        for (Iterator<Map.Entry<UUID, Map<String, Instance>>> it = auras.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            Entity holder = Bukkit.getEntity(entry.getKey());
            if (holder == null || !holder.isValid() || holder.isDead()) {
                it.remove(); // unloaded or gone: drop silently
                continue;
            }
            for (Instance inst : new ArrayList<>(entry.getValue().values())) {
                inst.age++;
                inst.ticksLeft--;
                if (inst.spec.interval() > 0 && inst.age % inst.spec.interval() == 0) {
                    castFromSource(inst.spec.onTick(), inst, holder);
                }
                if (inst.spec.particle() != null && inst.age % 5 == 0) {
                    Registries.particle(inst.spec.particle()).ifPresent(p -> spawnParticle(holder, p));
                }
                if (inst.ticksLeft <= 0 && entry.getValue().containsKey(inst.spec.id())) {
                    pendingEnds.add(new Object[]{holder, inst});
                }
            }
        }
        // End outside the iteration (onend skills may apply new auras).
        for (Object[] p : pendingEnds) {
            end((Entity) p[0], (Instance) p[1], true);
        }
        pendingEnds.clear();
    }

    private final List<Object[]> pendingEnds = new ArrayList<>();

    private static void spawnParticle(Entity holder, Particle particle) {
        if (particle.getDataType() != Void.class) {
            return; // particles that need extra data are not supported here
        }
        holder.getWorld().spawnParticle(particle, holder.getLocation().add(0, holder.getHeight() / 2, 0),
                4, 0.3, holder.getHeight() / 3, 0.3, 0.01);
    }

    private void castFromSource(@Nullable String skill, Instance inst, Entity holder) {
        if (skill == null) {
            return;
        }
        Entity source = Bukkit.getEntity(inst.source);
        if (source == null || !source.isValid()) {
            return; // the caster is gone (logged out / died): its DoT stops
        }
        plugin.skills().cast(skill, new SkillMetadata(source, holder, holder.getLocation(), inst.power));
    }

    private void castFromHolder(@Nullable String skill, Instance inst, Entity holder, Entity trigger) {
        if (skill == null || dispatching) {
            return;
        }
        dispatching = true;
        try {
            plugin.skills().cast(skill, new SkillMetadata(holder, trigger, holder.getLocation(), inst.power));
        } finally {
            dispatching = false;
        }
    }

    // ------------------------------------------------------------------ modifiers

    private void updateSpeed(LivingEntity holder, Instance inst) {
        if (inst.spec.speed() == 0) {
            return;
        }
        var attr = holder.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr == null) {
            return;
        }
        NamespacedKey key = new NamespacedKey(plugin, "aura_" + inst.spec.id().replaceAll("[^a-z0-9_.-]", "_"));
        attr.removeModifier(key);
        if (inst.stacks > 0) {
            attr.addTransientModifier(new AttributeModifier(key, inst.spec.speed() * inst.stacks,
                    AttributeModifier.Operation.ADD_SCALAR));
        }
    }

    private static @Nullable Entity attacker(EntityDamageByEntityEvent event) {
        Entity d = event.getDamager();
        if (d instanceof Projectile p && p.getShooter() instanceof Entity shooter) {
            return shooter;
        }
        return d;
    }

    /** damage-dealt (attacker) and damage-taken (victim) multipliers. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Entity attacker = attacker(event);
        double mult = 1;
        if (attacker != null) {
            for (Instance i : all(attacker)) {
                mult += i.spec.damageDealt() * i.stacks;
            }
        }
        double taken = 1;
        for (Instance i : all(event.getEntity())) {
            taken += i.spec.damageTaken() * i.stacks;
        }
        double total = Math.max(0, mult) * Math.max(0, taken);
        if (total != 1) {
            event.setDamage(event.getDamage() * total);
        }
    }

    /** Shields absorb damage from any source. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        Entity victim = event.getEntity();
        double finalDamage = event.getFinalDamage();
        if (finalDamage <= 0) {
            return;
        }
        double remaining = finalDamage;
        for (Instance i : all(victim)) {
            if (i.shield <= 0) continue;
            double absorbed = Math.min(i.shield, remaining);
            i.shield -= absorbed;
            remaining -= absorbed;
            if (i.hasShield && i.shield <= 0.001) {
                end(victim, i, true); // shield broken
            }
            if (remaining <= 0) break;
        }
        if (remaining < finalDamage) {
            event.setDamage(event.getDamage() * (remaining / finalDamage));
        }
    }

    /** onhit / ondamaged skills, after the hit went through. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (dispatching) {
            return;
        }
        Entity attacker = attacker(event);
        Entity victim = event.getEntity();
        if (attacker != null) {
            for (Instance i : all(attacker)) {
                if (i.spec.onHit() == null) continue;
                castFromHolder(i.spec.onHit(), i, attacker, victim);
                if (i.spec.onHitConsume() > 0) {
                    remove(attacker, i.spec.id(), null, i.spec.onHitConsume(), true);
                }
            }
        }
        if (attacker != null) {
            for (Instance i : all(victim)) {
                castFromHolder(i.spec.onDamaged(), i, victim, attacker);
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        clear(event.getEntity(), false);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clear(event.getPlayer(), false);
    }
}

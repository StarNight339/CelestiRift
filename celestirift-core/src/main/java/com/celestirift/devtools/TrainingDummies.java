package com.celestirift.devtools;

import com.celestirift.CelestiRift;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Training dummies for testing skills: Mannequins ({@code /cr dummy spawn}) that never die
 * and report damage, healing and DPS on the action bar of players nearby.
 *
 * <p>Role decides how targeting treats them ({@link com.celestirift.skill.CombatRules}):
 * <ul>
 *   <li>{@code enemy}: hit by {@code filter: enemies} (AoE); heals back to full after 3 s idle</li>
 *   <li>{@code ally}: hit by {@code filter: allies} (heals), never harmed by players; resets to 50% HP</li>
 *   <li>{@code player}: acts like another player: an enemy with {@code combat.pvp: true}, an ally without</li>
 * </ul>
 * Mannequins without the tag (decoration) are never targeted by skills.
 */
public final class TrainingDummies implements Listener {

    public enum Role { ENEMY, ALLY, PLAYER }

    private static final long IDLE_RESET_MS = 3000;
    private static final double REPORT_RADIUS = 24;

    /** Live stats per dummy, from health changes (catches setHealth heals too). */
    private static final class Stats {
        double lastHealth;
        double damage;
        double healing;
        long firstChange;
        long lastChange;
    }

    private final CelestiRift plugin;
    private final NamespacedKey roleKey;
    private final Map<UUID, Stats> stats = new HashMap<>();
    private @Nullable BukkitTask task;

    public TrainingDummies(CelestiRift plugin) {
        this.plugin = plugin;
        this.roleKey = new NamespacedKey(plugin, "dummy");
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    // ------------------------------------------------------------------ roles

    /** The dummy role of an entity, or null if it's not a training dummy. */
    public @Nullable Role role(Entity e) {
        if (!(e instanceof Mannequin)) {
            return null;
        }
        String r = e.getPersistentDataContainer().get(roleKey, PersistentDataType.STRING);
        if (r == null) {
            return null;
        }
        try {
            return Role.valueOf(r);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    // ------------------------------------------------------------------ spawn / clear

    public Mannequin spawn(Location at, Role role, double health, @Nullable Player skinOf) {
        return at.getWorld().spawn(at, Mannequin.class, m -> {
            m.getPersistentDataContainer().set(roleKey, PersistentDataType.STRING, role.name());
            var max = m.getAttribute(Attribute.MAX_HEALTH);
            if (max != null) {
                max.setBaseValue(health);
            }
            m.setHealth(role == Role.ALLY ? health / 2 : health);
            m.setImmovable(true);
            m.setPersistent(true);
            m.setCustomNameVisible(true);
            if (skinOf != null) {
                m.setProfile(ResolvableProfile.resolvableProfile(skinOf.getPlayerProfile()));
            }
            m.setDescription(Component.text("Training dummy · " + role.name().toLowerCase(Locale.ROOT),
                    color(role)));
            updateName(m);
        });
    }

    public int clear() {
        int n = 0;
        for (var world : Bukkit.getWorlds()) {
            for (Mannequin m : world.getEntitiesByClass(Mannequin.class)) {
                if (role(m) != null) {
                    m.remove();
                    n++;
                }
            }
        }
        stats.clear();
        return n;
    }

    private static NamedTextColor color(Role role) {
        return switch (role) {
            case ENEMY -> NamedTextColor.RED;
            case ALLY -> NamedTextColor.GREEN;
            case PLAYER -> NamedTextColor.GOLD;
        };
    }

    // ------------------------------------------------------------------ tracking

    /** Never let a dummy die: lethal hits leave it at 1 HP. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Mannequin m) || role(m) == null) {
            return;
        }
        // Only attacks, skills and fire count; the surroundings never hurt a dummy.
        switch (event.getCause()) {
            case SUFFOCATION, FALL, DROWNING, CRAMMING, CONTACT, HOT_FLOOR, FREEZE, STARVATION, WORLD_BORDER,
                 FLY_INTO_WALL -> {
                event.setCancelled(true);
                return;
            }
            default -> { }
        }
        plugin.debug(() -> "dummy " + m.getUniqueId().toString().substring(0, 8) + " took "
                + String.format(Locale.ROOT, "%.2f", event.getFinalDamage()) + " (" + event.getCause()
                + (event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent by
                        ? " by " + by.getDamager().getType() : "") + ")");
        if (event.getFinalDamage() >= m.getHealth()) {
            event.setDamage(Math.max(0, m.getHealth() - 1));
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (var world : Bukkit.getWorlds()) {
            for (Mannequin m : world.getEntitiesByClass(Mannequin.class)) {
                Role role = role(m);
                if (role == null) {
                    continue;
                }
                Stats s = stats.computeIfAbsent(m.getUniqueId(), id -> {
                    Stats n = new Stats();
                    n.lastHealth = m.getHealth();
                    return n;
                });
                double diff = m.getHealth() - s.lastHealth;
                if (Math.abs(diff) > 0.001) {
                    if (s.lastChange == 0 || now - s.lastChange > IDLE_RESET_MS) {
                        s.damage = 0;
                        s.healing = 0;
                        s.firstChange = now;
                    }
                    if (diff < 0) {
                        s.damage += -diff;
                    } else {
                        s.healing += diff;
                    }
                    s.lastChange = now;
                    s.lastHealth = m.getHealth();
                    report(m, role, diff, s, now);
                    updateName(m);
                } else if (s.lastChange != 0 && now - s.lastChange > IDLE_RESET_MS) {
                    reset(m, role);
                    s.lastChange = 0;
                    s.lastHealth = m.getHealth();
                }
            }
        }
    }

    private void reset(Mannequin m, Role role) {
        double max = maxHealth(m);
        m.setHealth(role == Role.ALLY ? max / 2 : max);
        updateName(m);
    }

    private void report(Mannequin m, Role role, double diff, Stats s, long now) {
        double seconds = Math.max(0.05, (now - s.firstChange) / 1000.0);
        Component line = Component.text("[" + role.name().toLowerCase(Locale.ROOT) + " dummy] ", color(role))
                .append(Component.text(diff < 0 ? String.format(Locale.ROOT, "-%.1f", -diff)
                        : String.format(Locale.ROOT, "+%.1f", diff),
                        diff < 0 ? NamedTextColor.RED : NamedTextColor.GREEN))
                .append(Component.text(String.format(Locale.ROOT, "  ·  dmg %.1f (%.1f/s)  ·  heal %.1f (%.1f/s)",
                        s.damage, s.damage / seconds, s.healing, s.healing / seconds), NamedTextColor.GRAY));
        for (Player p : m.getLocation().getNearbyPlayers(REPORT_RADIUS)) {
            plugin.messages().actionBar(p, line); // through OrionRP's HUD when it is on
        }
    }

    private void updateName(LivingEntity m) {
        m.customName(Component.text(String.format(Locale.ROOT, "%.0f / %.0f ❤", m.getHealth(), maxHealth(m)),
                NamedTextColor.WHITE));
    }

    private static double maxHealth(LivingEntity e) {
        var a = e.getAttribute(Attribute.MAX_HEALTH);
        return a != null ? a.getValue() : 20;
    }
}

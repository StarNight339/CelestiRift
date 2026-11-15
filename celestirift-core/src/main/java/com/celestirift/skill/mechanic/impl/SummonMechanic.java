package com.celestirift.skill.mechanic.impl;

import com.celestirift.CelestiRift;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.TargetedLocationMechanic;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * {@code summon{type=ZOMBIE;amount=3;radius=2;duration=200;health=20;name="<red>Minion"}}
 * — spawns minions at each targeted location (pair it with {@code @casterLocation}
 * or {@code @targetLocation}).
 *
 * <p>Summons are tagged with their owner, so they won't attack the caster, and
 * they open combat on the skill's {@code @trigger} entity if there is one.
 * {@code duration} (ticks) despawns them again; 0 makes them permanent.
 * {@code onSummon} casts another skill <em>as</em> each summon.
 */
public final class SummonMechanic implements TargetedLocationMechanic {

    private final CelestiRift plugin;
    private final EntityType type;
    private final int amount;
    private final double radius;
    private final int duration;
    private final double health;
    private final String name;
    private final boolean baby;
    private final boolean glowing;
    private final boolean silent;
    private final boolean targetTrigger;
    private final String onSummon;

    public SummonMechanic(CelestiRift plugin, ConfigMap cfg) {
        this.plugin = plugin;
        this.type = resolveType(cfg.getString("ZOMBIE", "type", "mob", "m", "t"));
        this.amount = Math.max(1, cfg.getInt(1, "amount", "a", "count"));
        this.radius = Math.max(0.0, cfg.getDouble(1.5, "radius", "r", "spread"));
        this.duration = Math.max(0, cfg.getInt(0, "duration", "d", "ticks"));
        this.health = cfg.getDouble(0.0, "health", "hp");
        this.name = cfg.getString("", "name", "n");
        this.baby = cfg.getBoolean(false, "baby");
        this.glowing = cfg.getBoolean(false, "glowing", "glow");
        this.silent = cfg.getBoolean(false, "silent");
        this.targetTrigger = cfg.getBoolean(true, "target");
        this.onSummon = cfg.getString("", "onsummon", "summonskill");
    }

    /** Unknown or non-spawnable types fall back to ZOMBIE rather than killing the skill. */
    private static EntityType resolveType(String raw) {
        return com.celestirift.util.Registries.entityType(raw)
                .filter(t -> t.isSpawnable() && t.isAlive())
                .orElse(EntityType.ZOMBIE);
    }

    @Override
    public void cast(SkillMetadata meta, Location target) {
        if (target.getWorld() == null) {
            return;
        }
        for (int i = 0; i < amount; i++) {
            spawnOne(meta, scatter(target));
        }
    }

    /** A random point within {@code radius} of the target, on the same Y. */
    private Location scatter(Location target) {
        if (radius <= 0) {
            return target.clone();
        }
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double angle = rng.nextDouble(2 * Math.PI);
        double dist = radius * Math.sqrt(rng.nextDouble());
        return target.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
    }

    private void spawnOne(SkillMetadata meta, Location at) {
        Entity summon = at.getWorld().spawnEntity(at, type);
        plugin.summons().setOwner(summon, meta.caster().getUniqueId());

        if (!name.isBlank()) {
            summon.customName(MiniMessage.miniMessage().deserialize(name, com.celestirift.integration.Orion.tags()));
            summon.setCustomNameVisible(true);
        }
        summon.setGlowing(glowing);
        summon.setSilent(silent);
        if (baby && summon instanceof Ageable ageable) {
            ageable.setBaby();
        }
        if (health > 0 && summon instanceof LivingEntity le) {
            AttributeInstance max = le.getAttribute(Attribute.MAX_HEALTH);
            if (max != null) {
                max.setBaseValue(health);
                le.setHealth(Math.min(health, max.getValue()));
            }
        }
        if (targetTrigger && summon instanceof Mob mob) {
            LivingEntity victim = livingTrigger(meta);
            if (victim != null) {
                mob.setTarget(victim);
            }
        }
        if (duration > 0) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> despawn(summon), duration);
        }
        if (!onSummon.isBlank()) {
            plugin.skills().cast(onSummon,
                    new SkillMetadata(summon, meta.trigger(), summon.getLocation(), meta.power()));
        }
    }

    private static @Nullable LivingEntity livingTrigger(SkillMetadata meta) {
        return (meta.trigger() instanceof LivingEntity le && !le.equals(meta.caster())) ? le : null;
    }

    private static void despawn(Entity summon) {
        if (!summon.isValid()) {
            return;
        }
        Location at = summon.getLocation();
        at.getWorld().spawnParticle(Particle.POOF, at.add(0, 0.5, 0), 20, 0.3, 0.4, 0.3, 0.02);
        summon.remove();
    }
}

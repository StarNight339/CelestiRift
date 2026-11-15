package com.celestirift.skill.mechanic.impl;

import com.celestirift.CelestiRift;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.NumberValue;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.mechanic.TargetedEntityMechanic;
import com.celestirift.skill.CombatRules;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * {@code chain{damage=6;jumps=4;radius=6;decay=0.8}} - chain lightning: hits the target,
 * then jumps to the nearest enemy not hit yet, up to {@code jumps} targets. Each jump
 * deals {@code decay} times the previous damage and draws a jagged bolt.
 * {@code onhit} runs on every target ({@code @trigger} = it).
 */
public final class ChainMechanic implements TargetedEntityMechanic {

    private final CelestiRift plugin;
    private final NumberValue damage;
    private final int jumps;
    private final double radius;
    private final double decay;
    private final int delay;
    private final Color color;
    private final String onHit;

    public ChainMechanic(CelestiRift plugin, ConfigMap cfg) {
        this.plugin = plugin;
        this.damage = cfg.getNumber(4, "damage", "dmg", "amount");
        this.jumps = Math.max(1, cfg.getInt(4, "jumps", "targets", "bounces"));
        this.radius = cfg.getDouble(6, "radius", "r", "range");
        this.decay = cfg.getDouble(0.8, "decay");
        this.delay = Math.max(0, cfg.getInt(3, "delay"));
        this.color = parse(cfg.getString("#9be7ff", "color", "c"));
        this.onHit = cfg.getString("", "onhit");
    }

    private static Color parse(String hex) {
        try {
            return Color.fromRGB(Integer.parseInt(hex.replace("#", ""), 16));
        } catch (NumberFormatException e) {
            return Color.AQUA;
        }
    }

    @Override
    public void cast(SkillMetadata meta, Entity first) {
        if (!(first instanceof LivingEntity) || !CombatRules.canHarm(meta.caster(), first)) {
            return;
        }
        Location from = meta.caster() instanceof LivingEntity le ? le.getEyeLocation() : meta.caster().getLocation();
        hop(meta, from, (LivingEntity) first, 0, 1.0, new HashSet<>());
    }

    private void hop(SkillMetadata meta, Location from, LivingEntity target, int index, double mult, Set<UUID> hit) {
        if (!target.isValid() || !meta.caster().isValid()) {
            return;
        }
        hit.add(target.getUniqueId());
        Location to = target.getLocation().add(0, target.getHeight() * 0.6, 0);
        bolt(from, to);
        to.getWorld().playSound(to, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 1.6f);

        double dmg = Math.max(0, damage.get(meta, target)) * meta.power() * mult
                * CombatRules.damageMultiplier(meta.caster(), target);
        if (dmg > 0) {
            if (meta.caster() instanceof LivingEntity src) {
                target.damage(dmg, src);
            } else {
                target.damage(dmg);
            }
        }
        if (!onHit.isBlank()) {
            plugin.skills().cast(onHit, new SkillMetadata(meta.caster(), target, to, meta.power()));
        }
        if (index + 1 >= jumps) {
            return;
        }
        LivingEntity next = null;
        double best = Double.MAX_VALUE;
        double r = radius * meta.rangeMultiplier();
        for (Entity e : target.getWorld().getNearbyEntities(target.getLocation(), r, r, r)) {
            if (!(e instanceof LivingEntity le) || hit.contains(e.getUniqueId())
                    || !CombatRules.isEnemy(meta.caster(), e)) {
                continue;
            }
            double d = e.getLocation().distanceSquared(target.getLocation());
            if (d < best && d <= r * r) {
                best = d;
                next = le;
            }
        }
        if (next == null) {
            return;
        }
        LivingEntity n = next;
        Runnable go = () -> hop(meta, to, n, index + 1, mult * decay, hit);
        if (delay == 0) {
            go.run();
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, go, delay);
        }
    }

    /** A jagged line of dust between two points. */
    private void bolt(Location a, Location b) {
        if (a.getWorld() != b.getWorld()) {
            return;
        }
        Vector delta = b.toVector().subtract(a.toVector());
        double len = delta.length();
        int segments = Math.max(3, (int) (len / 1.2));
        Particle.DustOptions dust = new Particle.DustOptions(color, 0.9f);
        Location prev = a.clone();
        java.util.concurrent.ThreadLocalRandom rnd = java.util.concurrent.ThreadLocalRandom.current();
        for (int i = 1; i <= segments; i++) {
            Location next = a.clone().add(delta.clone().multiply((double) i / segments));
            if (i < segments) {
                next.add(rnd.nextDouble(-0.35, 0.35), rnd.nextDouble(-0.35, 0.35), rnd.nextDouble(-0.35, 0.35));
            }
            Vector seg = next.toVector().subtract(prev.toVector());
            double sl = seg.length();
            for (double d = 0; d <= sl; d += 0.15) {
                Location p = prev.clone().add(seg.clone().multiply(d / Math.max(sl, 1e-4)));
                a.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, dust);
            }
            prev = next;
        }
        b.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, b, 8, 0.2, 0.3, 0.2, 0.05);
    }
}

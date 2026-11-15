package com.celestirift.skill.mechanic.impl;

import com.celestirift.CelestiRift;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.mechanic.TargetedLocationMechanic;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * {@code telegraph{skill=BossSlam;radius=5;duration=30}} - draw a warning circle on the
 * ground that fills up, then cast {@code skill} with that spot as its origin, so
 * {@code nearby}/{@code ring} targets inside the circle. Players see the attack coming
 * and can step out. The spot is fixed when the telegraph starts.
 * <pre>
 * - telegraph: { skill: FrostLordNova, radius: 6, duration: 30, color: "#4da6ff" }
 *   target: target          # under the boss's target (or casterLocation, targetLocation...)
 * </pre>
 */
public final class TelegraphMechanic implements TargetedLocationMechanic {

    private final CelestiRift plugin;
    private final String skill;
    private final double radius;
    private final double inner;
    private final int duration;
    private final Color color;

    public TelegraphMechanic(CelestiRift plugin, ConfigMap cfg) {
        this.plugin = plugin;
        this.skill = cfg.getString("", "skill", "s", "then");
        this.radius = Math.max(0.5, cfg.getDouble(4, "radius", "r"));
        this.inner = Math.max(0, cfg.getDouble(0, "inner"));
        this.duration = Math.max(1, cfg.getInt(30, "duration", "d", "ticks"));
        this.color = parse(cfg.getString("#ff3030", "color", "c"));
    }

    private static Color parse(String hex) {
        try {
            return Color.fromRGB(Integer.parseInt(hex.replace("#", ""), 16));
        } catch (NumberFormatException e) {
            return Color.RED;
        }
    }

    @Override
    public void cast(SkillMetadata meta, Location target) {
        Location center = target.clone();
        center.setY(Math.floor(center.getY()) + 0.15);
        double r = radius * meta.rangeMultiplier();
        double in = inner * meta.rangeMultiplier();
        Particle.DustOptions edge = new Particle.DustOptions(color, 1.3f);
        Particle.DustOptions fill = new Particle.DustOptions(color, 0.9f);

        new BukkitRunnable() {
            int t;

            @Override
            public void run() {
                if (!meta.caster().isValid()) {
                    cancel(); // the boss died: no hit
                    return;
                }
                t++;
                if (t % 3 == 1) {
                    ring(center, r, edge);
                    if (in > 0) {
                        ring(center, in, edge);
                    }
                }
                double progress = (double) t / duration;
                ring(center, in + (r - in) * progress, fill);
                if (t >= duration) {
                    cancel();
                    if (!skill.isBlank()) {
                        plugin.skills().cast(skill, new SkillMetadata(meta.caster(), meta.trigger(),
                                center.clone(), meta.power()).rangeMultiplier(meta.rangeMultiplier()));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static void ring(Location c, double r, Particle.DustOptions dust) {
        if (r <= 0.05) {
            return;
        }
        int points = (int) Math.max(8, Math.min(120, r * 2 * Math.PI * 2.5));
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            c.getWorld().spawnParticle(Particle.DUST, c.getX() + Math.cos(a) * r, c.getY(),
                    c.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0, dust);
        }
    }
}

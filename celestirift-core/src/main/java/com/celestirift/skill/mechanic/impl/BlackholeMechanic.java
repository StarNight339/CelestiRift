package com.celestirift.skill.mechanic.impl;

import com.celestirift.CelestiRift;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.TargetedLocationMechanic;
import com.celestirift.vfx.ParticleSpec;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * {@code blackhole{particle=DUST;color=#8000ff;radius=4;arms=3;duration=40;pull=true;pullStrength=0.5}}
 * — an animated accretion vortex at each targeted location: several spiral arms
 * swirl inward for {@code duration} ticks and, when {@code pull=true}, nearby
 * living entities are dragged toward the centre.
 */
public final class BlackholeMechanic implements TargetedLocationMechanic {

    private final CelestiRift plugin;
    private final ParticleSpec spec;
    private final double radius;
    private final int arms;
    private final int pointsPerArm;
    private final int duration;
    private final double rotationSpeed;
    private final double tightness;
    private final boolean pull;
    private final double pullStrength;

    public BlackholeMechanic(CelestiRift plugin, ConfigMap cfg) {
        this.plugin = plugin;
        this.spec = ParticleSpec.parse(cfg);
        this.radius = cfg.getDouble(4.0, "radius", "r");
        this.arms = Math.max(1, cfg.getInt(3, "arms"));
        this.pointsPerArm = Math.max(6, cfg.getInt(24, "points", "n"));
        this.duration = Math.max(1, cfg.getInt(40, "duration", "ticks"));
        this.rotationSpeed = cfg.getDouble(0.35, "rotationspeed", "spin");
        this.tightness = cfg.getDouble(6.0, "tightness");
        this.pull = cfg.getBoolean(true, "pull");
        this.pullStrength = cfg.getDouble(0.5, "pullstrength", "strength");
    }

    @Override
    public void cast(SkillMetadata meta, Location center) {
        final Entity caster = meta.caster();
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (tick >= duration || center.getWorld() == null) {
                    cancel();
                    return;
                }
                double angleBase = tick * rotationSpeed;
                for (int arm = 0; arm < arms; arm++) {
                    double armOffset = arm * (2 * Math.PI / arms);
                    for (int i = 0; i < pointsPerArm; i++) {
                        double frac = i / (double) pointsPerArm; // 0 outer -> 1 inner
                        double rr = radius * (1 - frac);
                        double ang = angleBase + armOffset + frac * tightness;
                        double y = 0.15 + frac * 0.6; // slight funnel toward centre
                        spec.spawn(center.clone().add(Math.cos(ang) * rr, y, Math.sin(ang) * rr));
                    }
                }
                if (pull) {
                    for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                        if (!(e instanceof LivingEntity) || e.equals(caster) || !com.celestirift.skill.CombatRules.isEnemy(caster, e)) {
                            continue;
                        }
                        Vector toCenter = center.toVector().subtract(e.getLocation().toVector());
                        if (toCenter.lengthSquared() > 0.25) {
                            e.setVelocity(e.getVelocity().add(toCenter.normalize().multiply(pullStrength)));
                        }
                    }
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}

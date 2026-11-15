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
 * {@code tornado{particle=CLOUD;radiusBottom=0.5;radiusTop=3;height=5;rings=12;points=16;duration=60;spin=0.4;pull=true}}
 * — an animated funnel: stacked rings widening toward the top, rotating each
 * tick. When {@code pull=true}, nearby living entities are dragged toward the
 * axis and lifted.
 */
public final class TornadoMechanic implements TargetedLocationMechanic {

    private final CelestiRift plugin;
    private final ParticleSpec spec;
    private final double radiusBottom;
    private final double radiusTop;
    private final double height;
    private final int rings;
    private final int points;
    private final int duration;
    private final double spin;
    private final double swirl;
    private final boolean pull;
    private final double pullStrength;
    private final double lift;

    public TornadoMechanic(CelestiRift plugin, ConfigMap cfg) {
        this.plugin = plugin;
        this.spec = ParticleSpec.parse(cfg);
        this.radiusBottom = cfg.getDouble(0.5, "radiusbottom", "rbottom", "rb");
        this.radiusTop = cfg.getDouble(3.0, "radiustop", "rtop", "rt", "radius", "r");
        this.height = cfg.getDouble(5.0, "height", "h");
        this.rings = Math.max(2, cfg.getInt(12, "rings"));
        this.points = Math.max(4, cfg.getInt(16, "points", "n"));
        this.duration = Math.max(1, cfg.getInt(60, "duration", "ticks"));
        this.spin = cfg.getDouble(0.4, "spin", "rotationspeed");
        this.swirl = cfg.getDouble(4.0, "swirl", "twist");
        this.pull = cfg.getBoolean(true, "pull");
        this.pullStrength = cfg.getDouble(0.3, "pullstrength", "strength");
        this.lift = cfg.getDouble(0.15, "lift");
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
                double base = tick * spin;
                for (int h = 0; h < rings; h++) {
                    double t = h / (double) rings;
                    double y = height * t;
                    double rr = radiusBottom + (radiusTop - radiusBottom) * t;
                    double off = base + t * swirl;
                    for (int i = 0; i < points; i++) {
                        double ang = 2 * Math.PI * i / points + off;
                        spec.spawn(center.clone().add(Math.cos(ang) * rr, y, Math.sin(ang) * rr));
                    }
                }
                if (pull) {
                    for (Entity e : center.getWorld().getNearbyEntities(center, radiusTop, height, radiusTop)) {
                        if (!(e instanceof LivingEntity) || e.equals(caster) || !com.celestirift.skill.CombatRules.isEnemy(caster, e)) {
                            continue;
                        }
                        Vector toAxis = new Vector(center.getX() - e.getLocation().getX(), 0,
                                center.getZ() - e.getLocation().getZ());
                        if (toAxis.lengthSquared() > 0.04) {
                            toAxis.normalize().multiply(pullStrength);
                        }
                        toAxis.setY(lift);
                        e.setVelocity(e.getVelocity().add(toAxis));
                    }
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}

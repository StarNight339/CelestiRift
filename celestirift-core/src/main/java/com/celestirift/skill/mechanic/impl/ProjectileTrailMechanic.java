package com.celestirift.skill.mechanic.impl;

import com.celestirift.CelestiRift;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.NoTargetMechanic;
import com.celestirift.vfx.ParticleSpec;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Locale;

/**
 * {@code projectiletrail{particle=FLAME;speed=0.9;maxDistance=30;shape=ring;shapeRadius=0.5;hitRadius=1;damage=6;onHit=Explosion}}
 * — launches a purely-visual particle that flies from the caster's eye in their
 * look direction, drawing a trail (and an optional {@code ring}/{@code helix}
 * around it). On hitting a block, an entity, or {@code maxDistance} it can deal
 * {@code damage} and/or cast the {@code onHit} skill at the impact point.
 */
public final class ProjectileTrailMechanic implements NoTargetMechanic {

    private final CelestiRift plugin;
    private final ParticleSpec spec;
    private final double speed;
    private final double maxDistance;
    private final int subSteps;
    private final String shape;
    private final double shapeRadius;
    private final int shapePoints;
    private final double coilSpeed;
    private final double hitRadius;
    private final com.celestirift.api.skill.NumberValue damage;
    private final String onHit;
    private final boolean stopOnBlock;

    public ProjectileTrailMechanic(CelestiRift plugin, ConfigMap cfg) {
        this.plugin = plugin;
        this.spec = ParticleSpec.parse(cfg);
        this.speed = Math.max(0.1, cfg.getDouble(0.9, "speed", "velocity", "v"));
        this.maxDistance = cfg.getDouble(30.0, "maxdistance", "range", "distance");
        this.subSteps = Math.max(1, cfg.getInt(4, "substeps"));
        this.shape = cfg.getString("none", "shape").toLowerCase(Locale.ROOT);
        this.shapeRadius = cfg.getDouble(0.5, "shaperadius", "sr");
        this.shapePoints = Math.max(3, cfg.getInt(8, "shapepoints", "sp"));
        this.coilSpeed = cfg.getDouble(1.2, "coilspeed");
        this.hitRadius = cfg.getDouble(1.0, "hitradius", "hr");
        this.damage = cfg.getNumber(0.0, "damage", "dmg");
        this.onHit = cfg.getString("", "onhit", "hitskill");
        this.stopOnBlock = cfg.getBoolean(true, "stoponblock");
    }

    @Override
    public void cast(SkillMetadata meta) {
        Location start = (meta.caster() instanceof LivingEntity le)
                ? le.getEyeLocation() : meta.caster().getLocation();
        final Vector dir = start.getDirection().normalize();
        // Basis perpendicular to travel, for ring/helix shapes.
        Vector rightTmp = dir.clone().crossProduct(new Vector(0, 1, 0));
        if (rightTmp.lengthSquared() < 1.0e-6) {
            rightTmp = new Vector(1, 0, 0);
        }
        final Vector right = rightTmp.normalize();
        final Vector up = right.clone().crossProduct(dir).normalize();
        final Entity caster = meta.caster();
        final float power = meta.power();
        final double stepLen = speed / subSteps;

        new BukkitRunnable() {
            final Location point = start.clone();
            double traveled = 0;

            @Override
            public void run() {
                if (point.getWorld() == null) {
                    cancel();
                    return;
                }
                for (int s = 0; s < subSteps; s++) {
                    point.add(dir.getX() * stepLen, dir.getY() * stepLen, dir.getZ() * stepLen);
                    traveled += stepLen;

                    if (traveled >= maxDistance) {
                        impact(null);
                        cancel();
                        return;
                    }
                    if (stopOnBlock && !point.getBlock().isPassable()) {
                        impact(null);
                        cancel();
                        return;
                    }
                    LivingEntity hit = findHit();
                    if (hit != null) {
                        impact(hit);
                        cancel();
                        return;
                    }
                    draw();
                }
            }

            private LivingEntity findHit() {
                if (hitRadius <= 0) {
                    return null;
                }
                for (Entity e : point.getWorld().getNearbyEntities(point, hitRadius, hitRadius, hitRadius)) {
                    if (e instanceof LivingEntity le && !e.equals(caster) && com.celestirift.skill.CombatRules.isEnemy(caster, e)) {
                        return le;
                    }
                }
                return null;
            }

            private void draw() {
                spec.spawn(point);
                if (shape.equals("ring")) {
                    for (int i = 0; i < shapePoints; i++) {
                        double a = 2 * Math.PI * i / shapePoints;
                        spec.spawn(offset(a));
                    }
                } else if (shape.equals("helix")) {
                    double a = traveled * coilSpeed;
                    spec.spawn(offset(a));
                    spec.spawn(offset(a + Math.PI));
                }
            }

            private Location offset(double angle) {
                Vector o = right.clone().multiply(Math.cos(angle) * shapeRadius)
                        .add(up.clone().multiply(Math.sin(angle) * shapeRadius));
                return point.clone().add(o);
            }

            private void impact(LivingEntity hit) {
                double base = hit != null ? damage.get(meta, hit) : 0;
                if (hit != null && base > 0) {
                    double dmg = base * power * com.celestirift.skill.CombatRules.damageMultiplier(caster, hit);
                    if (caster instanceof LivingEntity casterLe) {
                        hit.damage(dmg, casterLe);
                    } else {
                        hit.damage(dmg);
                    }
                }
                if (!onHit.isBlank()) {
                    plugin.skills().cast(onHit, new SkillMetadata(caster, hit, point.clone(), power));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}

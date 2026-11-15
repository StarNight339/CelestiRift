package com.celestirift.skill.mechanic.impl;

import com.celestirift.CelestiRift;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.NoTargetMechanic;
import com.celestirift.vfx.ParticleSpec;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * {@code slash{particle=SWEEP_ATTACK;radius=3;arc=160;forward=2;points=28;animate=true}}
 * — a crescent "air slash" swung in front of the caster, facing their look
 * direction. When {@code animate=true} it sweeps across a few ticks like a swing.
 */
public final class SlashMechanic implements NoTargetMechanic {

    private final CelestiRift plugin;
    private final ParticleSpec spec;
    private final double radius;
    private final double arcRad;
    private final double forward;
    private final int points;
    private final boolean animate;
    private final int duration;
    private final double curve;

    public SlashMechanic(CelestiRift plugin, ConfigMap cfg) {
        this.plugin = plugin;
        this.spec = ParticleSpec.parse(cfg);
        this.radius = cfg.getDouble(3.0, "radius", "r");
        this.arcRad = Math.toRadians(cfg.getDouble(160.0, "arc", "degrees"));
        this.forward = cfg.getDouble(2.0, "forward", "f");
        this.points = Math.max(6, cfg.getInt(28, "points", "n"));
        this.animate = cfg.getBoolean(true, "animate");
        this.duration = Math.max(1, cfg.getInt(4, "duration", "ticks"));
        this.curve = cfg.getDouble(0.6, "curve", "depth");
    }

    @Override
    public void cast(SkillMetadata meta) {
        Location eye = (meta.caster() instanceof LivingEntity le)
                ? le.getEyeLocation() : meta.caster().getLocation();
        Vector dir = eye.getDirection().normalize();
        Vector up = new Vector(0, 1, 0);
        Vector right = dir.clone().crossProduct(up);
        if (right.lengthSquared() < 1.0e-6) {
            right = new Vector(1, 0, 0);
        }
        right.normalize();
        final Vector fRight = right;
        final Vector fDir = dir;
        final Vector planeUp = right.clone().crossProduct(dir).normalize();
        final Location center = eye.clone().add(dir.clone().multiply(forward));

        if (!animate) {
            drawWindow(center, fDir, fRight, planeUp, 0, points);
            return;
        }
        final int perTick = Math.max(1, points / duration);
        new BukkitRunnable() {
            int index = 0;
            @Override
            public void run() {
                if (index >= points) {
                    cancel();
                    return;
                }
                drawWindow(center, fDir, fRight, planeUp, index, Math.min(points, index + perTick));
                index += perTick;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void drawWindow(Location center, Vector dir, Vector right, Vector planeUp, int from, int to) {
        for (int i = from; i < to; i++) {
            double a = -arcRad / 2 + arcRad * (i / (double) (points - 1));
            // Point on the crescent (facing plane), bowed toward the caster at the edges.
            Vector local = planeUp.clone().multiply(Math.cos(a)).add(right.clone().multiply(Math.sin(a)));
            Vector bow = dir.clone().multiply(-Math.abs(Math.sin(a)) * curve);
            spec.spawn(center.clone().add(local.multiply(radius)).add(bow));
        }
    }
}

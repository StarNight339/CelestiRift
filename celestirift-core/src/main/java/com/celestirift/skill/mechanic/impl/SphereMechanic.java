package com.celestirift.skill.mechanic.impl;

import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.TargetedLocationMechanic;
import com.celestirift.vfx.ParticleSpec;
import org.bukkit.Location;

/**
 * {@code sphere{particle=END_ROD;radius=2;points=120}} — an evenly-distributed
 * spherical shell of particles (Fibonacci sphere) around each targeted location.
 */
public final class SphereMechanic implements TargetedLocationMechanic {

    private static final double GOLDEN_ANGLE = Math.PI * (3 - Math.sqrt(5));

    private final ParticleSpec spec;
    private final double radius;
    private final int points;

    public SphereMechanic(ConfigMap cfg) {
        this.spec = ParticleSpec.parse(cfg);
        this.radius = cfg.getDouble(2.0, "radius", "r");
        this.points = Math.max(8, cfg.getInt(120, "points", "n"));
    }

    @Override
    public void cast(SkillMetadata meta, Location center) {
        for (int i = 0; i < points; i++) {
            double y = 1 - (i / (double) (points - 1)) * 2; // 1 .. -1
            double ringRadius = Math.sqrt(Math.max(0, 1 - y * y));
            double theta = GOLDEN_ANGLE * i;
            double x = Math.cos(theta) * ringRadius;
            double z = Math.sin(theta) * ringRadius;
            spec.spawn(center.clone().add(x * radius, y * radius, z * radius));
        }
    }
}

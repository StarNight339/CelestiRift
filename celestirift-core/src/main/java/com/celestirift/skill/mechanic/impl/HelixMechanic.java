package com.celestirift.skill.mechanic.impl;

import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.TargetedLocationMechanic;
import com.celestirift.vfx.ParticleSpec;
import org.bukkit.Location;

/**
 * {@code helix{particle=SOUL_FIRE_FLAME;radius=1;height=3;coils=3;points=60;strands=2}}
 * — one or more spiral strands rising from each targeted location (a DNA-style
 * double helix at {@code strands=2}).
 */
public final class HelixMechanic implements TargetedLocationMechanic {

    private final ParticleSpec spec;
    private final double radius;
    private final double height;
    private final double coils;
    private final int points;
    private final int strands;

    public HelixMechanic(ConfigMap cfg) {
        this.spec = ParticleSpec.parse(cfg);
        this.radius = cfg.getDouble(1.0, "radius", "r");
        this.height = cfg.getDouble(3.0, "height", "h");
        this.coils = cfg.getDouble(3.0, "coils", "turns");
        this.points = Math.max(4, cfg.getInt(60, "points", "n"));
        this.strands = Math.max(1, cfg.getInt(1, "strands"));
    }

    @Override
    public void cast(SkillMetadata meta, Location base) {
        for (int i = 0; i < points; i++) {
            double t = i / (double) points;
            double y = height * t;
            double angle = 2 * Math.PI * coils * t;
            for (int s = 0; s < strands; s++) {
                double a = angle + (2 * Math.PI * s / strands);
                spec.spawn(base.clone().add(Math.cos(a) * radius, y, Math.sin(a) * radius));
            }
        }
    }
}

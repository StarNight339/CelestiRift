package com.celestirift.skill.mechanic.impl;

import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.TargetedLocationMechanic;
import com.celestirift.vfx.ParticleSpec;
import org.bukkit.Location;

import java.util.Locale;

/**
 * {@code circle{particle=FLAME;radius=2;points=40;plane=xz}} — draws a ring of
 * particles around each targeted location. {@code plane}: xz (flat, default),
 * xy or yz (vertical).
 */
public final class CircleMechanic implements TargetedLocationMechanic {

    private final ParticleSpec spec;
    private final double radius;
    private final int points;
    private final String plane;

    public CircleMechanic(ConfigMap cfg) {
        this.spec = ParticleSpec.parse(cfg);
        this.radius = cfg.getDouble(2.0, "radius", "r");
        this.points = Math.max(3, cfg.getInt(40, "points", "n"));
        this.plane = cfg.getString("xz", "plane").toLowerCase(Locale.ROOT);
    }

    @Override
    public void cast(SkillMetadata meta, Location center) {
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            double c = Math.cos(a) * radius;
            double s = Math.sin(a) * radius;
            Location p = switch (plane) {
                case "xy" -> center.clone().add(c, s, 0);
                case "yz" -> center.clone().add(0, c, s);
                default -> center.clone().add(c, 0, s);
            };
            spec.spawn(p);
        }
    }
}

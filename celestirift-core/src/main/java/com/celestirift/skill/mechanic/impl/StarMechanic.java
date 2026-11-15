package com.celestirift.skill.mechanic.impl;

import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.TargetedLocationMechanic;
import com.celestirift.vfx.ParticleSpec;
import org.bukkit.Location;

import java.util.Locale;

/**
 * {@code star{particle=DUST;color=#ffd700;spikes=5;radius=3;innerRatio=0.45;perEdge=8;plane=xz;rotation=0}}
 * — draws an n-pointed star outline (alternating outer/inner vertices) around
 * each targeted location. {@code plane}: xz (flat) / xy / yz.
 */
public final class StarMechanic implements TargetedLocationMechanic {

    private final ParticleSpec spec;
    private final int spikes;
    private final double radius;
    private final double innerRatio;
    private final int perEdge;
    private final String plane;
    private final double rotation;

    public StarMechanic(ConfigMap cfg) {
        this.spec = ParticleSpec.parse(cfg);
        this.spikes = Math.max(2, cfg.getInt(5, "spikes", "points", "n"));
        this.radius = cfg.getDouble(3.0, "radius", "r");
        this.innerRatio = Math.max(0.05, Math.min(0.95, cfg.getDouble(0.45, "innerratio", "inner")));
        this.perEdge = Math.max(2, cfg.getInt(8, "peredge", "step"));
        this.plane = cfg.getString("xz", "plane").toLowerCase(Locale.ROOT);
        this.rotation = Math.toRadians(cfg.getDouble(0.0, "rotation", "rot"));
    }

    @Override
    public void cast(SkillMetadata meta, Location center) {
        int verts = spikes * 2;
        // Precompute alternating outer/inner vertices.
        double[][] v = new double[verts][2];
        for (int k = 0; k < verts; k++) {
            double ang = rotation + Math.PI * k / spikes; // 2*spikes steps over 2*PI
            double rr = (k % 2 == 0) ? radius : radius * innerRatio;
            v[k][0] = Math.cos(ang) * rr;
            v[k][1] = Math.sin(ang) * rr;
        }
        for (int k = 0; k < verts; k++) {
            double[] a = v[k];
            double[] b = v[(k + 1) % verts];
            for (int j = 0; j < perEdge; j++) {
                double t = j / (double) perEdge;
                double x = a[0] + (b[0] - a[0]) * t;
                double y = a[1] + (b[1] - a[1]) * t;
                spec.spawn(map(center, x, y));
            }
        }
    }

    private Location map(Location center, double u, double w) {
        return switch (plane) {
            case "xy" -> center.clone().add(u, w, 0);
            case "yz" -> center.clone().add(0, w, u);
            default -> center.clone().add(u, 0, w);
        };
    }
}

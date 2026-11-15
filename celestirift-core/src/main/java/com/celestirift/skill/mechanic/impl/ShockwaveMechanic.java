package com.celestirift.skill.mechanic.impl;

import com.celestirift.CelestiRift;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.TargetedLocationMechanic;
import com.celestirift.vfx.ParticleSpec;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Locale;

/**
 * {@code shockwave{particle=CRIT;startRadius=0.5;maxRadius=8;speed=0.5;density=1.5;plane=xz}}
 * — an animated ring that expands outward from each targeted location, keeping an
 * even particle density as it grows. Pair with a {@code push}/{@code damage} at
 * a matching radius for a knockback nova.
 */
public final class ShockwaveMechanic implements TargetedLocationMechanic {

    private final CelestiRift plugin;
    private final ParticleSpec spec;
    private final double startRadius;
    private final double maxRadius;
    private final double speed;
    private final double density;
    private final int minPoints;
    private final String plane;

    public ShockwaveMechanic(CelestiRift plugin, ConfigMap cfg) {
        this.plugin = plugin;
        this.spec = ParticleSpec.parse(cfg);
        this.startRadius = cfg.getDouble(0.5, "startradius", "start");
        this.maxRadius = cfg.getDouble(8.0, "maxradius", "max", "radius", "r");
        this.speed = Math.max(0.05, cfg.getDouble(0.5, "speed", "expand"));
        this.density = cfg.getDouble(1.5, "density");
        this.minPoints = Math.max(6, cfg.getInt(12, "minpoints"));
        this.plane = cfg.getString("xz", "plane").toLowerCase(Locale.ROOT);
    }

    @Override
    public void cast(SkillMetadata meta, Location center) {
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                double radius = startRadius + speed * tick;
                if (radius > maxRadius || center.getWorld() == null) {
                    cancel();
                    return;
                }
                int points = Math.max(minPoints, (int) (2 * Math.PI * radius * density));
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
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}

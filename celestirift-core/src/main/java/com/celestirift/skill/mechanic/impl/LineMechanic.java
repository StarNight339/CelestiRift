package com.celestirift.skill.mechanic.impl;

import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.TargetedLocationMechanic;
import com.celestirift.vfx.ParticleSpec;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

/**
 * {@code line{particle=CRIT;step=0.3}} — draws a beam of particles from the
 * caster (eye location) to each targeted location. Great with {@code @target} or
 * {@code @targetLocation} for laser/beam effects.
 */
public final class LineMechanic implements TargetedLocationMechanic {

    private final ParticleSpec spec;
    private final double step;

    public LineMechanic(ConfigMap cfg) {
        this.spec = ParticleSpec.parse(cfg);
        this.step = Math.max(0.1, cfg.getDouble(0.4, "step", "gap"));
    }

    @Override
    public void cast(SkillMetadata meta, Location end) {
        Location start = (meta.caster() instanceof LivingEntity le)
                ? le.getEyeLocation() : meta.caster().getLocation();
        if (start.getWorld() != end.getWorld()) {
            return;
        }
        Vector delta = end.toVector().subtract(start.toVector());
        double length = delta.length();
        if (length < 1.0e-4) {
            return;
        }
        Vector dir = delta.multiply(1.0 / length);
        for (double d = 0; d <= length; d += step) {
            spec.spawn(start.clone().add(dir.clone().multiply(d)));
        }
    }
}

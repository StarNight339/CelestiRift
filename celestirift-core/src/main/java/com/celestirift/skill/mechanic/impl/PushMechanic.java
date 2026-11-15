package com.celestirift.skill.mechanic.impl;

import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.TargetedEntityMechanic;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

/**
 * {@code push{velocity=1.2;mode=away;y=0.4}} — pushes targets relative to the
 * caster. {@code mode}: {@code away} (default) or {@code toward}. {@code y} adds
 * upward lift.
 */
public final class PushMechanic implements TargetedEntityMechanic {

    private final double velocity;
    private final boolean toward;
    private final double y;

    public PushMechanic(ConfigMap cfg) {
        this.velocity = cfg.getDouble(1.0, "velocity", "v", "power");
        this.toward = cfg.getString("away", "mode", "m").equalsIgnoreCase("toward");
        this.y = cfg.getDouble(0.3, "y", "lift");
    }

    @Override
    public void cast(SkillMetadata meta, Entity target) {
        Vector dir = target.getLocation().toVector()
                .subtract(meta.caster().getLocation().toVector());
        if (dir.lengthSquared() < 1.0e-6) {
            dir = meta.caster().getLocation().getDirection();
        }
        dir = dir.normalize().multiply(velocity);
        if (toward) {
            dir.multiply(-1);
        }
        dir.setY(y);
        target.setVelocity(target.getVelocity().add(dir));
    }
}

package com.celestirift.skill.mechanic.impl;

import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.TargetedEntityMechanic;
import org.bukkit.util.Vector;

/** {@code leap{forward=1.4;up=0.8}} — launches the target in its facing direction. */
public final class LeapMechanic implements TargetedEntityMechanic {

    private final double forward;
    private final double up;

    public LeapMechanic(ConfigMap cfg) {
        this.forward = cfg.getDouble(1.2, "forward", "f", "velocity", "v");
        this.up = cfg.getDouble(0.7, "up", "u", "y");
    }

    @Override
    public void cast(SkillMetadata meta, org.bukkit.entity.Entity target) {
        Vector dir = target.getLocation().getDirection().normalize().multiply(forward);
        dir.setY(up);
        target.setVelocity(dir);
    }
}

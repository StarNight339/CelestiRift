package com.celestirift.skill.condition.impl;

import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.condition.Condition;
import com.celestirift.api.skill.ConfigMap;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * {@code ?distance{max=10;min=0}} — true if the target is within [min,max]
 * blocks of the caster. Requires a target entity.
 */
public final class DistanceCondition implements Condition {

    private final double min;
    private final double max;

    public DistanceCondition(ConfigMap cfg) {
        this.max = cfg.getDouble(10.0, "max", "distance", "d");
        this.min = cfg.getDouble(0.0, "min");
    }

    @Override
    public boolean test(SkillMetadata meta, @Nullable Entity target) {
        if (target == null || target.getWorld() != meta.caster().getWorld()) {
            return false;
        }
        double dist = target.getLocation().distance(meta.caster().getLocation());
        return dist >= min && dist <= max;
    }
}

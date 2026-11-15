package com.celestirift.skill.targeter;

import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.SkillTargets;
import com.celestirift.api.skill.targeter.Targeter;

import org.bukkit.entity.Entity;

/**
 * {@code @NearbyEntities} / {@code @EIR} — living entities within a radius.
 * Attributes: {@code radius} (default 5) plus {@link TargetFilter} options
 * ({@code filter} default enemies, {@code limit}, {@code sort}, {@code ignoreCaster}).
 */
public final class NearbyEntitiesTargeter implements Targeter {

    private final double radius;
    private final TargetFilter filter;

    public NearbyEntitiesTargeter(ConfigMap cfg) {
        this.radius = cfg.getDouble(5.0, "radius", "r");
        this.filter = TargetFilter.read(cfg, TargetFilter.Mode.ENEMIES);
    }

    @Override
    public SkillTargets resolve(SkillMetadata meta) {
        SkillTargets targets = new SkillTargets();
        double radius = this.radius * meta.rangeMultiplier();
        // Sphere, not the bounding box getNearbyEntities returns.
        var inRange = meta.origin().getWorld().getNearbyEntities(meta.origin(), radius, radius, radius,
                e -> e.getLocation().distanceSquared(meta.origin()) <= radius * radius);
        filter.apply(meta.caster(), meta.origin(), inRange).forEach(targets::addEntity);
        return targets;
    }
}

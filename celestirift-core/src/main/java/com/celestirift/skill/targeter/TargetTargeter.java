package com.celestirift.skill.targeter;

import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.SkillTargets;
import com.celestirift.api.skill.targeter.Targeter;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;

/**
 * {@code @target} — the entity the caster is looking at (ray-traced).
 * Attributes: {@code range} (default 30), {@code raysize} (default 0.5).
 */
public final class TargetTargeter implements Targeter {

    private final double range;
    private final double raySize;
    private final TargetFilter filter;

    public TargetTargeter(ConfigMap cfg) {
        this.range = cfg.getDouble(30.0, "range", "r");
        this.raySize = cfg.getDouble(0.5, "raysize", "rs");
        // Aimed on purpose, so anything not protected (use filter: allies for heals).
        this.filter = TargetFilter.read(cfg, TargetFilter.Mode.ALL);
    }

    @Override
    public SkillTargets resolve(SkillMetadata meta) {
        SkillTargets targets = new SkillTargets();
        if (!(meta.caster() instanceof LivingEntity caster)) {
            return targets;
        }
        // Mobs and bosses aim at whatever they are already fighting.
        if (caster instanceof org.bukkit.entity.Mob mob && mob.getTarget() != null
                && mob.getTarget().getWorld().equals(mob.getWorld())
                && mob.getTarget().getLocation().distanceSquared(mob.getLocation())
                <= Math.pow(range * meta.rangeMultiplier(), 2)) {
            return filter.test(caster, mob.getTarget()) ? targets.addEntity(mob.getTarget()) : targets;
        }
        RayTraceResult result = caster.getWorld().rayTraceEntities(
                caster.getEyeLocation(),
                caster.getEyeLocation().getDirection(),
                range * meta.rangeMultiplier(),
                raySize,
                e -> e != caster && filter.test(caster, e));
        if (result != null && result.getHitEntity() != null) {
            targets.addEntity(result.getHitEntity());
        }
        return targets;
    }
}

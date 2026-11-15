package com.celestirift.skill.targeter;

import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.SkillTargets;
import com.celestirift.api.skill.targeter.Targeter;

import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

/**
 * {@code @targetLocation} / {@code @TL} — the block location the caster is
 * looking at. Attribute: {@code range} (default 30).
 */
public final class TargetLocationTargeter implements Targeter {

    private final int range;

    public TargetLocationTargeter(ConfigMap cfg) {
        this.range = cfg.getInt(30, "range", "r");
    }

    @Override
    public SkillTargets resolve(SkillMetadata meta) {
        SkillTargets targets = new SkillTargets();
        if (meta.caster() instanceof LivingEntity le) {
            int r = (int) Math.round(range * meta.rangeMultiplier());
            Block block = le.getTargetBlockExact(r);
            if (block != null) {
                targets.addLocation(block.getLocation().add(0.5, 1.0, 0.5));
            } else {
                // No block hit: use a point in front along the look direction.
                targets.addLocation(le.getEyeLocation().add(
                        le.getEyeLocation().getDirection().multiply(r)));
            }
        }
        return targets;
    }
}

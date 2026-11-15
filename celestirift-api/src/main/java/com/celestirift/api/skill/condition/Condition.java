package com.celestirift.api.skill.condition;

import com.celestirift.api.skill.SkillMetadata;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * A gate on a skill line. Evaluated against the caster and (when relevant) the
 * current target entity. Return true to allow the mechanic to run.
 */
public interface Condition {
    boolean test(SkillMetadata meta, @Nullable Entity target);
}

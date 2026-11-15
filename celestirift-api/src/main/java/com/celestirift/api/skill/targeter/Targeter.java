package com.celestirift.api.skill.targeter;

import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.SkillTargets;

/** Selects the entities/locations a mechanic will act on. */
public interface Targeter {
    SkillTargets resolve(SkillMetadata meta);
}

package com.celestirift.api.skill.mechanic;

import com.celestirift.api.skill.SkillMetadata;

/** A mechanic that runs once regardless of targets (delay, cast-another-skill). */
public interface NoTargetMechanic extends Mechanic {
    void cast(SkillMetadata meta);
}

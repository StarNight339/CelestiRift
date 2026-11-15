package com.celestirift.skill.targeter;

import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.SkillTargets;
import com.celestirift.api.skill.targeter.Targeter;


/** {@code @casterLocation} / {@code @CL} — the caster's current location. */
public final class CasterLocationTargeter implements Targeter {
    @Override
    public SkillTargets resolve(SkillMetadata meta) {
        return new SkillTargets().addLocation(meta.caster().getLocation());
    }
}

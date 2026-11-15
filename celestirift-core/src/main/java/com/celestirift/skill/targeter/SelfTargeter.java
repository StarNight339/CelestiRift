package com.celestirift.skill.targeter;

import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.SkillTargets;
import com.celestirift.api.skill.targeter.Targeter;


/** {@code @self} / {@code @caster} — the caster. This is the default targeter. */
public final class SelfTargeter implements Targeter {
    @Override
    public SkillTargets resolve(SkillMetadata meta) {
        return new SkillTargets().addEntity(meta.caster());
    }
}

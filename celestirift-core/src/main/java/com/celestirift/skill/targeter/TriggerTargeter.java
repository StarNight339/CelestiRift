package com.celestirift.skill.targeter;

import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.SkillTargets;
import com.celestirift.api.skill.targeter.Targeter;


/** {@code @trigger} — the entity that triggered the skill (attacker/victim). */
public final class TriggerTargeter implements Targeter {
    @Override
    public SkillTargets resolve(SkillMetadata meta) {
        SkillTargets t = new SkillTargets();
        if (meta.trigger() != null) {
            t.addEntity(meta.trigger());
        }
        return t;
    }
}

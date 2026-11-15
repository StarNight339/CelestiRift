package com.celestirift.skill.targeter;

import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.SkillTargets;
import com.celestirift.api.skill.targeter.Targeter;


/**
 * {@code @origin} — the skill's origin location. For most casts this is the
 * caster's location, but for skills cast at an impact point (e.g. a
 * projectiletrail {@code onHit}) it is the impact location.
 */
public final class OriginTargeter implements Targeter {
    @Override
    public SkillTargets resolve(SkillMetadata meta) {
        return new SkillTargets().addLocation(meta.origin());
    }
}

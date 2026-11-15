package com.celestirift.skill.mechanic.impl;

import com.celestirift.CelestiRift;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.NoTargetMechanic;

/**
 * {@code skill{s=OtherSkill}} — casts another named skill, reusing the current
 * metadata (caster/trigger/power). Lets you compose skills, MythicMobs-style.
 */
public final class MetaSkillMechanic implements NoTargetMechanic {

    private final CelestiRift plugin;
    private final String skillName;

    public MetaSkillMechanic(CelestiRift plugin, ConfigMap cfg) {
        this.plugin = plugin;
        this.skillName = cfg.getString("", "skill", "s", "name");
    }

    @Override
    public void cast(SkillMetadata meta) {
        if (!skillName.isBlank()) {
            plugin.skills().cast(skillName, meta);
        }
    }
}

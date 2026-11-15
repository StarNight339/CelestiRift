package com.celestirift.skill.mechanic.impl;

import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.TargetedEntityMechanic;
import org.bukkit.entity.Entity;

/** {@code ignite{ticks=60}} — sets each targeted entity on fire. */
public final class IgniteMechanic implements TargetedEntityMechanic {

    private final com.celestirift.api.skill.NumberValue ticks;

    public IgniteMechanic(ConfigMap cfg) {
        this.ticks = cfg.getNumber(40, "ticks", "duration", "d");
    }

    @Override
    public void cast(SkillMetadata meta, Entity target) {
        if (!com.celestirift.skill.CombatRules.canHarm(meta.caster(), target)) {
            return;
        }
        target.setFireTicks((int) Math.max(0, Math.round(ticks.get(meta, target))));
    }
}

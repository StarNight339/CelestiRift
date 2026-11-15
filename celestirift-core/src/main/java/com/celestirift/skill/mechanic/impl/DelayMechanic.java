package com.celestirift.skill.mechanic.impl;

import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.NoTargetMechanic;

/**
 * {@code delay{ticks=20}} — pauses the rest of the skill for N ticks (20 = 1s).
 * The actual scheduling is done by {@code SkillExecutor}; this mechanic itself
 * does nothing, it just carries the tick count (read at compile time).
 */
public final class DelayMechanic implements NoTargetMechanic {

    private final int ticks;

    public DelayMechanic(ConfigMap cfg) {
        this.ticks = Math.max(0, cfg.getInt(20, "ticks", "t", "duration", "d"));
    }

    public int ticks() {
        return ticks;
    }

    @Override
    public void cast(SkillMetadata meta) {
        // no-op; handled by the executor's scheduler
    }
}

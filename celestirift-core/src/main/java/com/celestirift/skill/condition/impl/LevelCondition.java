package com.celestirift.skill.condition.impl;

import com.celestirift.CelestiRift;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.condition.Condition;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * {@code level{min=10;max=20}} — the player's CelestiRift level is within range.
 * Checks the target when it's a player, else the caster. Non-players fail.
 * Shorthand: {@code - level: 10} means min level 10.
 */
public final class LevelCondition implements Condition {

    private final CelestiRift plugin;
    private final int min;
    private final int max;

    public LevelCondition(CelestiRift plugin, ConfigMap cfg) {
        this.plugin = plugin;
        this.min = cfg.getInt(1, "min", "level", "l");
        this.max = cfg.getInt(Integer.MAX_VALUE, "max");
    }

    @Override
    public boolean test(SkillMetadata meta, @Nullable Entity target) {
        Entity subject = target instanceof Player ? target : meta.caster();
        if (!(subject instanceof Player p)) {
            return false;
        }
        int level = plugin.profiles().level(p);
        return level >= min && level <= max;
    }
}

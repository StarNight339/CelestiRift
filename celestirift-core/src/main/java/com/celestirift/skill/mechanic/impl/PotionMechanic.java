package com.celestirift.skill.mechanic.impl;

import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.TargetedEntityMechanic;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import com.celestirift.util.Registries;

/**
 * {@code potion{type=SPEED;duration=100;level=1}} — applies a potion effect.
 * {@code duration} is in ticks; {@code level} is 1-based (amplifier = level-1).
 */
public final class PotionMechanic implements TargetedEntityMechanic {

    private final PotionEffectType type;
    private final com.celestirift.api.skill.NumberValue duration;
    private final com.celestirift.api.skill.NumberValue level;

    public PotionMechanic(ConfigMap cfg) {
        this.type = resolveType(cfg.getString("SPEED", "type", "t"));
        this.duration = cfg.getNumber(100, "duration", "d", "ticks");
        this.level = cfg.getNumber(1, "level", "lvl", "l");
    }

    private static PotionEffectType resolveType(String name) {
        return Registries.potion(name).orElseGet(() -> {
            java.util.logging.Logger.getLogger("CelestiRift").warning(
                    "[CelestiRift] Unknown potion effect '" + name + "', defaulting to SPEED");
            return PotionEffectType.SPEED;
        });
    }

    @Override
    public void cast(SkillMetadata meta, Entity target) {
        if (target instanceof LivingEntity le) {
            int ticks = (int) Math.max(1, Math.round(duration.get(meta, target)));
            int amplifier = (int) Math.max(0, Math.round(level.get(meta, target)) - 1);
            le.addPotionEffect(new PotionEffect(type, ticks, amplifier));
        }
    }
}

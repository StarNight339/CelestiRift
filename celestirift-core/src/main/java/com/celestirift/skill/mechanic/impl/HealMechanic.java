package com.celestirift.skill.mechanic.impl;

import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.TargetedEntityMechanic;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/** {@code heal{amount=5}} — heals each targeted living entity, capped at max health. */
public final class HealMechanic implements TargetedEntityMechanic {

    /** Set by the plugin for floating heal numbers. */
    public static com.celestirift.vfx.DamageNumbers damageNumbers;

    private final com.celestirift.api.skill.NumberValue amount;

    public HealMechanic(ConfigMap cfg) {
        this.amount = cfg.getNumber(1.0, "amount", "a", "heal", "h");
    }

    @Override
    public void cast(SkillMetadata meta, Entity target) {
        if (!(target instanceof LivingEntity le)) {
            return;
        }
        double max = le.getAttribute(Attribute.MAX_HEALTH) != null
                ? le.getAttribute(Attribute.MAX_HEALTH).getValue()
                : 20.0;
        double before = le.getHealth();
        le.setHealth(Math.min(max, before + Math.max(0, amount.get(meta, target)) * meta.power()));
        if (meta.caster() instanceof org.bukkit.entity.Player && damageNumbers != null) {
            damageNumbers.heal(le, le.getHealth() - before);
        }
    }
}

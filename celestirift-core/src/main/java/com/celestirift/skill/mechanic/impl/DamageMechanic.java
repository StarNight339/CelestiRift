package com.celestirift.skill.mechanic.impl;

import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.TargetedEntityMechanic;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/**
 * {@code damage{amount=6}} — deals damage to each targeted living entity,
 * scaled by the skill's {@code power}. Attribute {@code ignoreArmor} deals
 * direct health loss instead of combat damage.
 */
public final class DamageMechanic implements TargetedEntityMechanic {

    private final com.celestirift.api.skill.NumberValue amount;
    private final boolean ignoreArmor;

    public DamageMechanic(ConfigMap cfg) {
        this.amount = cfg.getNumber(1.0, "amount", "a", "damage", "d");
        this.ignoreArmor = cfg.getBoolean(false, "ignorearmor", "ia");
    }

    @Override
    public void cast(SkillMetadata meta, Entity target) {
        if (!(target instanceof LivingEntity le) || !com.celestirift.skill.CombatRules.canHarm(meta.caster(), target)) {
            return;
        }
        double dmg = Math.max(0, amount.get(meta, target)) * meta.power()
                * com.celestirift.skill.CombatRules.damageMultiplier(meta.caster(), target);
        if (ignoreArmor) {
            // Magic damage skips armor but still fires damage events (region protection,
            // kill credit for drops/XP).
            var source = org.bukkit.damage.DamageSource.builder(org.bukkit.damage.DamageType.MAGIC);
            if (meta.caster() != null) {
                source.withCausingEntity(meta.caster()).withDirectEntity(meta.caster());
            }
            le.damage(dmg, source.build());
        } else if (meta.caster() instanceof LivingEntity casterLe) {
            le.damage(dmg, casterLe);
        } else {
            le.damage(dmg);
        }
    }
}

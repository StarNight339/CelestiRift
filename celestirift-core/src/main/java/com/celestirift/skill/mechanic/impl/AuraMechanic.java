package com.celestirift.skill.mechanic.impl;

import com.celestirift.CelestiRift;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.mechanic.TargetedEntityMechanic;
import com.celestirift.aura.AuraSpec;
import com.celestirift.skill.CombatRules;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/**
 * {@code aura{id=Mark;duration=100;maxstacks=5;type=debuff;damage-taken=0.05}} - apply a
 * buff/debuff with stacks, timers and stat modifiers. See {@link AuraSpec} and
 * {@link com.celestirift.aura.AuraManager}. A debuff is never put on your own team.
 */
public final class AuraMechanic implements TargetedEntityMechanic {

    private final CelestiRift plugin;
    private final AuraSpec spec;

    public AuraMechanic(CelestiRift plugin, ConfigMap cfg) {
        this.plugin = plugin;
        this.spec = AuraSpec.read(cfg);
    }

    @Override
    public void cast(SkillMetadata meta, Entity target) {
        if (!(target instanceof LivingEntity le) || CombatRules.isProtected(target)) {
            return;
        }
        if (spec.type().equals("debuff") && !CombatRules.canHarm(meta.caster(), target)) {
            return;
        }
        plugin.auras().apply(spec, meta, le);
    }
}

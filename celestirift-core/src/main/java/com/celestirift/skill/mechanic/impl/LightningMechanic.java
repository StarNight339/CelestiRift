package com.celestirift.skill.mechanic.impl;

import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.TargetedLocationMechanic;
import org.bukkit.Location;

/**
 * {@code lightning{damage=true}} — strikes lightning at each targeted location.
 * Set {@code damage=false} for a visual-only strike.
 */
public final class LightningMechanic implements TargetedLocationMechanic {

    private final boolean damage;

    public LightningMechanic(ConfigMap cfg) {
        this.damage = cfg.getBoolean(true, "damage", "d");
    }

    @Override
    public void cast(SkillMetadata meta, Location target) {
        if (target.getWorld() == null) {
            return;
        }
        if (damage) {
            target.getWorld().strikeLightning(target);
        } else {
            target.getWorld().strikeLightningEffect(target);
        }
    }
}

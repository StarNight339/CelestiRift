package com.celestirift.skill.mechanic.impl;

import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.TargetedLocationMechanic;
import org.bukkit.Location;

/**
 * {@code teleport} — teleports the caster to each targeted location (usually
 * paired with {@code @targetLocation}). Keeps the caster's facing direction.
 */
public final class TeleportMechanic implements TargetedLocationMechanic {

    public TeleportMechanic(ConfigMap cfg) {
        // no attributes yet
    }

    @Override
    public void cast(SkillMetadata meta, Location target) {
        Location dest = target.clone();
        dest.setYaw(meta.caster().getLocation().getYaw());
        dest.setPitch(meta.caster().getLocation().getPitch());
        meta.caster().teleport(dest);
    }
}

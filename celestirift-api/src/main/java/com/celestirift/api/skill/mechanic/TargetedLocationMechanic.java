package com.celestirift.api.skill.mechanic;

import com.celestirift.api.skill.SkillMetadata;
import org.bukkit.Location;

/** A mechanic that acts on each targeted location (particle, sound, lightning). */
public interface TargetedLocationMechanic extends Mechanic {
    void cast(SkillMetadata meta, Location target);
}

package com.celestirift.api.skill.mechanic;

import com.celestirift.api.skill.SkillMetadata;
import org.bukkit.entity.Entity;

/** A mechanic that acts on each targeted entity (damage, heal, potion, ...). */
public interface TargetedEntityMechanic extends Mechanic {
    void cast(SkillMetadata meta, Entity target);
}

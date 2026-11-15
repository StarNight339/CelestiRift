package com.celestirift.api.skill.mechanic;

/**
 * Marker for all mechanics. A concrete mechanic implements one or more of:
 * <ul>
 *   <li>{@link TargetedEntityMechanic} – runs once per targeted entity</li>
 *   <li>{@link TargetedLocationMechanic} – runs once per targeted location</li>
 *   <li>{@link NoTargetMechanic} – runs once, ignoring targets</li>
 * </ul>
 * This mirrors MythicMobs' ITargetedEntitySkill / ITargetedLocationSkill split.
 */
public interface Mechanic {

    /** Human-readable name, used only for logging. */
    default String name() {
        return getClass().getSimpleName();
    }
}

package com.celestirift.api.skill;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Everything a mechanic needs to run: who cast it, what triggered it, and any
 * runtime variables. Passed down through the whole skill tree, MythicMobs-style.
 */
public final class SkillMetadata {

    private final Entity caster;
    private final @Nullable Entity trigger;
    private final Location origin;
    private final float power;
    private double rangeMultiplier = 1.0;
    private final Map<String, Object> variables = new HashMap<>();

    public SkillMetadata(Entity caster, @Nullable Entity trigger, Location origin, float power) {
        this.caster = caster;
        this.trigger = trigger;
        this.origin = origin;
        this.power = power;
    }

    /** Convenience: caster is both the caster and the origin, no trigger. */
    public static SkillMetadata of(Entity caster) {
        return new SkillMetadata(caster, null, caster.getLocation(), 1.0f);
    }

    public static SkillMetadata of(Entity caster, @Nullable Entity trigger) {
        return new SkillMetadata(caster, trigger, caster.getLocation(), 1.0f);
    }

    public Entity caster() { return caster; }
    public @Nullable Entity trigger() { return trigger; }
    public Location origin() { return origin.clone(); }
    public float power() { return power; }
    public Map<String, Object> variables() { return variables; }

    /** Multiplier applied to targeter range/radius (from passive augment gems). */
    public double rangeMultiplier() { return rangeMultiplier; }
    public SkillMetadata rangeMultiplier(double m) { this.rangeMultiplier = m; return this; }
}

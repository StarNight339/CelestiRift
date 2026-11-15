package com.celestirift.gem;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import com.celestirift.util.Registries;

/**
 * The effect of a passive gem. Numeric fields are multipliers combined
 * multiplicatively across all passive gems on a weapon; the optional buff is a
 * potion effect applied to the caster whenever an active skill fires.
 *
 * @param power        damage/heal multiplier (mechanics that scale by power)
 * @param range        range/radius multiplier for targeters
 * @param cooldown     cooldown multiplier (e.g. 0.8 = 20% faster)
 * @param buffType     potion effect name applied on cast (empty = none)
 * @param buffDuration ticks
 * @param buffLevel    amplifier level (1-based)
 */
public record Augment(double power, double range, double cooldown,
                      String buffType, int buffDuration, int buffLevel) {

    public static final Augment NONE = new Augment(1, 1, 1, "", 0, 0);

    public boolean hasBuff() {
        return hasBuffType() && buffDuration > 0;
    }

    /** True if a buff type is configured, even with no duration (worn-armor passives supply their own). */
    public boolean hasBuffType() {
        return buffType != null && !buffType.isBlank();
    }

    public boolean isIdentity() {
        return power == 1 && range == 1 && cooldown == 1 && !hasBuff();
    }

    /** Combine multipliers only (buffs are applied per-passive, not merged). */
    public Augment timesMultipliers(Augment o) {
        return new Augment(power * o.power, range * o.range, cooldown * o.cooldown,
                buffType, buffDuration, buffLevel);
    }

    public void applyBuff(LivingEntity target) {
        if (!hasBuff()) {
            return;
        }
        Registries.potion(buffType).ifPresent(type ->
                target.addPotionEffect(new PotionEffect(type, buffDuration, Math.max(0, buffLevel - 1))));
    }

    /**
     * Apply the buff with an overridden duration — used to keep worn-armor buffs
     * alive between refresh ticks. Ambient and particle-free so a permanent
     * equipment buff doesn't swirl forever.
     */
    public void applyBuff(LivingEntity target, int durationTicks) {
        if (!hasBuffType()) {
            return;
        }
        Registries.potion(buffType).ifPresent(type -> target.addPotionEffect(new PotionEffect(
                type, durationTicks, Math.max(0, buffLevel - 1), true, false)));
    }

    // --- Field-level (de)serialization; the socket layer joins/splits --------

    /** The six augment fields as strings, in the order {@link #deserialize} expects. */
    public String[] toFields() {
        return new String[]{
                String.valueOf(power), String.valueOf(range), String.valueOf(cooldown),
                buffType == null ? "" : buffType, String.valueOf(buffDuration), String.valueOf(buffLevel)};
    }

    public static Augment deserialize(String[] parts, int offset) {
        try {
            return new Augment(
                    Double.parseDouble(parts[offset]),
                    Double.parseDouble(parts[offset + 1]),
                    Double.parseDouble(parts[offset + 2]),
                    parts[offset + 3],
                    Integer.parseInt(parts[offset + 4]),
                    Integer.parseInt(parts[offset + 5]));
        } catch (RuntimeException e) {
            return NONE;
        }
    }
}

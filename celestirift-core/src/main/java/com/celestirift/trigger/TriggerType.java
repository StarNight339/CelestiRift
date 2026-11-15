package com.celestirift.trigger;

import java.util.Locale;

/**
 * Player events that can cast a skill when bound in config.yml under
 * {@code triggers:}. {@code trigger} entities (where applicable) are passed to
 * the skill so {@code @trigger} targets the right entity.
 */
public enum TriggerType {
    /** Started sneaking (shift). */
    SNEAK,
    /** Stopped sneaking. */
    UNSNEAK,
    /** Left-clicked air or a block. */
    LEFT_CLICK,
    /** Right-clicked air or a block (independent of skill items). */
    RIGHT_CLICK,
    /** Swapped items between hands (default F key) — a great skill hotkey. */
    SWAP_HANDS,
    /** Dropped an item (default Q key). */
    DROP_ITEM,
    /** Attacked an entity; {@code @trigger} = the victim. */
    ATTACK,
    /** Took damage from an entity; {@code @trigger} = the attacker. */
    DAMAGED,
    /** Joined the server. */
    JOIN,
    /** Killed an entity; {@code @trigger} = the victim. */
    KILL,
    /** Landed a critical (falling) melee hit; {@code @trigger} = the victim. Also fires ATTACK. */
    CRIT,
    /** An arrow/trident/projectile you shot hit an entity; {@code @trigger} = the victim. */
    PROJECTILE_HIT,
    /** Health dropped below combat.low-health (default 30%); {@code @trigger} = the attacker, if any. */
    LOW_HEALTH,
    /** Every second (use the skill's cooldown, or a class passive's every:, to pace it). */
    TIMER;

    /** A deliberate key press: show cooldown/mana feedback when it can't cast. */
    public boolean isKeyPress() {
        return this == SNEAK || this == SWAP_HANDS || this == DROP_ITEM || this == RIGHT_CLICK || this == LEFT_CLICK;
    }

    /** Events it is safe to cancel (so the key acts purely as a skill hotkey). */
    public boolean cancellableByDefault() {
        return this == SWAP_HANDS || this == DROP_ITEM || this == RIGHT_CLICK || this == LEFT_CLICK;
    }

    public static TriggerType fromString(String s) {
        try {
            return TriggerType.valueOf(s.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

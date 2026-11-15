package com.celestirift.api;

/** Outcome of a player skill cast through {@link CelestiRiftApi#cast}. */
public enum CastResult {
    /** The skill ran; cooldown and mana were applied. */
    SUCCESS,
    /** No skill with that name is loaded. */
    UNKNOWN_SKILL,
    /** A skill-level {@code Conditions:} entry failed. Nothing was consumed. */
    CONDITIONS_FAILED,
    /** The skill is still on cooldown for this player. */
    ON_COOLDOWN,
    /** The player lacks the mana to pay the cost. */
    NOT_ENOUGH_MANA,
    /** A {@link com.celestirift.api.event.SkillPreCastEvent} listener cancelled the cast. */
    CANCELLED,
    /** The skill has a cast time: the player started casting; it fires when the cast completes. */
    CASTING;

    /** @return true if the skill actually ran */
    public boolean success() {
        return this == SUCCESS;
    }

    /**
     * @return true if the cast went through or started casting (the key press was "used")
     */
    public boolean accepted() {
        return this == SUCCESS || this == CASTING;
    }
}

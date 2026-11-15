package com.celestirift.trigger;

import org.jetbrains.annotations.Nullable;

/**
 * One "cast this skill when this trigger fires" rule.
 *
 * @param skill       the skill name to cast
 * @param permission  optional permission the player must have (null = none)
 * @param cancel      whether to cancel the underlying event (hotkey behaviour)
 * @param requireItem if true, the player must be holding (main or off hand) a
 *                    skill item bound to this same skill for the trigger to fire
 */
public record TriggerBinding(String skill, @Nullable String permission,
                             boolean cancel, boolean requireItem) {
}

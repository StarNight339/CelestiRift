package com.celestirift.gem;

import com.celestirift.trigger.TriggerType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A named gem template defined in gems.yml. Active gems grant a skill; passive
 * gems ({@code type: PASSIVE}) carry an {@link Augment}. Example:
 * <pre>
 * EmberGem:                 # active
 *   skill: Fireball
 *   tier: 3
 *   trigger: RIGHT_CLICK
 * PowerGem:                 # passive
 *   type: PASSIVE
 *   tier: 4
 *   power: 1.5
 *   range: 1.2
 *   cooldown: 0.85
 *   buff: { type: STRENGTH, duration: 100, level: 1 }
 *   model: celestirift:power_rune   # optional resource-pack item model
 * </pre>
 */
public record GemDef(String id, SocketType type, String skill, int tier, TriggerType trigger,
                     Augment augment, @Nullable String material, @Nullable String name, List<String> lore,
                     @Nullable String model) {

    /** Definition without a custom resource-pack model. */
    public GemDef(String id, SocketType type, String skill, int tier, TriggerType trigger,
                  Augment augment, @Nullable String material, @Nullable String name, List<String> lore) {
        this(id, type, skill, tier, trigger, augment, material, name, lore, null);
    }

    public boolean isPassive() {
        return type == SocketType.PASSIVE;
    }
}

package com.celestirift.gem;

import com.celestirift.trigger.TriggerType;

/**
 * A gem read from an item's persistent data.
 *
 * @param id      the gem definition id (empty for ad-hoc gems)
 * @param type    ACTIVE (grants {@code skill}) or PASSIVE (applies {@code augment})
 * @param skill   the granted skill (ACTIVE only)
 * @param tier    1-5, cosmetic colour
 * @param trigger default activation trigger (ACTIVE only)
 * @param augment the passive effect (PASSIVE only; NONE for ACTIVE)
 */
public record SkillGem(String id, SocketType type, String skill, int tier,
                       TriggerType trigger, Augment augment) {

    public boolean isPassive() {
        return type == SocketType.PASSIVE;
    }
}

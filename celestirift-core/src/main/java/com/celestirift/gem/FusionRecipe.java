package com.celestirift.gem;

import com.celestirift.trigger.TriggerType;

import java.util.List;

/**
 * A gem fusion: combining the {@code ingredients} (gem ids) while socketing (or
 * via {@code /cr fuse}) yields the result.
 *
 * <p>The result is either:
 * <ul>
 *   <li>a defined gem — set {@code resultGem} to a gems.yml id (custom
 *       name/material/lore, and an id so it can chain into further fusions), or</li>
 *   <li>an auto-generated gem from {@code skill}/{@code trigger}/{@code tier}
 *       (no id, default appearance).</li>
 * </ul>
 * If {@code resultGem} is set it wins; otherwise {@code skill} is used.
 */
public record FusionRecipe(String id, List<String> ingredients,
                           String resultGem, String skill, TriggerType trigger, int tier) {

    public boolean hasResultGem() {
        return resultGem != null && !resultGem.isBlank();
    }
}

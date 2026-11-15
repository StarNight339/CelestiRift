package com.celestirift.rpg;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A class from classes.yml. Stats grow linearly with level:
 * {@code value = base + perLevel × (level − 1)}.
 *
 * @param id            classes.yml key
 * @param display       MiniMessage name
 * @param description   MiniMessage description
 * @param icon          menu icon
 * @param manaBase      max mana at level 1
 * @param manaPerLevel  extra max mana per level
 * @param regenBase     mana regen per second at level 1
 * @param regenPerLevel extra regen per level
 * @param socketBonus   extra socket capacity this class can upgrade weapons to
 * @param startingGems  gems.yml ids given once when the class is chosen
 * @param permission    permission needed to choose it (null = anyone)
 * @param passives      skills the class casts automatically on triggers
 */
public record RpgClass(String id, String display, String description, Material icon,
                       double manaBase, double manaPerLevel, double regenBase, double regenPerLevel,
                       int socketBonus, List<String> startingGems, @Nullable String permission,
                       List<Passive> passives) {

    /**
     * {@code - { trigger: kill, skill: Bloodlust, chance: 0.5, every: 5, min-level: 10 }}
     *
     * @param every    for TIMER: seconds between casts (1 = every second)
     * @param minLevel the passive unlocks at this level
     */
    public record Passive(com.celestirift.trigger.TriggerType trigger, String skill, double chance, int every,
                          int minLevel) {}

    public double maxMana(int level) {
        return manaBase + manaPerLevel * (Math.max(1, level) - 1);
    }

    public double regen(int level) {
        return regenBase + regenPerLevel * (Math.max(1, level) - 1);
    }
}

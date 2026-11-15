package com.celestirift.api.event;

import com.celestirift.api.skill.SkillMetadata;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after a player successfully cast a skill (mana and cooldown already applied).
 * Useful for XP gain, statistics, quests and combos.
 */
public final class SkillCastEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String skill;
    private final SkillMetadata metadata;
    private final double manaSpent;

    public SkillCastEvent(Player player, String skill, SkillMetadata metadata, double manaSpent) {
        super(player);
        this.skill = skill;
        this.metadata = metadata;
        this.manaSpent = manaSpent;
    }

    /** @return the skill name */
    public String getSkill() { return skill; }

    /** @return caster/trigger/origin/power for this cast */
    public SkillMetadata getMetadata() { return metadata; }

    /** @return mana consumed by this cast */
    public double getManaSpent() { return manaSpent; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    /** @return the handler list (Bukkit requirement) */
    public static HandlerList getHandlerList() { return HANDLERS; }
}

package com.celestirift.api.event;

import com.celestirift.api.skill.SkillMetadata;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Fired before a player casts a skill, after conditions, cooldown and mana checks
 * passed but before anything is consumed. Cancel to block the cast, or change the
 * mana cost / cooldown (e.g. class bonuses, RPG stats).
 */
public final class SkillPreCastEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String skill;
    private final SkillMetadata metadata;
    private double manaCost;
    private double cooldownSeconds;
    private boolean cancelled;

    public SkillPreCastEvent(Player player, String skill, SkillMetadata metadata,
                             double manaCost, double cooldownSeconds) {
        super(player);
        this.skill = skill;
        this.metadata = metadata;
        this.manaCost = manaCost;
        this.cooldownSeconds = cooldownSeconds;
    }

    /** @return the skill name */
    public String getSkill() { return skill; }

    /** @return caster/trigger/origin/power for this cast */
    public SkillMetadata getMetadata() { return metadata; }

    /** @return mana that will be consumed */
    public double getManaCost() { return manaCost; }

    /** @param manaCost new mana cost (negative values are treated as 0) */
    public void setManaCost(double manaCost) { this.manaCost = Math.max(0, manaCost); }

    /** @return cooldown in seconds applied after the cast */
    public double getCooldownSeconds() { return cooldownSeconds; }

    /** @param cooldownSeconds new cooldown (negative values are treated as 0) */
    public void setCooldownSeconds(double cooldownSeconds) { this.cooldownSeconds = Math.max(0, cooldownSeconds); }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    /** @return the handler list (Bukkit requirement) */
    public static HandlerList getHandlerList() { return HANDLERS; }
}

package com.celestirift.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when an online player's mana changes. Cancel to keep the old value, or
 * change {@link #setNewMana(double) the new value} (clamped to 0..max afterwards).
 */
public final class ManaChangeEvent extends PlayerEvent implements Cancellable {

    /** Why the mana changed. */
    public enum Reason {
        /** Spent on a skill cast. */
        CAST,
        /** Periodic regeneration. */
        REGEN,
        /** Set by a command or another plugin. */
        SET
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final double oldMana;
    private double newMana;
    private final Reason reason;
    private boolean cancelled;

    public ManaChangeEvent(Player player, double oldMana, double newMana, Reason reason) {
        super(player);
        this.oldMana = oldMana;
        this.newMana = newMana;
        this.reason = reason;
    }

    /** @return mana before the change */
    public double getOldMana() { return oldMana; }

    /** @return mana after the change */
    public double getNewMana() { return newMana; }

    /** @param newMana replacement value */
    public void setNewMana(double newMana) { this.newMana = newMana; }

    /** @return why the mana changed */
    public Reason getReason() { return reason; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    /** @return the handler list (Bukkit requirement) */
    public static HandlerList getHandlerList() { return HANDLERS; }
}

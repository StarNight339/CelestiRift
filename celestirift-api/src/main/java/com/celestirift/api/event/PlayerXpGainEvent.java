package com.celestirift.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/** Fired before a player gains CelestiRift XP. Cancel it or change the amount (boosters, parties...). */
public final class PlayerXpGainEvent extends PlayerEvent implements Cancellable {

    /** Where the XP came from. */
    public enum Source {
        /** A successful skill cast. */
        SKILL_CAST,
        /** Killing a mob or boss (drops.yml). */
        MOB_KILL,
        /** An admin command. */
        COMMAND,
        /** Another plugin through the API. */
        API
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private double amount;
    private final Source source;
    private boolean cancelled;

    public PlayerXpGainEvent(Player player, double amount, Source source) {
        super(player);
        this.amount = amount;
        this.source = source;
    }

    /** @return XP that will be added */
    public double getAmount() { return amount; }

    /** @param amount new amount (negative values are treated as 0) */
    public void setAmount(double amount) { this.amount = Math.max(0, amount); }

    /** @return where the XP came from */
    public Source getSource() { return source; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    /** @return the handler list (Bukkit requirement) */
    public static HandlerList getHandlerList() { return HANDLERS; }
}

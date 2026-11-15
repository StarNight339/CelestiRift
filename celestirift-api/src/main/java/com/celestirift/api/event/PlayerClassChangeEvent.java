package com.celestirift.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Fired before a player's class changes (choose, admin set, or reset). Cancel to keep the old class. */
public final class PlayerClassChangeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @Nullable String oldClass;
    private final @Nullable String newClass;
    private boolean cancelled;

    public PlayerClassChangeEvent(Player player, @Nullable String oldClass, @Nullable String newClass) {
        super(player);
        this.oldClass = oldClass;
        this.newClass = newClass;
    }

    /** @return the previous class id, or null if none */
    public @Nullable String getOldClass() { return oldClass; }

    /** @return the new class id, or null when the class is being removed */
    public @Nullable String getNewClass() { return newClass; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    /** @return the handler list (Bukkit requirement) */
    public static HandlerList getHandlerList() { return HANDLERS; }
}

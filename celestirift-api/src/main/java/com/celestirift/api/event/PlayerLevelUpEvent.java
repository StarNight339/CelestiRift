package com.celestirift.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/** Fired after a player's CelestiRift level increased (once per gained level). */
public final class PlayerLevelUpEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int oldLevel;
    private final int newLevel;

    public PlayerLevelUpEvent(Player player, int oldLevel, int newLevel) {
        super(player);
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    /** @return level before this level-up */
    public int getOldLevel() { return oldLevel; }

    /** @return level after this level-up */
    public int getNewLevel() { return newLevel; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    /** @return the handler list (Bukkit requirement) */
    public static HandlerList getHandlerList() { return HANDLERS; }
}

package com.celestirift.listener;

import com.celestirift.CelestiRift;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/** Persists and clears per-player state when a player leaves. */
public final class PlayerSessionListener implements Listener {

    private final CelestiRift plugin;

    public PlayerSessionListener(CelestiRift plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        plugin.refreshItemLooks(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.mana().unload(event.getPlayer());
        plugin.profiles().unload(event.getPlayer());
        // Cooldowns are intentionally kept in memory so relogging can't reset them.
    }
}

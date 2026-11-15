package com.celestirift.rpg;

import com.celestirift.CelestiRift;
import com.celestirift.api.event.PlayerXpGainEvent;
import com.celestirift.api.event.SkillCastEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** Gives XP for successful skill casts ({@code leveling.xp.skill-cast}). */
public final class RpgListener implements Listener {

    private final CelestiRift plugin;

    public RpgListener(CelestiRift plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCast(SkillCastEvent event) {
        double xp = plugin.profiles().xpPerCast();
        if (xp > 0) {
            plugin.profiles().addXp(event.getPlayer(), xp, PlayerXpGainEvent.Source.SKILL_CAST);
        }
    }
}

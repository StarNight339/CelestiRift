package com.celestirift.mana;

import com.celestirift.CelestiRift;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/** Runs every second: regenerates mana and (optionally) shows it on the action bar. */
public final class ManaRegenTask extends BukkitRunnable {

    private final CelestiRift plugin;

    public ManaRegenTask(CelestiRift plugin) {
        this.plugin = plugin;
    }

    /** Schedule at 1s (20 ticks) interval. */
    public void start() {
        runTaskTimer(plugin, 20L, 20L);
    }

    @Override
    public void run() {
        ManaManager mana = plugin.mana();
        if (!mana.enabled()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Only show the bar while regenerating, so it isn't shown at full forever.
            if (mana.regen(player) && mana.showActionBar()) {
                mana.showBar(player);
            }
        }
    }
}

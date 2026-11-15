package com.celestirift.gem;

import com.celestirift.CelestiRift;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Keeps passive-gem buffs active on players wearing socketed armor. Every
 * {@link #PERIOD_TICKS} it re-applies each worn passive's potion buff for
 * slightly longer than the period, so the effect holds seamlessly while the
 * armor is worn and fades shortly after it's removed.
 */
public final class ArmorPassiveTask extends BukkitRunnable {

    private static final long PERIOD_TICKS = 40L;
    private static final int BUFF_DURATION_TICKS = 60; // period + margin so it never flickers

    private final CelestiRift plugin;

    public ArmorPassiveTask(CelestiRift plugin) {
        this.plugin = plugin;
    }

    public void start() {
        runTaskTimer(plugin, PERIOD_TICKS, PERIOD_TICKS);
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (Augment passive : plugin.sockets().wornPassives(player)) {
                passive.applyBuff(player, BUFF_DURATION_TICKS);
            }
        }
    }
}

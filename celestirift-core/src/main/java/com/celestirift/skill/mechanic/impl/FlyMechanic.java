package com.celestirift.skill.mechanic.impl;

import com.celestirift.CelestiRift;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.TargetedEntityMechanic;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * {@code fly{duration=200;speed=0.15;launch=0.5}} — grants each targeted player
 * creative-style flight for {@code duration} ticks, then takes it back.
 * {@code launch} gives an initial upward kick so they take off immediately, and
 * {@code safeLand} (default true) hands out Slow Falling when the flight expires
 * mid-air so it isn't a death sentence.
 *
 * <p>Creative/spectator players are left alone — they already fly, and revoking
 * afterwards would strip flight they're entitled to. Re-casting on a player who
 * is already flying refreshes the timer rather than stacking a second revoke.
 */
public final class FlyMechanic implements TargetedEntityMechanic {

    /** Pending revokes, so a re-cast refreshes the timer instead of double-revoking. */
    private static final Map<UUID, Grant> ACTIVE = new HashMap<>();

    private final CelestiRift plugin;
    private final int duration;
    private final float speed;
    private final double launch;
    private final boolean safeLand;

    public FlyMechanic(CelestiRift plugin, ConfigMap cfg) {
        this.plugin = plugin;
        this.duration = Math.max(1, cfg.getInt(200, "duration", "d", "ticks", "t"));
        // Bukkit fly speed is 0..1; 0.1 is vanilla creative.
        this.speed = (float) Math.max(0.0, Math.min(1.0, cfg.getDouble(0.1, "speed", "s", "flyspeed")));
        this.launch = cfg.getDouble(0.0, "launch", "up", "boost");
        this.safeLand = cfg.getBoolean(true, "safeland", "slowfall");
    }

    @Override
    public void cast(SkillMetadata meta, Entity target) {
        if (!(target instanceof Player player) || alwaysFlies(player)) {
            return;
        }
        UUID id = player.getUniqueId();

        // Refresh: keep the ORIGINAL state to restore, just cancel the old revoke.
        Grant existing = ACTIVE.remove(id);
        if (existing != null) {
            existing.revoke().cancel();
        }
        boolean wasAllowed = existing != null ? existing.wasAllowed() : player.getAllowFlight();
        float previousSpeed = existing != null ? existing.previousSpeed() : player.getFlySpeed();

        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(speed);
        if (launch != 0) {
            player.setVelocity(player.getVelocity().setY(launch));
        }

        BukkitTask revoke = Bukkit.getScheduler().runTaskLater(plugin,
                () -> revoke(id, wasAllowed, previousSpeed), duration);
        ACTIVE.put(id, new Grant(revoke, wasAllowed, previousSpeed));
    }

    private void revoke(UUID id, boolean wasAllowed, float previousSpeed) {
        ACTIVE.remove(id);
        Player player = Bukkit.getPlayer(id);
        if (player == null || alwaysFlies(player)) {
            return;
        }
        boolean airborne = player.isFlying() && nothingUnderfoot(player);
        player.setFlySpeed(previousSpeed);
        player.setFlying(false);
        player.setAllowFlight(wasAllowed);
        if (safeLand && airborne) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 0));
        }
    }

    /** Server-side ground check — {@code Player#isOnGround} is client-reported. */
    private static boolean nothingUnderfoot(Player player) {
        return player.getLocation().subtract(0, 0.2, 0).getBlock().isPassable();
    }

    private static boolean alwaysFlies(Player player) {
        return player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR;
    }

    private record Grant(BukkitTask revoke, boolean wasAllowed, float previousSpeed) {}
}

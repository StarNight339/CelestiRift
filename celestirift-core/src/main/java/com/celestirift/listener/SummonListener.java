package com.celestirift.listener;

import com.celestirift.CelestiRift;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.Nullable;

/**
 * Keeps {@code summon} minions loyal: they can neither hit nor aim at the player
 * who summoned them. Without this, a summoned zombie treats its summoner like
 * any other player and immediately turns on them.
 */
public final class SummonListener implements Listener {

    private final CelestiRift plugin;

    public SummonListener(CelestiRift plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity attacker = source(event.getDamager());
        if (attacker != null && plugin.summons().isOwnedBy(attacker, event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetEvent event) {
        Entity victim = event.getTarget();
        if (victim != null && plugin.summons().isOwnedBy(event.getEntity(), victim.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /** The entity behind the damage — an arrow/fireball resolves to its shooter. */
    private static @Nullable Entity source(Entity damager) {
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            return (shooter instanceof Entity e) ? e : null;
        }
        return damager;
    }
}

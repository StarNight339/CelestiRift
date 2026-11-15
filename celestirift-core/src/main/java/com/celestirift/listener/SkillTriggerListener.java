package com.celestirift.listener;

import com.celestirift.CelestiRift;
import com.celestirift.trigger.TriggerType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Bridges Bukkit events to skill casts. Two sources are checked for every event:
 * <ul>
 *   <li><b>Weapon sockets</b> on the player's held item (the primary system).</li>
 *   <li><b>Global {@code triggers:} bindings</b> from config (empty by default).</li>
 * </ul>
 * Legacy single-skill items (from {@code /cr give}) still cast on right-click.
 */
public final class SkillTriggerListener implements Listener {

    private final CelestiRift plugin;

    public SkillTriggerListener(CelestiRift plugin) {
        this.plugin = plugin;
    }

    /** Fire both the weapon sockets and global bindings; true if event should cancel. */
    private boolean fire(TriggerType type, Player player, Entity trigger) {
        return plugin.dispatcher().fire(type, player, trigger);
    }

    // --- Legacy skill items + left/right click -----------------------------

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // avoid firing twice (main + off hand)
        }
        Action action = event.getAction();
        Player player = event.getPlayer();

        boolean rightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        boolean leftClick = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;

        // Legacy single-skill item takes priority on right-click.
        if (rightClick) {
            String itemSkill = plugin.items().getSkill(event.getItem());
            if (itemSkill != null) {
                event.setCancelled(true);
                if (player.hasPermission("celestirift.use")) {
                    plugin.casts().cast(player, itemSkill, null, true, com.celestirift.gem.Augment.NONE);
                }
                return;
            }
        }

        if (rightClick && fire(TriggerType.RIGHT_CLICK, player, null)) {
            event.setCancelled(true);
        } else if (leftClick && fire(TriggerType.LEFT_CLICK, player, null)) {
            event.setCancelled(true);
        }
    }

    // --- Sneak -------------------------------------------------------------

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        TriggerType type = event.isSneaking() ? TriggerType.SNEAK : TriggerType.UNSNEAK;
        fire(type, event.getPlayer(), null);
    }

    // --- Swap hands (F) ----------------------------------------------------

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (fire(TriggerType.SWAP_HANDS, event.getPlayer(), null)) {
            event.setCancelled(true);
        }
    }

    // --- Drop item (Q): read sockets from the dropped item -----------------

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (plugin.dispatcher().fire(TriggerType.DROP_ITEM, player, null, event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    // --- Attack / Damaged --------------------------------------------------

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        if (damager instanceof Player attacker) {
            fire(TriggerType.ATTACK, attacker, victim);
            if (event.isCritical()) {
                fire(TriggerType.CRIT, attacker, victim);
            }
        }
        if (damager instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof Player shooter
                && !victim.equals(shooter)) {
            fire(TriggerType.PROJECTILE_HIT, shooter, victim);
        }
        if (victim instanceof Player hurt) {
            fire(TriggerType.DAMAGED, hurt, damager);
        }
    }

    // --- Low health: once when health crosses below the threshold -------------

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onHurt(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p)) {
            return;
        }
        if (event.getFinalDamage() > 0) {
            plugin.casts().onDamaged(p); // cast-time skills with cast-interrupt: damage
        }
        var maxAttr = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        double max = maxAttr != null ? maxAttr.getValue() : 20;
        double threshold = plugin.getConfig().getDouble("combat.low-health", 0.3) * max;
        double before = p.getHealth();
        double after = before - event.getFinalDamage();
        if (before > threshold && after <= threshold && after > 0) {
            Entity attacker = null;
            if (event instanceof EntityDamageByEntityEvent by) {
                attacker = by.getDamager() instanceof org.bukkit.entity.Projectile proj
                        && proj.getShooter() instanceof Entity shooter ? shooter : by.getDamager();
            }
            fire(TriggerType.LOW_HEALTH, p, attacker);
        }
    }

    // --- Kill -----------------------------------------------------------------

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
    public void onKill(org.bukkit.event.entity.EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null && !killer.equals(event.getEntity())) {
            fire(TriggerType.KILL, killer, event.getEntity());
        }
    }

    // --- Join --------------------------------------------------------------

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        fire(TriggerType.JOIN, event.getPlayer(), null);
    }
}

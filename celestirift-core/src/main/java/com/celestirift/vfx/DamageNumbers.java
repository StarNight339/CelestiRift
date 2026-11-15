package com.celestirift.vfx;

import com.celestirift.CelestiRift;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Floating numbers ({@code vfx.damage-numbers: true}): damage dealt by players, damage to
 * bosses and training dummies, and heals from skills. Short-lived text displays.
 */
public final class DamageNumbers implements Listener {

    private final CelestiRift plugin;

    public DamageNumbers(CelestiRift plugin) {
        this.plugin = plugin;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("vfx.damage-numbers", true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!enabled() || !(event.getEntity() instanceof LivingEntity victim) || event.getFinalDamage() <= 0.05) {
            return;
        }
        Entity attacker = null;
        boolean crit = false;
        if (event instanceof EntityDamageByEntityEvent by) {
            attacker = by.getDamager() instanceof Projectile p && p.getShooter() instanceof Entity s ? s : by.getDamager();
            crit = by.isCritical();
        }
        boolean show = attacker instanceof Player
                || plugin.dummies().role(victim) != null
                || plugin.bosses().bossId(victim) != null;
        if (!show) {
            return;
        }
        String amount = fmt(event.getFinalDamage());
        Component text = crit
                ? Component.text("✦ " + amount, NamedTextColor.GOLD, TextDecoration.BOLD)
                : Component.text(amount, victim instanceof Player ? NamedTextColor.DARK_RED : NamedTextColor.RED);
        pop(victim, text);
    }

    /** Called by the heal mechanic. */
    public void heal(LivingEntity target, double amount) {
        if (enabled() && amount > 0.05) {
            pop(target, Component.text("+" + fmt(amount), NamedTextColor.GREEN));
        }
    }

    private void pop(LivingEntity at, Component text) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Location loc = at.getLocation().add(r.nextDouble(-0.4, 0.4), at.getHeight() + 0.2, r.nextDouble(-0.4, 0.4));
        TextDisplay d = DisplayFx.spawn(loc, TextDisplay.class, e -> {
            e.text(text);
            e.setBillboard(Display.Billboard.CENTER);
            e.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            e.setShadowed(true);
            e.setViewRange(0.5f); // ~32 blocks
        });
        // Rise and fade by shrinking.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (d.isValid()) {
                DisplayFx.animate(d, DisplayFx.transform(0.6f, 0, 0, 0.9f, false), 14);
            }
        }, 2L);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> DisplayFx.remove(d), 18L);
    }

    private static String fmt(double d) {
        return d >= 10 || d == Math.floor(d) ? String.valueOf((long) Math.round(d)) : String.format(Locale.ROOT, "%.1f", d);
    }
}

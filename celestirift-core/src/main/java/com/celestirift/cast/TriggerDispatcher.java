package com.celestirift.cast;

import com.celestirift.CelestiRift;
import com.celestirift.gem.Augment;
import com.celestirift.gem.SocketData;
import com.celestirift.gem.SocketType;
import com.celestirift.trigger.TriggerBinding;
import com.celestirift.trigger.TriggerType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Turns trigger events into casts from two sources: sockets on the player's
 * weapon and global {@code triggers:} bindings in config.
 *
 * <p>Re-entrancy guard is <b>per player and trigger type</b>: while a player's ATTACK is
 * being dispatched, a nested ATTACK for that player is ignored (an ATTACK skill that deals
 * damage can't re-fire ATTACK forever), but other types still fire - an ATTACK skill that
 * kills still fires KILL - and chains stay finite because each type runs once per chain.
 */
public final class TriggerDispatcher {

    private final CelestiRift plugin;
    private final Set<String> dispatching = new HashSet<>();

    public TriggerDispatcher(CelestiRift plugin) {
        this.plugin = plugin;
    }

    /** Fire weapon sockets (main hand) and global bindings. Returns true if the event should be cancelled. */
    public boolean fire(TriggerType type, Player player, @Nullable Entity trigger) {
        return fire(type, player, trigger, player.getInventory().getItemInMainHand());
    }

    /** As above, reading sockets from a specific item (e.g. the item being dropped). */
    public boolean fire(TriggerType type, Player player, @Nullable Entity trigger, ItemStack weapon) {
        String key = player.getUniqueId() + ":" + type;
        if (!dispatching.add(key)) {
            return false; // a triggered skill must not re-trigger itself
        }
        try {
            boolean cancel = fireSockets(type, player, trigger, weapon);
            cancel |= fireBindings(type, player, trigger);
            firePassives(type, player, trigger);
            return cancel;
        } finally {
            dispatching.remove(key);
        }
    }

    private boolean fireSockets(TriggerType type, Player player, @Nullable Entity trigger, ItemStack weapon) {
        SocketData sockets = plugin.sockets();
        Augment augment = sockets.combinedAugment(weapon);
        List<Augment> passives = sockets.passives(weapon);

        boolean cancel = false;
        for (SocketData.Socket socket : sockets.get(weapon)) {
            if (socket.type() != SocketType.ACTIVE || socket.trigger() != type) {
                continue;
            }
            for (Augment p : passives) {
                p.applyBuff(player);
            }
            var result = plugin.casts().cast(player, socket.skill(), trigger, true, augment);
            if (result.accepted() && type.cancellableByDefault()) {
                cancel = true;
            }
            if (type.isKeyPress()) {
                plugin.casts().explain(player, socket.skill(), result); // why a key press did nothing
            }
        }
        return cancel;
    }

    private long timerSeconds;

    /** TIMER fires once a second for every online player. */
    public void startTimer() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            timerSeconds++;
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (!p.isDead()) {
                    fire(TriggerType.TIMER, p, null);
                }
            }
        }, 20L, 20L);
    }

    /** Class passives (classes.yml passives:). Silent casts; skill cooldown and mana still apply. */
    private void firePassives(TriggerType type, Player player, @Nullable Entity trigger) {
        var clazz = plugin.profiles().rpgClass(player);
        if (clazz.isEmpty()) {
            return;
        }
        int level = plugin.profiles().level(player);
        for (var passive : clazz.get().passives()) {
            if (passive.trigger() != type || level < passive.minLevel()) continue;
            if (type == TriggerType.TIMER && timerSeconds % passive.every() != 0) continue;
            if (passive.chance() < 1 && java.util.concurrent.ThreadLocalRandom.current().nextDouble() >= passive.chance()) continue;
            plugin.casts().cast(player, passive.skill(), trigger, true, Augment.NONE);
        }
    }

    private boolean fireBindings(TriggerType type, Player player, @Nullable Entity trigger) {
        boolean cancel = false;
        for (TriggerBinding binding : plugin.triggers().get(type)) {
            if (binding.permission() != null && !player.hasPermission(binding.permission())) {
                continue;
            }
            if (binding.requireItem() && !isHoldingSkillItem(player, binding.skill())) {
                continue;
            }
            if (plugin.casts().cast(player, binding.skill(), trigger).accepted() && binding.cancel()) {
                cancel = true;
            }
        }
        return cancel;
    }

    /** True if the player holds a skill item bound to {@code skillName} in either hand. */
    private boolean isHoldingSkillItem(Player player, String skillName) {
        String main = plugin.items().getSkill(player.getInventory().getItemInMainHand());
        String off = plugin.items().getSkill(player.getInventory().getItemInOffHand());
        return skillName.equalsIgnoreCase(main) || skillName.equalsIgnoreCase(off);
    }
}

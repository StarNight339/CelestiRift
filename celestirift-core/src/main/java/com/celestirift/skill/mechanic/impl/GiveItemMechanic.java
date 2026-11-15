package com.celestirift.skill.mechanic.impl;

import com.celestirift.CelestiRift;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.mechanic.TargetedEntityMechanic;
import com.celestirift.integration.Orion;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * {@code giveitem{item=demo:ruby;amount=2}} - gives each targeted player an OrionRP custom item
 * (what doesn't fit drops at their feet). Needs OrionRP; without it, or for an unknown item,
 * it does nothing and logs once.
 * <pre>
 * - giveitem{item=demo:ruby;amount=1} @self
 * </pre>
 */
public final class GiveItemMechanic implements TargetedEntityMechanic {

    private final CelestiRift plugin;
    private final String item;
    private final int amount;
    private boolean warned;

    public GiveItemMechanic(CelestiRift plugin, ConfigMap cfg) {
        this.plugin = plugin;
        this.item = cfg.getString("", "item", "i", "id");
        this.amount = Math.max(1, cfg.getInt(1, "amount", "a"));
    }

    @Override
    public void cast(SkillMetadata meta, Entity target) {
        if (!(target instanceof Player player)) {
            return;
        }
        int left = amount;
        while (left > 0) {
            ItemStack stack = Orion.item(item, 1);
            if (stack == null) {
                if (!warned) {
                    warned = true;
                    plugin.getLogger().warning("giveitem: " + (Orion.available()
                            ? "OrionRP has no item '" + item + "' (see /orp items)"
                            : "needs OrionRP, which is not installed"));
                }
                return;
            }
            int n = Math.min(left, stack.getMaxStackSize());
            stack.setAmount(n);
            left -= n;
            player.getInventory().addItem(stack).values()
                    .forEach(rest -> player.getWorld().dropItemNaturally(player.getLocation(), rest));
        }
    }
}

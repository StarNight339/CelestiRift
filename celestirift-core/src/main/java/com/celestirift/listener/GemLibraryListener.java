package com.celestirift.listener;

import com.celestirift.CelestiRift;
import com.celestirift.gui.GemLibraryGui;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/** Handles the {@link GemLibraryGui}: click a gem to receive one, arrows to page. */
public final class GemLibraryListener implements Listener {

    private final CelestiRift plugin;

    public GemLibraryListener(CelestiRift plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GemLibraryGui gui)) {
            return;
        }
        event.setCancelled(true); // give-only menu, no item movement
        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == GemLibraryGui.PREV_SLOT) {
            gui.turn(-1);
            return;
        }
        if (slot == GemLibraryGui.NEXT_SLOT) {
            gui.turn(1);
            return;
        }
        if (slot == GemLibraryGui.CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        if (slot >= GemLibraryGui.PER_PAGE) {
            return; // filler in the nav row
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !(plugin.gems().isGem(clicked) || plugin.gems().isShardVoid(clicked))) {
            return;
        }
        ItemStack give = clicked.clone();
        give.setAmount(event.isShiftClick() ? Math.min(give.getMaxStackSize(), 16) : 1);
        for (ItemStack leftover : player.getInventory().addItem(give).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.3f);
    }
}

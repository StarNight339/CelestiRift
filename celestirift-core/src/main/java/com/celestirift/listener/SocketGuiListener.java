package com.celestirift.listener;

import com.celestirift.CelestiRift;
import com.celestirift.gui.SocketGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Routes clicks to the {@link SocketGui}. Every click while the menu is open is
 * cancelled and handled by the menu itself (no item can be dropped into it), and the
 * weapon is handed back when it closes.
 */
public final class SocketGuiListener implements Listener {

    public SocketGuiListener(CelestiRift plugin) {
        // no state; kept for symmetry with the other listeners
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SocketGui gui)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null) {
            return;
        }
        if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
            gui.clickTop(event.getSlot(), event.getClick());
        } else if (event.getClickedInventory().equals(event.getWhoClicked().getInventory())) {
            gui.clickBottom(event.getSlot());
        }
        if (event.getWhoClicked() instanceof Player p) {
            p.updateInventory();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SocketGui) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof SocketGui gui) {
            gui.close();
        }
    }
}

package com.celestirift.listener;

import com.celestirift.CelestiRift;
import com.celestirift.gui.TriggerGui;
import com.celestirift.trigger.TriggerType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

/** Handles clicks in the {@link TriggerGui}: select a skill, assign a trigger. */
public final class TriggerGuiListener implements Listener {

    private final CelestiRift plugin;

    public TriggerGuiListener(CelestiRift plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof TriggerGui) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TriggerGui gui)) {
            return;
        }
        event.setCancelled(true); // this menu is view/click only, no item movement

        // Ignore clicks in the player's own inventory.
        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        // Select a socket/skill.
        for (int i = 0; i < TriggerGui.SOCKET_SLOTS.length; i++) {
            if (slot == TriggerGui.SOCKET_SLOTS[i]) {
                if (i < gui.socketCount()) {
                    gui.setSelected(i);
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 0.6f, 1.3f);
                }
                return;
            }
        }

        // Assign a trigger to the selected socket/gem.
        for (int i = 0; i < TriggerGui.TRIGGER_SLOTS.length; i++) {
            if (slot == TriggerGui.TRIGGER_SLOTS[i]) {
                assign(player, gui, TriggerGui.TRIGGERS[i]);
                return;
            }
        }
    }

    private void assign(Player player, TriggerGui gui, TriggerType trigger) {
        ItemStack held = player.getInventory().getItemInMainHand();
        boolean changed;
        if (plugin.gems().isGem(held)) {
            changed = plugin.gems().setTrigger(held, trigger);
        } else {
            changed = plugin.sockets().setTriggerAtActive(held, gui.selected(), trigger);
        }
        if (!changed) {
            player.sendMessage(Component.text("Nothing to edit — hold a gem or socketed weapon.",
                    NamedTextColor.RED));
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
            return;
        }
        player.getInventory().setItemInMainHand(held);
        player.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
        gui.render();
    }
}

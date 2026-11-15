package com.celestirift.gui;

import com.celestirift.CelestiRift;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import com.celestirift.item.ItemBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A paginated "give" menu listing every defined gem (and a Shardvoid). Clicking
 * an entry gives the player a copy — no commands needed.
 */
public final class GemLibraryGui implements InventoryHolder {

    public static final int PER_PAGE = 45;
    public static final int PREV_SLOT = 45;
    public static final int CLOSE_SLOT = 49;
    public static final int NEXT_SLOT = 53;

    private final CelestiRift plugin;
    private final List<ItemStack> entries = new ArrayList<>();
    private final Inventory inventory;
    private int page = 0;

    public GemLibraryGui(CelestiRift plugin) {
        this.plugin = plugin;
        // Build the catalogue: all defined gems, then a Shardvoid.
        for (String id : plugin.gemDefs().ids()) {
            plugin.gemDefs().get(id).ifPresent(def -> entries.add(plugin.gems().create(def)));
        }
        entries.add(plugin.gems().createShardVoid());
        this.inventory = Bukkit.createInventory(this, 54,
                com.celestirift.integration.Orion.guiTitle("gem-library", Component.text("Gem Library", NamedTextColor.DARK_PURPLE)));
        render();
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public int totalPages() {
        return Math.max(1, (int) Math.ceil(entries.size() / (double) PER_PAGE));
    }

    public void turn(int delta) {
        int next = page + delta;
        if (next >= 0 && next < totalPages()) {
            page = next;
            render();
        }
    }

    private void render() {
        ItemStack filler = pane(Material.GRAY_STAINED_GLASS_PANE, null);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, i < PER_PAGE ? null : filler);
        }
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = start + i;
            inventory.setItem(i, idx < entries.size() ? entries.get(idx).clone() : null);
        }
        if (page > 0) {
            inventory.setItem(PREV_SLOT, pane(Material.ARROW, "<yellow>Previous"));
        }
        if (page < totalPages() - 1) {
            inventory.setItem(NEXT_SLOT, pane(Material.ARROW, "<yellow>Next"));
        }
        inventory.setItem(CLOSE_SLOT, pane(Material.BARRIER,
                "<red>Close <gray>(page " + (page + 1) + "/" + totalPages() + ")"));
    }

    private static ItemStack pane(Material material, String miniName) {
        ItemBuilder b = ItemBuilder.of(material);
        if (miniName != null) {
            b.name(miniName);
        }
        return b.build();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}

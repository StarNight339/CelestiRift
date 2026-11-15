package com.celestirift.gui;

import com.celestirift.CelestiRift;
import com.celestirift.gem.GemItems;
import com.celestirift.gem.SkillGem;
import com.celestirift.gem.SocketData;
import com.celestirift.trigger.TriggerType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
 * A menu to set the activation trigger of the player's held item. Works on a
 * Skill Gem (its default trigger) or a socketed weapon (per-socket trigger):
 * click a skill in the top row to select it, then click a trigger below to bind.
 * The held item is edited live.
 */
public final class TriggerGui implements InventoryHolder {

    public static final int[] SOCKET_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    public static final TriggerType[] TRIGGERS = TriggerType.values();
    /** Triggers fill the rows below the sockets (slot 18 onwards). */
    public static final int[] TRIGGER_SLOTS = java.util.stream.IntStream.range(18, 18 + TRIGGERS.length).toArray();
    private static final int SIZE = (int) Math.ceil((18 + TRIGGERS.length) / 9.0) * 9;

    private final CelestiRift plugin;
    private final Player player;
    private final Inventory inventory;
    private int selected = 0;

    public TriggerGui(CelestiRift plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, SIZE,
                com.celestirift.integration.Orion.guiTitle("trigger", Component.text("Set Trigger", NamedTextColor.DARK_PURPLE)));
        render();
    }

    public void open() {
        player.openInventory(inventory);
    }

    public int selected() {
        return selected;
    }

    public boolean isGemMode() {
        return plugin.gems().isGem(player.getInventory().getItemInMainHand());
    }

    public int socketCount() {
        return currentSockets().size();
    }

    public void setSelected(int index) {
        if (index >= 0 && index < socketCount()) {
            this.selected = index;
            render();
        }
    }

    private List<SocketData.Socket> currentSockets() {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (plugin.gems().isGem(held)) {
            SkillGem gem = plugin.gems().read(held);
            List<SocketData.Socket> one = new ArrayList<>();
            if (gem != null && !gem.isPassive()) {
                one.add(SocketData.Socket.active(gem.trigger(), gem.skill()));
            }
            return one;
        }
        // Weapon: only ACTIVE sockets have an editable trigger.
        List<SocketData.Socket> active = new ArrayList<>();
        for (SocketData.Socket s : plugin.sockets().get(held)) {
            if (s.isActive()) {
                active.add(s);
            }
        }
        return active;
    }

    /** Re-draw everything from the current held item + selection. */
    public void render() {
        ItemStack filler = pane(Material.GRAY_STAINED_GLASS_PANE, null, null, false);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
        inventory.setItem(4, pane(Material.NAME_TAG,
                "<gold><bold>Set Trigger</bold>",
                List.of("<gray>Pick a skill, then a trigger."), false));

        List<SocketData.Socket> sockets = currentSockets();
        if (sockets.isEmpty()) {
            inventory.setItem(13, pane(Material.BARRIER,
                    "<red>Nothing to edit",
                    List.of("<gray>Hold a Skill Gem or a", "<gray>socketed weapon."), false));
            return;
        }
        if (selected >= sockets.size()) {
            selected = 0;
        }
        boolean gemMode = plugin.gems().isGem(player.getInventory().getItemInMainHand());

        for (int i = 0; i < sockets.size() && i < SOCKET_SLOTS.length; i++) {
            SocketData.Socket s = sockets.get(i);
            boolean sel = i == selected;
            inventory.setItem(SOCKET_SLOTS[i], pane(gemMode ? gemMaterial() : Material.PAPER,
                    (sel ? "<aqua>" : "<white>") + s.skill(),
                    List.of("<gray>Trigger: <white>" + GemItems.prettyTrigger(s.trigger()),
                            sel ? "<green>✔ Selected" : "<yellow>Click to select"),
                    sel));
        }

        SocketData.Socket sel = sockets.get(selected);
        for (int i = 0; i < TRIGGERS.length; i++) {
            TriggerType t = TRIGGERS[i];
            boolean active = t == sel.trigger();
            inventory.setItem(TRIGGER_SLOTS[i], pane(triggerMaterial(t),
                    (active ? "<green>" : "<white>") + GemItems.prettyTrigger(t),
                    List.of(active ? "<green>● Current" : "<yellow>Click to assign"),
                    active));
        }
    }

    private Material gemMaterial() {
        return com.celestirift.util.Registries.itemMaterial(plugin.getConfig().getString("gems.material"))
                .orElse(Material.AMETHYST_SHARD);
    }

    private static Material triggerMaterial(TriggerType t) {
        return switch (t) {
            case SNEAK -> Material.LEATHER_BOOTS;
            case UNSNEAK -> Material.FEATHER;
            case LEFT_CLICK -> Material.WOODEN_SWORD;
            case RIGHT_CLICK -> Material.STICK;
            case SWAP_HANDS -> Material.SHIELD;
            case DROP_ITEM -> Material.DROPPER;
            case ATTACK -> Material.IRON_SWORD;
            case DAMAGED -> Material.SHIELD;
            case JOIN -> Material.OAK_DOOR;
            case KILL -> Material.SKELETON_SKULL;
            case CRIT -> Material.DIAMOND_SWORD;
            case PROJECTILE_HIT -> Material.ARROW;
            case LOW_HEALTH -> Material.RED_DYE;
            case TIMER -> Material.CLOCK;
        };
    }

    private static ItemStack pane(Material material, String miniName, List<String> miniLore, boolean glow) {
        ItemBuilder b = ItemBuilder.of(material);
        if (miniName != null) {
            b.name(miniName);
        } else {
            b.name(Component.text(" "));
        }
        if (miniLore != null && !miniLore.isEmpty()) {
            b.loreMini(miniLore);
        }
        if (glow) {
            b.glint(true);
        }
        return b.build();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}

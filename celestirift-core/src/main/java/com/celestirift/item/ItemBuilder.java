package com.celestirift.item;

import com.celestirift.util.Registries;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for plugin items using Paper's data component API (no ItemMeta
 * copies). Every CelestiRift item — skill items, gems, Shardvoid and GUI icons —
 * goes through here so models/tooltips/glint are handled in one place.
 */
public final class ItemBuilder {

    private final ItemStack item;

    private ItemBuilder(ItemStack item) {
        this.item = item;
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(ItemStack.of(material));
    }

    /** MiniMessage (with OrionRP {@code <glyph:x>}) → Component with the vanilla italic styling removed. */
    public static Component mm(String miniMessage) {
        return MiniMessage.miniMessage().deserialize(miniMessage, com.celestirift.integration.Orion.tags())
                .decoration(TextDecoration.ITALIC, false);
    }

    public ItemBuilder name(Component name) {
        item.setData(DataComponentTypes.CUSTOM_NAME, name.decorationIfAbsent(TextDecoration.ITALIC,
                TextDecoration.State.FALSE));
        return this;
    }

    public ItemBuilder name(String miniMessage) {
        return name(mm(miniMessage));
    }

    public ItemBuilder lore(List<? extends Component> lines) {
        item.setData(DataComponentTypes.LORE, ItemLore.lore(lines));
        return this;
    }

    public ItemBuilder loreMini(List<String> miniMessageLines) {
        List<Component> out = new ArrayList<>(miniMessageLines.size());
        for (String s : miniMessageLines) {
            out.add(mm(s));
        }
        return lore(out);
    }

    /** Custom item model (resource pack), e.g. {@code celestirift:ember_gem}. Ignored if blank/invalid. */
    public ItemBuilder model(@Nullable String key) {
        Registries.key(key).ifPresent(k -> item.setData(DataComponentTypes.ITEM_MODEL, k));
        return this;
    }

    /** Custom tooltip style (resource pack), e.g. {@code celestirift:legendary}. Ignored if blank/invalid. */
    public ItemBuilder tooltipStyle(@Nullable String key) {
        Registries.key(key).ifPresent(k -> item.setData(DataComponentTypes.TOOLTIP_STYLE, k));
        return this;
    }

    public ItemBuilder glint(boolean glint) {
        item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, glint);
        return this;
    }

    public <P, C> ItemBuilder data(NamespacedKey key, PersistentDataType<P, C> type, C value) {
        item.editPersistentDataContainer(pdc -> pdc.set(key, type, value));
        return this;
    }

    public ItemStack build() {
        return item;
    }

    // ------------------------------------------------------------------ looks on existing items

    /** Set (or reset to the material default when null/blank) an existing item's model. */
    public static void applyModel(ItemStack item, @Nullable String key) {
        var k = Registries.key(key);
        if (k.isPresent()) {
            item.setData(DataComponentTypes.ITEM_MODEL, k.get());
        } else {
            item.resetData(DataComponentTypes.ITEM_MODEL);
        }
    }

    /** Set (or remove when null/blank) an existing item's tooltip style. */
    public static void applyTooltipStyle(ItemStack item, @Nullable String key) {
        var k = Registries.key(key);
        if (k.isPresent()) {
            item.setData(DataComponentTypes.TOOLTIP_STYLE, k.get());
        } else {
            item.resetData(DataComponentTypes.TOOLTIP_STYLE);
        }
    }

    // ------------------------------------------------------------------ lore on existing items

    /** Current lore lines of an item (empty list if none). */
    public static List<Component> loreOf(ItemStack item) {
        ItemLore lore = item.getData(DataComponentTypes.LORE);
        return lore == null ? new ArrayList<>() : new ArrayList<>(lore.lines());
    }

    public static void setLore(ItemStack item, List<? extends Component> lines) {
        if (lines.isEmpty()) {
            item.unsetData(DataComponentTypes.LORE);
        } else {
            item.setData(DataComponentTypes.LORE, ItemLore.lore(lines));
        }
    }
}

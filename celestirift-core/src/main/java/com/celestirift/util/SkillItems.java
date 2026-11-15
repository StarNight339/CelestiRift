package com.celestirift.util;

import com.celestirift.CelestiRift;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import com.celestirift.item.ItemBuilder;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Helpers to tag an ItemStack with a skill name (persistent data) so that
 * right-clicking it casts that skill. The item's name and lore are rendered with
 * MiniMessage, including <gradient> colours read from config (items: section),
 * and the lore shows the skill's mana cost and cooldown.
 */
public final class SkillItems {

    private final CelestiRift plugin;
    private final NamespacedKey key;

    public SkillItems(CelestiRift plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "skill");
    }

    /** Read the skill bound to an item, or null. */
    public @Nullable String getSkill(@Nullable ItemStack item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        return item.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    /**
     * Re-apply the current resource-pack model to an existing skill item (a model set
     * in the skill's {@code item:} block is kept) and its cooldown group. Returns true if
     * it's a skill item.
     */
    public boolean refreshLook(@Nullable ItemStack item) {
        String skill = getSkill(item);
        if (skill == null) {
            return false;
        }
        var def = plugin.skills().getSkill(skill);
        String explicit = def.map(s -> s.itemStyle().model()).orElse(null);
        ItemBuilder.applyModel(item, explicit != null && !explicit.isBlank()
                ? explicit : plugin.resourcePack().model("skill-item"));
        applyCooldownGroup(item, skill, def.map(com.celestirift.skill.Skill::cooldownSeconds).orElse(0.0));
        return true;
    }

    /** Give the item the skill's cooldown group so casts show the vanilla cooldown sweep. */
    private static void applyCooldownGroup(ItemStack item, String skillName, double cooldownSeconds) {
        item.setData(io.papermc.paper.datacomponent.DataComponentTypes.USE_COOLDOWN,
                io.papermc.paper.datacomponent.item.UseCooldown.useCooldown((float) Math.max(0.05, cooldownSeconds))
                        .cooldownGroup(cooldownGroup(skillName)).build());
    }

    /** Create a skill-bound item of the given material with gradient name/lore. */
    /** Cooldown group shared by every skill item of a skill (hotbar sweep). */
    public static net.kyori.adventure.key.Key cooldownGroup(String skillName) {
        String id = skillName.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        return net.kyori.adventure.key.Key.key("celestirift", "skill/" + id);
    }

    public ItemStack create(String skillName, Material material) {
        var cfg = plugin.getConfig();
        var skillOpt = plugin.skills().getSkill(skillName);

        // Per-skill Item: block overrides the global items: config where set.
        com.celestirift.skill.Skill.ItemStyle style =
                skillOpt.map(com.celestirift.skill.Skill::itemStyle)
                        .orElse(com.celestirift.skill.Skill.ItemStyle.EMPTY);

        String nameFrom = orElse(style.nameFrom(), cfg.getString("items.name-gradient.from", "#b06ab3"));
        String nameTo = orElse(style.nameTo(), cfg.getString("items.name-gradient.to", "#4568dc"));
        String manaFrom = orElse(style.manaFrom(), cfg.getString("items.mana-gradient.from", "#00c6ff"));
        String manaTo = orElse(style.manaTo(), cfg.getString("items.mana-gradient.to", "#0072ff"));

        double mana = skillOpt.map(s -> s.manaCost()).orElse(0.0);
        double cooldown = skillOpt.map(s -> s.cooldownSeconds()).orElse(0.0);

        ItemBuilder builder = ItemBuilder.of(material)
                .data(key, PersistentDataType.STRING, skillName)
                .name(line("<gradient:" + nameFrom + ":" + nameTo + "><bold>"
                + skillName + "</bold></gradient>"));

        List<Component> lore = new ArrayList<>();
        lore.add(line("<gradient:" + manaFrom + ":" + manaTo + ">✦ Mana Cost: "
                + fmt(mana) + "</gradient>"));
        if (cooldown > 0) {
            lore.add(line("<dark_aqua>⦿ Cooldown: <gray>" + fmt(cooldown) + "s"));
        }
        lore.add(Component.empty());
        lore.add(line("<dark_gray>▶ <gray>Right-click to cast"));
        ItemStack item = builder.lore(lore)
                .model(style.model() != null && !style.model().isBlank() ? style.model()
                        : plugin.resourcePack().model("skill-item"))
                .build();
        applyCooldownGroup(item, skillName, cooldown);
        return item;
    }

    /** Deserialize MiniMessage and strip the default italic lore/name styling. */
    private static Component line(String miniMessage) {
        return ItemBuilder.mm(miniMessage);
    }

    /** First value if non-null and non-blank, otherwise the fallback. */
    private static String orElse(@Nullable String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    private static String fmt(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return String.valueOf((long) d);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", d);
    }
}

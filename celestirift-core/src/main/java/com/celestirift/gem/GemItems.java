package com.celestirift.gem;

import com.celestirift.CelestiRift;
import com.celestirift.trigger.TriggerType;
import net.kyori.adventure.text.Component;
import com.celestirift.item.ItemBuilder;
import com.celestirift.util.Registries;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Creates and reads Skill Gems (active/passive) and Shardvoid upgrade items.
 * Active gems grant a skill; passive gems carry an {@link Augment}; Shardvoid
 * upgrades a weapon's socket capacity.
 */
public final class GemItems {

    private final CelestiRift plugin;
    private final NamespacedKey markerKey;
    private final NamespacedKey idKey;
    private final NamespacedKey typeKey;
    private final NamespacedKey skillKey;
    private final NamespacedKey tierKey;
    private final NamespacedKey triggerKey;
    private final NamespacedKey augKey;      // serialized augment fields (US-joined)
    private final NamespacedKey shardKey;

    public GemItems(CelestiRift plugin) {
        this.plugin = plugin;
        this.markerKey = new NamespacedKey(plugin, "gem");
        this.idKey = new NamespacedKey(plugin, "gem_id");
        this.typeKey = new NamespacedKey(plugin, "gem_type");
        this.skillKey = new NamespacedKey(plugin, "gem_skill");
        this.tierKey = new NamespacedKey(plugin, "gem_tier");
        this.triggerKey = new NamespacedKey(plugin, "gem_trigger");
        this.augKey = new NamespacedKey(plugin, "gem_aug");
        this.shardKey = new NamespacedKey(plugin, "shardvoid");
    }

    // ------------------------------------------------------------------ reads

    public boolean isGem(@Nullable ItemStack item) {
        return item != null && !item.isEmpty()
                && item.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    public boolean isShardVoid(@Nullable ItemStack item) {
        return item != null && !item.isEmpty()
                && item.getPersistentDataContainer().has(shardKey, PersistentDataType.BYTE);
    }

    public String getId(@Nullable ItemStack item) {
        if (!isGem(item)) {
            return "";
        }
        return item.getPersistentDataContainer()
                .getOrDefault(idKey, PersistentDataType.STRING, "");
    }

    /** Read the full gem data from an item, or null if it isn't a gem. */
    public @Nullable SkillGem read(@Nullable ItemStack item) {
        if (!isGem(item)) {
            return null;
        }
        var pdc = item.getPersistentDataContainer();
        SocketType type = SocketType.fromString(
                pdc.getOrDefault(typeKey, PersistentDataType.STRING, "ACTIVE"), SocketType.ACTIVE);
        String id = pdc.getOrDefault(idKey, PersistentDataType.STRING, "");
        String skill = pdc.getOrDefault(skillKey, PersistentDataType.STRING, "");
        int tier = pdc.getOrDefault(tierKey, PersistentDataType.INTEGER, 1);
        TriggerType trigger = TriggerType.fromString(
                pdc.getOrDefault(triggerKey, PersistentDataType.STRING, "RIGHT_CLICK"));
        if (trigger == null) {
            trigger = TriggerType.RIGHT_CLICK;
        }
        Augment augment = Augment.NONE;
        String augStr = pdc.get(augKey, PersistentDataType.STRING);
        if (augStr != null) {
            augment = Augment.deserialize(augStr.split(""), 0);
        }
        return new SkillGem(id, type, skill, tier, trigger, augment);
    }

    // ------------------------------------------------------------------ creates

    public ItemStack create(String skill, int tier, TriggerType trigger) {
        return create(new GemDef("", SocketType.ACTIVE, skill, tier, trigger, Augment.NONE, null, null, List.of()));
    }

    public ItemStack create(String skill, int tier, TriggerType trigger,
                            @Nullable String materialName, @Nullable String nameOverride, List<String> extraLore) {
        return create(new GemDef("", SocketType.ACTIVE, skill, tier, trigger, Augment.NONE,
                materialName, nameOverride, extraLore));
    }

    /** Create a gem item from a definition (active or passive). */
    public ItemStack create(GemDef def) {
        Material material = matchMaterial(def.material(),
                plugin.getConfig().getString("gems.material", "AMETHYST_SHARD"), Material.AMETHYST_SHARD);
        String[] grad = tierGradient(def.tier());

        ItemBuilder builder = ItemBuilder.of(material)
                .data(markerKey, PersistentDataType.BYTE, (byte) 1)
                .data(idKey, PersistentDataType.STRING, def.id())
                .data(typeKey, PersistentDataType.STRING, def.type().name())
                .data(skillKey, PersistentDataType.STRING, def.skill())
                .data(tierKey, PersistentDataType.INTEGER, def.tier())
                .data(triggerKey, PersistentDataType.STRING, def.trigger().name());
        if (def.isPassive()) {
            builder.data(augKey, PersistentDataType.STRING, String.join("\u001F", def.augment().toFields()));
        }

        String defaultName = "<gradient:" + grad[0] + ":" + grad[1] + "><bold>"
                + tierName(def.tier()) + (def.isPassive() ? " Rune" : " Skill Gem") + "</bold></gradient>";
        builder.name(line((def.name() != null && !def.name().isBlank()) ? def.name() : defaultName));

        return builder.lore(buildLore(def))
                .glint(true)
                .model(def.model() != null && !def.model().isBlank() ? def.model()
                        : plugin.resourcePack().gemModel(def.id(), def.tier(), def.isPassive()))
                .tooltipStyle(plugin.resourcePack().tooltipStyle(def.tier()))
                .build();
    }

    /** Change the default trigger stored on an active gem item, updating its lore. */
    public boolean setTrigger(ItemStack item, TriggerType trigger) {
        SkillGem gem = read(item);
        if (gem == null || gem.isPassive()) {
            return false;
        }
        item.editPersistentDataContainer(pdc -> pdc.set(triggerKey, PersistentDataType.STRING, trigger.name()));
        GemDef def = defFor(gem);
        ItemBuilder.setLore(item, buildLore(new GemDef(def.id(), def.type(), def.skill(), def.tier(), trigger,
                def.augment(), def.material(), def.name(), def.lore(), def.model())));
        return true;
    }

    /** Create a Shardvoid — upgrades a weapon's socket capacity by one. */
    public ItemStack createShardVoid() {
        Material material = matchMaterial(plugin.getConfig().getString("gems.shardvoid-material"),
                "ECHO_SHARD", Material.ECHO_SHARD);
        return ItemBuilder.of(material)
                .data(shardKey, PersistentDataType.BYTE, (byte) 1)
                .name("<gradient:#2b0055:#8000ff><bold>Shardvoid</bold></gradient>")
                .loreMini(List.of(
                        "<gray>Adds a socket slot to a weapon.",
                        "<dark_gray>Place with a weapon in /cr socket"))
                .glint(true)
                .model(plugin.resourcePack().model("shardvoid"))
                .build();
    }

    /**
     * Re-apply the current resource-pack look (model + tooltip) to an existing gem or
     * Shardvoid, e.g. after the pack is enabled/disabled. A model set explicitly on the
     * gem's definition is kept. Returns true if the item is one of ours.
     */
    public boolean refreshLook(@Nullable ItemStack item) {
        if (isShardVoid(item)) {
            ItemBuilder.applyModel(item, plugin.resourcePack().model("shardvoid"));
            return true;
        }
        SkillGem gem = read(item);
        if (gem == null) {
            return false;
        }
        String explicit = gem.id().isBlank() ? null
                : plugin.gemDefs().get(gem.id()).map(GemDef::model).orElse(null);
        ItemBuilder.applyModel(item, explicit != null && !explicit.isBlank()
                ? explicit : plugin.resourcePack().gemModel(gem.id(), gem.tier(), gem.isPassive()));
        ItemBuilder.applyTooltipStyle(item, plugin.resourcePack().tooltipStyle(gem.tier()));
        ItemBuilder.setLore(item, buildLore(defFor(gem))); // keeps tooltips current after edits/updates
        return true;
    }

    /** The definition behind a gem item, with the item's own trigger/tier/augment (players can change them). */
    private GemDef defFor(SkillGem gem) {
        GemDef d = gem.id().isBlank() ? null : plugin.gemDefs().get(gem.id()).orElse(null);
        return new GemDef(gem.id(), gem.type(), gem.skill(), gem.tier(), gem.trigger(), gem.augment(),
                d != null ? d.material() : null, d != null ? d.name() : null,
                d != null ? d.lore() : List.of(), d != null ? d.model() : null);
    }

    /**
     * Tooltip of a gem:
     * <pre>
     * ✦ Fireball                       (tier colours)
     * Hurl a blazing orb that ...      (the skill's description, wrapped)
     *
     * ▸ Trigger Right-Click
     * ▸ Mana 20 · Cooldown 3s
     * ▸ Rare ★★★☆☆
     * flavour lines from gems.yml
     *
     * Socket into a weapon · /cr socket
     * </pre>
     */
    private List<Component> buildLore(GemDef def) {
        String[] grad = tierGradient(def.tier());
        String g = "<gradient:" + grad[0] + ":" + grad[1] + ">";
        List<Component> lore = new ArrayList<>();
        if (def.isPassive()) {
            lore.add(line(g + "◆ Passive Rune</gradient>"));
            Augment a = def.augment();
            if (a.power() != 1) lore.add(stat("Power", "<green>" + pct(a.power())));
            if (a.range() != 1) lore.add(stat("Range", "<green>" + pct(a.range())));
            if (a.cooldown() != 1) lore.add(stat("Cooldown", "<green>" + pct(a.cooldown())));
            if (a.hasBuffType()) lore.add(stat("Buff", "<white>" + pretty(a.buffType()) + " " + roman(a.buffLevel())));
        } else {
            lore.add(line(g + "<bold>✦ " + def.skill() + "</bold></gradient>"));
            var skill = plugin.skills().getSkill(def.skill());
            String desc = skill.map(com.celestirift.skill.Skill::description).orElse("");
            for (String l : wrap(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().stripTags(desc), 34)) {
                lore.add(line("<gray>" + l));
            }
            lore.add(Component.empty());
            lore.add(stat("Trigger", "<white>" + prettyTrigger(def.trigger())));
            if (skill.isPresent()) {
                var s = skill.get();
                String mana = s.manaCost() > 0 ? "<aqua>" + num(s.manaCost()) : "<green>free";
                String cd = s.cooldownSeconds() > 0 ? "<white>" + num(s.cooldownSeconds()) + "s" : "<white>none";
                lore.add(stat("Mana", mana + " <dark_gray>· <gray>Cooldown " + cd));
            }
        }
        lore.add(stat(tierName(def.tier()), g + "★".repeat(def.tier()) + "</gradient><dark_gray>"
                + "☆".repeat(5 - def.tier())));
        if (def.lore() != null && !def.lore().isEmpty()) {
            for (String extra : def.lore()) {
                lore.add(line("<dark_gray><italic>" + extra));
            }
        }
        lore.add(Component.empty());
        lore.add(line(def.isPassive() ? "<dark_gray>Socket into a weapon or armor · /cr socket"
                : "<dark_gray>Socket into a weapon · /cr socket"));
        return lore;
    }

    private static Component stat(String label, String value) {
        return line("<dark_gray>▸ <gray>" + label + " " + value);
    }

    private static String num(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.format(Locale.ROOT, "%.1f", d);
    }

    private static String pretty(String enumName) {
        String s = enumName.toLowerCase(Locale.ROOT).replace('_', ' ');
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String roman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            default -> String.valueOf(n);
        };
    }

    /** Word-wrap plain text to lines of at most {@code width} characters. */
    static List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            if (word.isEmpty()) continue;
            if (cur.length() > 0 && cur.length() + 1 + word.length() > width) {
                out.add(cur.toString());
                cur.setLength(0);
            }
            if (cur.length() > 0) cur.append(' ');
            cur.append(word);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    // ------------------------------------------------------------------ helpers

    private static Material matchMaterial(String primary, String fallback, Material def) {
        return Registries.itemMaterial(primary)
                .or(() -> Registries.itemMaterial(fallback))
                .orElse(def);
    }

    private static String pct(double mult) {
        long p = Math.round((mult - 1) * 100);
        return (p >= 0 ? "+" : "") + p + "%";
    }

    public String[] tierGradient(int tier) {
        String base = "gems.tiers." + tier + ".";
        String from = plugin.getConfig().getString(base + "from");
        String to = plugin.getConfig().getString(base + "to");
        if (from != null && to != null) {
            return new String[]{from, to};
        }
        return switch (tier) {
            case 2 -> new String[]{"#4ade80", "#16a34a"};
            case 3 -> new String[]{"#3b82f6", "#1d4ed8"};
            case 4 -> new String[]{"#a855f7", "#7e22ce"};
            case 5 -> new String[]{"#f59e0b", "#f97316"};
            default -> new String[]{"#b0b0b0", "#e0e0e0"};
        };
    }

    public static String tierName(int tier) {
        return switch (tier) {
            case 2 -> "Uncommon";
            case 3 -> "Rare";
            case 4 -> "Epic";
            case 5 -> "Legendary";
            default -> "Common";
        };
    }

    public static String prettyTrigger(TriggerType trigger) {
        String[] parts = trigger.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append('-');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    private static Component line(String miniMessage) {
        return ItemBuilder.mm(miniMessage);
    }
}

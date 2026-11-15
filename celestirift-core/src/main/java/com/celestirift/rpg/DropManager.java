package com.celestirift.rpg;

import com.celestirift.CelestiRift;
import com.celestirift.api.event.PlayerXpGainEvent;
import com.celestirift.skill.load.ContentFiles;
import com.celestirift.util.Registries;
import com.celestirift.validate.Diagnostics;
import com.celestirift.validate.Suggest;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * drops.yml: what mobs and bosses drop (gems, Shardvoid) and how much XP they give.
 * Every rule that matches a death applies, so a boss can use its own table plus a
 * generic one.
 *
 * <pre>
 * UndeadGems:
 *   mobs: [ZOMBIE, HUSK, SKELETON]     # entity types (any if omitted)
 *   # name: "Captain"                  # custom name contains (case-insensitive)
 *   # tag: elite                       # scoreboard tag (/tag)
 *   # boss: FrostLord                  # a bosses.yml id
 *   # worlds: [world]
 *   xp: 8
 *   looting-bonus: 0.01                # +chance per Looting level
 *   drops:
 *   - { gem: EmberGem, chance: 0.05 }
 *   - { shardvoid: 1, chance: 0.01 }
 *   - { item: demo:ruby, amount: 2, chance: 0.1 }   # an OrionRP custom item (needs OrionRP)
 * </pre>
 */
public final class DropManager implements Listener, com.celestirift.util.Restorable {

    private static final List<String> KEYS = List.of("mobs", "name", "tag", "boss", "worlds", "xp",
            "player-kill-only", "looting-bonus", "drops");

    /** One drop entry: a gem (by id), an OrionRP item (by id) or Shardvoid, with amount and chance. */
    record Drop(@Nullable String gem, @Nullable String orionItem, int amount, double chance) {}

    /** OrionRP item ids already reported as missing (logged once per reload). */
    private final Set<String> missingItems = new java.util.HashSet<>();

    record Rule(String id, Set<EntityType> mobs, @Nullable String name, @Nullable String tag,
                @Nullable String boss, Set<String> worlds, double xp, boolean playerKillOnly,
                double lootingBonus, List<Drop> drops) {

        boolean matches(LivingEntity e, @Nullable String bossId) {
            if (!mobs.isEmpty() && !mobs.contains(e.getType())) return false;
            if (!worlds.isEmpty() && !worlds.contains(e.getWorld().getName().toLowerCase(Locale.ROOT))) return false;
            if (tag != null && !e.getScoreboardTags().contains(tag)) return false;
            if (boss != null && !boss.equalsIgnoreCase(bossId)) return false;
            if (name != null) {
                var custom = e.customName();
                if (custom == null) return false;
                String plain = PlainTextComponentSerializer.plainText().serialize(custom).toLowerCase(Locale.ROOT);
                if (!plain.contains(name.toLowerCase(Locale.ROOT))) return false;
            }
            return true;
        }
    }

    private final CelestiRift plugin;
    private final List<Rule> rules = new ArrayList<>();

    public DropManager(CelestiRift plugin) {
        this.plugin = plugin;
    }

    @Override
    public Runnable snapshot() {
        var copy = new ArrayList<>(rules);
        return () -> { rules.clear(); rules.addAll(copy); };
    }

    public int reload(Diagnostics diag) {
        rules.clear();
        missingItems.clear();
        for (ContentFiles.File file : ContentFiles.load(plugin, "drops.yml", "drops", diag)) {
            for (String id : file.yaml().getKeys(false)) {
                ConfigurationSection s = file.yaml().getConfigurationSection(id);
                if (s == null) {
                    diag.warn(file.path(), file.lines().key(id), id, "is not a drop table (expected a block)");
                    continue;
                }
                Rule rule = parse(file, id, s, diag, null);
                if (rule != null) {
                    rules.add(rule);
                }
            }
        }
        // Bosses can carry their own loot (bosses/<name>.yml -> loot:), so one file holds everything.
        for (var l : plugin.bosses().loot()) {
            Rule rule = parse(l.file(), l.bossId(), l.section(), diag, l.bossId());
            if (rule != null) {
                rules.add(rule);
            }
        }
        plugin.getLogger().info("Loaded " + rules.size() + " drop table(s).");
        return rules.size();
    }

    private @Nullable Rule parse(ContentFiles.File file, String id, ConfigurationSection s, Diagnostics diag,
                                @Nullable String forcedBoss) {
        for (String key : s.getKeys(false)) {
            if (!KEYS.contains(key.toLowerCase(Locale.ROOT))) {
                diag.warn(file.path(), file.lines().find(id, key + ":"), id,
                        "unknown key '" + key + "' (ignored)", Suggest.closest(key, KEYS));
            }
        }
        Set<EntityType> mobs = new HashSet<>();
        for (String m : s.getStringList("mobs")) {
            Registries.entityType(m).ifPresentOrElse(mobs::add, () -> diag.warn(file.path(),
                    file.lines().find(id, m), id, "unknown mob type '" + m + "'"));
        }
        String boss = forcedBoss != null ? forcedBoss : s.getString("boss");
        if (forcedBoss == null && boss != null && plugin.bosses().get(boss).isEmpty()) {
            diag.warn(file.path(), file.lines().find(id, "boss"), id, "boss '" + boss + "' is not in bosses.yml",
                    Suggest.closest(boss, plugin.bosses().ids()));
        }
        Set<String> worlds = new HashSet<>();
        s.getStringList("worlds").forEach(w -> worlds.add(w.toLowerCase(Locale.ROOT)));

        List<Drop> drops = new ArrayList<>();
        for (Map<?, ?> d : s.getMapList("drops")) {
            double chance = number(d.get("chance"), 1.0);
            if (d.containsKey("gem")) {
                String gem = String.valueOf(d.get("gem"));
                if (plugin.gemDefs().get(gem).isEmpty()) {
                    diag.warn(file.path(), file.lines().find(id, gem), id, "gem '" + gem + "' is not in gems.yml",
                            Suggest.closest(gem, plugin.gemDefs().ids()));
                    continue;
                }
                drops.add(new Drop(gem, null, (int) Math.max(1, number(d.get("amount"), 1)), chance));
            } else if (d.containsKey("item")) {
                // OrionRP custom item. Its items load after CelestiRift's content, so it is only
                // checked when it drops; here only that OrionRP is there at all.
                String item = String.valueOf(d.get("item"));
                if (!com.celestirift.integration.Orion.available()) {
                    diag.warn(file.path(), file.lines().find(id, item), id,
                            "item '" + item + "' needs OrionRP, which is not installed", null);
                    continue;
                }
                drops.add(new Drop(null, item, (int) Math.max(1, number(d.get("amount"), 1)), chance));
            } else if (d.containsKey("shardvoid")) {
                drops.add(new Drop(null, null, (int) Math.max(1, number(d.get("shardvoid"), 1)), chance));
            } else {
                diag.warn(file.path(), file.lines().find(id, "drops"), id,
                        "a drop needs 'gem: <id>', 'item: <orionrp item>' or 'shardvoid: <amount>': " + d);
            }
        }
        if (mobs.isEmpty() && boss == null && s.getString("tag") == null && s.getString("name") == null) {
            diag.warn(file.path(), file.lines().key(id), id,
                    "matches EVERY mob (no mobs/name/tag/boss) - is that intended?");
        }
        return new Rule(id, mobs, s.getString("name"), s.getString("tag"), boss, worlds,
                s.getDouble("xp", 0), s.getBoolean("player-kill-only", true),
                s.getDouble("looting-bonus", 0), drops);
    }

    private static double number(@Nullable Object o, double def) {
        if (o == null) return def;
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead instanceof Player) {
            return;
        }
        Player killer = dead.getKiller();
        String bossId = plugin.bosses().bossId(dead);
        int looting = 0;
        if (killer != null) {
            looting = killer.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.LOOTING);
        }
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (Rule rule : rules) {
            if ((rule.playerKillOnly() && killer == null) || !rule.matches(dead, bossId)) {
                continue;
            }
            if (killer != null && rule.xp() > 0) {
                plugin.profiles().addXp(killer, rule.xp(), PlayerXpGainEvent.Source.MOB_KILL);
            }
            for (Drop d : rule.drops()) {
                if (rnd.nextDouble() >= d.chance() + rule.lootingBonus() * looting) {
                    continue;
                }
                ItemStack item;
                if (d.gem() != null) {
                    item = plugin.gemDefs().get(d.gem()).map(plugin.gems()::create).orElse(null);
                } else if (d.orionItem() != null) {
                    item = com.celestirift.integration.Orion.item(d.orionItem(), 1);
                    if (item == null && missingItems.add(d.orionItem())) {
                        plugin.getLogger().warning("drops.yml " + rule.id() + ": OrionRP has no item '"
                                + d.orionItem() + "' (see /orp items)");
                    }
                } else {
                    item = plugin.gems().createShardVoid();
                }
                if (item != null) {
                    item.setAmount(d.amount());
                    event.getDrops().add(item);
                }
            }
        }
    }
}

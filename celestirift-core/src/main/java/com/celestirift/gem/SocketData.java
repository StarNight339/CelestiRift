package com.celestirift.gem;

import com.celestirift.CelestiRift;
import com.celestirift.trigger.TriggerType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.celestirift.item.ItemBuilder;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads/writes a weapon's sockets and its socket-slot capacity.
 * <ul>
 *   <li>Active socket → casts {@code skill} on {@code trigger} when held.</li>
 *   <li>Passive socket → applies an {@link Augment} to the weapon's active skills.</li>
 * </ul>
 * Capacity ({@code slots}) starts at {@code gems.default-slots} and is raised by
 * Shardvoid up to {@code gems.max-slots}. Storage is a single persistent-data
 * string; a managed lore block is re-rendered on every write.
 */
public final class SocketData {

    private static final String US = "";   // field separator inside a socket
    private static final String GS = ";";         // separator between sockets

    /** One socket. Passive sockets have {@code trigger == null} and carry an augment. */
    public record Socket(SocketType type, @Nullable TriggerType trigger, String skill, Augment augment) {
        public static Socket active(TriggerType trigger, String skill) {
            return new Socket(SocketType.ACTIVE, trigger, skill, Augment.NONE);
        }
        public static Socket passive(Augment augment) {
            return new Socket(SocketType.PASSIVE, null, "", augment);
        }
        public boolean isActive() {
            return type == SocketType.ACTIVE;
        }
    }

    private final CelestiRift plugin;
    private final NamespacedKey socketsKey;
    private final NamespacedKey loreCountKey;
    private final NamespacedKey slotsKey;

    public SocketData(CelestiRift plugin) {
        this.plugin = plugin;
        this.socketsKey = new NamespacedKey(plugin, "sockets");
        this.loreCountKey = new NamespacedKey(plugin, "socket_lore_lines");
        this.slotsKey = new NamespacedKey(plugin, "slots");
    }

    // ------------------------------------------------------------------ slots

    public int defaultSlots() {
        return clampSlots(plugin.getConfig().getInt("gems.default-slots", 1));
    }

    public int maxSlots() {
        return Math.max(1, plugin.getConfig().getInt("gems.max-slots", 6));
    }

    private int clampSlots(int n) {
        return Math.max(1, Math.min(maxSlots(), n));
    }

    /** A weapon's socket capacity (defaults to config, never below its current socket count). */
    public int getSlots(@Nullable ItemStack item) {
        if (item == null || item.isEmpty()) {
            return defaultSlots();
        }
        Integer stored = item.getPersistentDataContainer()
                .get(slotsKey, PersistentDataType.INTEGER);
        int slots = (stored == null) ? defaultSlots() : stored;
        return Math.max(slots, get(item).size());
    }

    /** Max capacity including a class bonus (classes.yml {@code socket-bonus}). */
    public int maxSlots(int bonus) {
        return maxSlots() + Math.max(0, bonus);
    }

    /** Raise capacity by one (Shardvoid). Returns false if already at max. */
    public boolean upgradeSlots(ItemStack item) {
        return upgradeSlots(item, 0);
    }

    /** As {@link #upgradeSlots(ItemStack)}, allowing {@code bonus} extra slots above the config max. */
    public boolean upgradeSlots(ItemStack item, int bonus) {
        int current = getSlots(item);
        if (current >= maxSlots(bonus)) {
            return false;
        }
        setSlots(item, current + 1, bonus);
        return true;
    }

    public void setSlots(ItemStack item, int slots) {
        setSlots(item, slots, 0);
    }

    public void setSlots(ItemStack item, int slots, int bonus) {
        if (item.isEmpty()) {
            return;
        }
        int clamped = Math.max(1, Math.min(maxSlots(bonus), slots));
        item.editPersistentDataContainer(pdc -> pdc.set(slotsKey, PersistentDataType.INTEGER, clamped));
        write(item, get(item)); // refresh lore (shows x/y)
    }

    // ------------------------------------------------------------------ read

    public boolean hasSockets(@Nullable ItemStack item) {
        return !get(item).isEmpty();
    }

    /** All sockets (active + passive). */
    public List<Socket> get(@Nullable ItemStack item) {
        List<Socket> out = new ArrayList<>();
        if (item == null || item.isEmpty()) {
            return out;
        }
        String raw = item.getPersistentDataContainer()
                .get(socketsKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : raw.split(GS)) {
            Socket s = parseSocket(part);
            if (s != null) {
                out.add(s);
            }
        }
        return out;
    }

    private @Nullable Socket parseSocket(String part) {
        if (part.contains(US)) {
            String[] f = part.split(US, -1);
            if (f[0].equals("A") && f.length >= 3) {
                TriggerType t = TriggerType.fromString(f[1]);
                return (t == null) ? null : Socket.active(t, f[2]);
            }
            if (f[0].equals("P") && f.length >= 7) {
                return Socket.passive(Augment.deserialize(f, 1));
            }
            return null;
        }
        // Legacy format: "TRIGGER:skill" (active).
        int colon = part.indexOf(':');
        if (colon > 0) {
            TriggerType t = TriggerType.fromString(part.substring(0, colon));
            return (t == null) ? null : Socket.active(t, part.substring(colon + 1));
        }
        return null;
    }

    /** True if the item is worn equipment (helmet, chestplate/elytra, leggings, boots). */
    public static boolean isArmor(@Nullable ItemStack item) {
        if (item == null) {
            return false;
        }
        return switch (item.getType().getEquipmentSlot()) {
            case HEAD, CHEST, LEGS, FEET -> true;
            default -> false;
        };
    }

    /** Combined multiplier augment from passive sockets across all worn armor pieces. */
    public Augment wornAugment(Player player) {
        Augment agg = Augment.NONE;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            agg = agg.timesMultipliers(combinedAugment(piece));
        }
        return agg;
    }

    /** All passive augments across worn armor (for persistent buffs). */
    public List<Augment> wornPassives(Player player) {
        List<Augment> out = new ArrayList<>();
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            out.addAll(passives(piece));
        }
        return out;
    }

    /** Combined multiplier augment from all passive sockets. */
    public Augment combinedAugment(@Nullable ItemStack item) {
        Augment agg = Augment.NONE;
        for (Socket s : get(item)) {
            if (s.type() == SocketType.PASSIVE) {
                agg = agg.timesMultipliers(s.augment());
            }
        }
        return agg;
    }

    /** All passive augments (for applying buffs individually). */
    public List<Augment> passives(@Nullable ItemStack item) {
        List<Augment> out = new ArrayList<>();
        for (Socket s : get(item)) {
            if (s.type() == SocketType.PASSIVE) {
                out.add(s.augment());
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ mutate

    /** Append a socket if there is a free slot. */
    public boolean add(ItemStack item, Socket socket) {
        List<Socket> sockets = get(item);
        if (sockets.size() >= getSlots(item)) {
            return false;
        }
        sockets.add(socket);
        write(item, sockets);
        return true;
    }

    /** Change the trigger of the {@code activeIndex}-th ACTIVE socket. */
    public boolean setTriggerAtActive(ItemStack item, int activeIndex, TriggerType newTrigger) {
        List<Socket> sockets = get(item);
        int seen = 0;
        for (int i = 0; i < sockets.size(); i++) {
            if (sockets.get(i).isActive()) {
                if (seen == activeIndex) {
                    sockets.set(i, Socket.active(newTrigger, sockets.get(i).skill()));
                    write(item, sockets);
                    return true;
                }
                seen++;
            }
        }
        return false;
    }

    public boolean retrigger(ItemStack item, String skill, TriggerType newTrigger) {
        List<Socket> sockets = get(item);
        for (int i = 0; i < sockets.size(); i++) {
            if (sockets.get(i).isActive() && sockets.get(i).skill().equalsIgnoreCase(skill)) {
                sockets.set(i, Socket.active(newTrigger, sockets.get(i).skill()));
                write(item, sockets);
                return true;
            }
        }
        return false;
    }

    public void clear(ItemStack item) {
        write(item, new ArrayList<>());
    }

    /** Persist sockets and re-render the managed lore block. */
    public void write(ItemStack item, List<Socket> sockets) {
        if (item.isEmpty()) {
            return;
        }
        var view = item.getPersistentDataContainer();

        String encoded = null;
        if (!sockets.isEmpty()) {
            List<String> parts = new ArrayList<>();
            for (Socket s : sockets) {
                if (s.isActive()) {
                    parts.add(String.join(US, "A", s.trigger().name(), s.skill()));
                } else {
                    List<String> f = new ArrayList<>();
                    f.add("P");
                    for (String v : s.augment().toFields()) f.add(v);
                    parts.add(String.join(US, f));
                }
            }
            encoded = String.join(GS, parts);
        }

        // Rebuild the managed lore block.
        List<Component> lore = ItemBuilder.loreOf(item);
        int managed = view.getOrDefault(loreCountKey, PersistentDataType.INTEGER, 0);
        for (int i = 0; i < managed && !lore.isEmpty(); i++) {
            lore.remove(lore.size() - 1);
        }

        int capacity = Math.max(sockets.size(),
                view.getOrDefault(slotsKey, PersistentDataType.INTEGER, defaultSlots()));
        List<Component> block = new ArrayList<>();
        block.add(Component.empty());
        block.add(line("<gold><bold>Sockets</bold> <yellow>" + sockets.size() + "/" + capacity));
        for (Socket s : sockets) {
            if (s.isActive()) {
                block.add(line("<gray> ◆ <white>" + s.skill()
                        + " <dark_gray>(" + GemItems.prettyTrigger(s.trigger()) + ")"));
            } else {
                block.add(line("<light_purple> ✦ " + augmentSummary(s.augment())));
            }
        }
        lore.addAll(block);

        ItemBuilder.setLore(item, lore);
        final String socketsValue = encoded;
        final int blockSize = block.size();
        item.editPersistentDataContainer(pdc -> {
            if (socketsValue == null) {
                pdc.remove(socketsKey);
            } else {
                pdc.set(socketsKey, PersistentDataType.STRING, socketsValue);
            }
            pdc.set(loreCountKey, PersistentDataType.INTEGER, blockSize);
        });
    }

    private static String augmentSummary(Augment a) {
        List<String> parts = new ArrayList<>();
        if (a.power() != 1) parts.add("Pwr " + signPct(a.power()));
        if (a.range() != 1) parts.add("Rng " + signPct(a.range()));
        if (a.cooldown() != 1) parts.add("CD " + signPct(a.cooldown()));
        if (a.hasBuffType()) parts.add(a.buffType() + " " + a.buffLevel());
        return parts.isEmpty() ? "Augment" : String.join(", ", parts);
    }

    private static String signPct(double mult) {
        long p = Math.round((mult - 1) * 100);
        return (p >= 0 ? "+" : "") + p + "%";
    }

    private static Component line(String miniMessage) {
        return MiniMessage.miniMessage().deserialize(miniMessage, com.celestirift.integration.Orion.tags())
                .decoration(TextDecoration.ITALIC, false);
    }
}

package com.celestirift.gui;

import com.celestirift.CelestiRift;
import com.celestirift.api.event.GemFuseEvent;
import com.celestirift.api.event.GemSocketEvent;
import com.celestirift.gem.FusionRecipe;
import com.celestirift.gem.GemDef;
import com.celestirift.gem.GemItems;
import com.celestirift.gem.SkillGem;
import com.celestirift.gem.SocketData;
import com.celestirift.gem.SocketType;
import com.celestirift.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The socket workbench (/cr socket). A 6-row menu:
 * <pre>
 *  row 0   tabs: [Skill Gems] [Runes] [Fusion]                    [help]
 *  row 1   [weapon] →  socket 1 … socket 7   (what is in the weapon now)
 *  row 2   ─── label for the list below ───
 *  row 3-4 your gems of the chosen tab (click to socket), or fusion recipes
 *  row 5   [◀]  [Shardvoid +slot]  [status]  [close]  [▶]
 * </pre>
 * The weapon is the item in your main hand when you open it (or click any item in
 * your inventory). Gems come straight from your inventory, so there is nothing to
 * drag around. Right-click a socket to take its gem back out.
 */
public final class SocketGui implements InventoryHolder {

    public enum Tab { GEMS, RUNES, FUSION }

    static final int TAB_GEMS = 2, TAB_RUNES = 4, TAB_FUSION = 6, HELP = 8;
    static final int WEAPON = 9;
    static final int FIRST_SOCKET = 11, SOCKET_COUNT = 7;
    static final int LABEL = 22;
    static final int LIST_START = 27, LIST_SIZE = 18;
    static final int PREV = 45, SHARD = 47, STATUS = 49, CLOSE = 51, NEXT = 53;

    private final CelestiRift plugin;
    private final Player player;
    private final Inventory inventory;
    private Tab tab = Tab.GEMS;
    private int page;
    private @Nullable ItemStack weapon;
    private boolean weaponFromHand;
    /** What each list slot currently shows (gem stack, or a fusion recipe). */
    private final Map<Integer, Object> listing = new HashMap<>();

    public SocketGui(CelestiRift plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54,
                com.celestirift.integration.Orion.guiTitle("socket", ItemBuilder.mm("<dark_purple><bold>Socket Workbench")));
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (isWeaponCandidate(hand)) {
            weapon = hand.clone();
            weaponFromHand = true;
            player.getInventory().setItemInMainHand(null);
        }
    }

    public void open() {
        render();
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    // ------------------------------------------------------------------ rendering

    void render() {
        inventory.clear();
        listing.clear();
        ItemStack frame = pane(switch (tab) {
            case GEMS -> Material.PURPLE_STAINED_GLASS_PANE;
            case RUNES -> Material.MAGENTA_STAINED_GLASS_PANE;
            case FUSION -> Material.CYAN_STAINED_GLASS_PANE;
        });
        ItemStack dark = pane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) inventory.setItem(i, frame);
        for (int i = 18; i < 27; i++) inventory.setItem(i, frame);
        for (int i = 45; i < 54; i++) inventory.setItem(i, dark);
        inventory.setItem(10, dark);

        inventory.setItem(TAB_GEMS, tabItem(Tab.GEMS, Material.AMETHYST_SHARD, "<light_purple>Skill Gems",
                "Gems that cast a skill."));
        inventory.setItem(TAB_RUNES, tabItem(Tab.RUNES, Material.FIRE_CHARGE, "<gold>Runes",
                "Passive runes that boost the weapon's skills", "(armour accepts runes only)."));
        inventory.setItem(TAB_FUSION, tabItem(Tab.FUSION, Material.END_CRYSTAL, "<aqua>Fusion",
                "Combine gems into a stronger one."));
        inventory.setItem(HELP, ItemBuilder.of(Material.BOOK).name("<yellow>How it works").loreMini(List.of(
                "<gray>1. The item in your hand is the weapon",
                "<gray>   (or click any item in your inventory).",
                "<gray>2. Pick a tab, click a gem to socket it.",
                "<gray>3. Shardvoid adds a socket slot.",
                "<gray>Right-click a socket to remove its gem.",
                "<gray>Change triggers: hold the weapon, /cr trigger")).build());

        renderWeapon();
        renderList();
        renderBottom();
    }

    private void renderWeapon() {
        if (weapon == null) {
            inventory.setItem(WEAPON, ItemBuilder.of(Material.ITEM_FRAME).name("<gray>No weapon")
                    .loreMini(List.of("<dark_gray>Click a weapon or armour piece", "<dark_gray>in your inventory.")).build());
            for (int i = 0; i < SOCKET_COUNT; i++) {
                inventory.setItem(FIRST_SOCKET + i, pane(Material.GRAY_STAINED_GLASS_PANE));
            }
            return;
        }
        inventory.setItem(WEAPON, weapon.clone());
        List<SocketData.Socket> sockets = plugin.sockets().get(weapon);
        int capacity = plugin.sockets().getSlots(weapon);
        int max = plugin.sockets().maxSlots(classBonus());
        for (int i = 0; i < SOCKET_COUNT; i++) {
            int slot = FIRST_SOCKET + i;
            if (i < sockets.size()) {
                inventory.setItem(slot, socketItem(sockets.get(i), i));
            } else if (i < capacity) {
                inventory.setItem(slot, ItemBuilder.of(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                        .name("<white>Empty socket " + (i + 1)).loreMini(List.of("<gray>Click a gem below to fill it.")).build());
            } else if (i < max) {
                inventory.setItem(slot, ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                        .name("<dark_gray>Locked socket " + (i + 1)).loreMini(List.of("<gray>Unlock with a Shardvoid.")).build());
            } else {
                inventory.setItem(slot, pane(Material.BLACK_STAINED_GLASS_PANE));
            }
        }
    }

    /** A socketed gem shown with its normal look and tooltip. */
    private ItemStack socketItem(SocketData.Socket s, int index) {
        ItemStack item = gemFor(s);
        List<Component> lore = ItemBuilder.loreOf(item);
        lore.add(ItemBuilder.mm("<aqua>Socket " + (index + 1)
                + (s.isActive() ? " · " + GemItems.prettyTrigger(s.trigger()) : "")));
        if (plugin.getConfig().getBoolean("gems.allow-unsocket", true)) {
            lore.add(ItemBuilder.mm("<yellow>Right-click: take it out"));
        }
        ItemBuilder.setLore(item, lore);
        return item;
    }

    private void renderList() {
        String label = switch (tab) {
            case GEMS -> "<light_purple>▼ Your skill gems <gray>- click one to socket it";
            case RUNES -> "<gold>▼ Your runes <gray>- click one to socket it";
            case FUSION -> "<aqua>▼ Fusion recipes <gray>- click one you can make";
        };
        inventory.setItem(LABEL, ItemBuilder.of(Material.OAK_SIGN).name(label).build());

        List<Object> entries = new ArrayList<>(tab == Tab.FUSION ? plugin.fusions().recipes() : gemStacks());
        int pages = Math.max(1, (entries.size() + LIST_SIZE - 1) / LIST_SIZE);
        page = Math.max(0, Math.min(page, pages - 1));
        if (entries.isEmpty()) {
            inventory.setItem(LIST_START + 4, ItemBuilder.of(Material.BARRIER).name(tab == Tab.FUSION
                    ? "<gray>No fusion recipes" : "<gray>You have no " + (tab == Tab.GEMS ? "skill gems" : "runes"))
                    .build());
            return;
        }
        for (int i = 0; i < LIST_SIZE; i++) {
            int idx = page * LIST_SIZE + i;
            if (idx >= entries.size()) break;
            Object e = entries.get(idx);
            int slot = LIST_START + i;
            listing.put(slot, e);
            inventory.setItem(slot, e instanceof FusionRecipe r ? recipeItem(r) : stackItem((GemStack) e));
        }
    }

    private void renderBottom() {
        int pages = Math.max(1, ((tab == Tab.FUSION ? plugin.fusions().recipes().size() : gemStacks().size())
                + LIST_SIZE - 1) / LIST_SIZE);
        if (page > 0) inventory.setItem(PREV, ItemBuilder.of(Material.ARROW).name("<white>◀ Previous page").build());
        if (page < pages - 1) inventory.setItem(NEXT, ItemBuilder.of(Material.ARROW).name("<white>Next page ▶").build());

        int shards = count(this::isShard);
        inventory.setItem(SHARD, ItemBuilder.of(shards > 0 ? Material.ECHO_SHARD : Material.GRAY_DYE)
                .name("<gradient:#2b0055:#8000ff><bold>Shardvoid</bold></gradient> <gray>(" + shards + ")")
                .loreMini(List.of("<gray>Click: use one to add a socket slot",
                        "<dark_gray>Max " + plugin.sockets().maxSlots(classBonus()) + " slots")).glint(shards > 0).build());

        List<String> status = new ArrayList<>();
        if (weapon == null) {
            status.add("<gray>No weapon selected");
        } else {
            int used = plugin.sockets().get(weapon).size();
            int cap = plugin.sockets().getSlots(weapon);
            status.add("<gray>Sockets: <white>" + used + "/" + cap);
            status.add(SocketData.isArmor(weapon) ? "<gold>Armour: runes only" : "<gray>Weapon: gems and runes");
        }
        status.add("<dark_gray>Page " + (page + 1) + "/" + pages);
        inventory.setItem(STATUS, ItemBuilder.of(Material.NETHER_STAR).name("<white>Status").loreMini(status).build());
        inventory.setItem(CLOSE, ItemBuilder.of(Material.BARRIER).name("<red>Close").build());
    }

    // ------------------------------------------------------------------ list content

    /** Identical gems in the player's inventory, grouped. */
    private record GemStack(ItemStack sample, int amount) {}

    private List<GemStack> gemStacks() {
        List<GemStack> out = new ArrayList<>();
        for (ItemStack it : player.getInventory().getStorageContents()) {
            SkillGem g = plugin.gems().read(it);
            if (g == null || g.isPassive() != (tab == Tab.RUNES)) continue;
            boolean merged = false;
            for (int i = 0; i < out.size(); i++) {
                if (out.get(i).sample().isSimilar(it)) {
                    out.set(i, new GemStack(out.get(i).sample(), out.get(i).amount() + it.getAmount()));
                    merged = true;
                    break;
                }
            }
            if (!merged) out.add(new GemStack(it.asOne(), it.getAmount()));
        }
        return out;
    }

    private ItemStack stackItem(GemStack s) {
        ItemStack item = s.sample().clone();
        item.setAmount(Math.min(64, s.amount()));
        List<Component> lore = ItemBuilder.loreOf(item);
        lore.add(ItemBuilder.mm("<green>Click: socket into the weapon"));
        ItemBuilder.setLore(item, lore);
        return item;
    }

    private ItemStack recipeItem(FusionRecipe r) {
        ItemStack item = resultItem(r);
        List<Component> lore = ItemBuilder.loreOf(item);
        lore.add(ItemBuilder.mm("<aqua><bold>Fusion ingredients"));
        boolean all = true;
        for (String ing : r.ingredients()) {
            boolean have = countGem(ing) >= needed(r, ing);
            all &= have;
            String name = plugin.gemDefs().get(ing).map(d -> d.name() != null ? d.name() : d.id()).orElse(ing);
            lore.add(ItemBuilder.mm((have ? "<green>✔ " : "<red>✘ ") + "<reset>" + name));
        }
        lore.add(ItemBuilder.mm(all ? "<green>Click: fuse and socket" : "<dark_gray>Missing ingredients"));
        ItemBuilder.setLore(item, lore);
        if (!all) {
            item.setAmount(1);
        }
        return item;
    }

    private ItemStack resultItem(FusionRecipe r) {
        if (r.hasResultGem()) {
            var def = plugin.gemDefs().get(r.resultGem());
            if (def.isPresent()) return plugin.gems().create(def.get());
        }
        return plugin.gems().create(r.skill(), r.tier(), r.trigger());
    }

    // ------------------------------------------------------------------ clicks

    /** A click in the menu (top inventory). */
    public void clickTop(int slot, ClickType click) {
        switch (slot) {
            case TAB_GEMS -> switchTab(Tab.GEMS);
            case TAB_RUNES -> switchTab(Tab.RUNES);
            case TAB_FUSION -> switchTab(Tab.FUSION);
            case PREV -> { page--; render(); }
            case NEXT -> { page++; render(); }
            case SHARD -> useShardvoid();
            case CLOSE -> player.closeInventory();
            case WEAPON -> takeWeaponBack();
            default -> {
                if (slot >= FIRST_SOCKET && slot < FIRST_SOCKET + SOCKET_COUNT) {
                    if (click.isRightClick()) unsocket(slot - FIRST_SOCKET);
                } else if (listing.containsKey(slot)) {
                    Object e = listing.get(slot);
                    if (e instanceof FusionRecipe r) fuse(r);
                    else socket(((GemStack) e).sample());
                }
            }
        }
    }

    /** A click on an item in the player's own inventory: gems socket, anything else becomes the weapon. */
    public void clickBottom(int slot) {
        ItemStack it = player.getInventory().getItem(slot);
        if (it == null || it.getType().isAir()) return;
        if (plugin.gems().isShardVoid(it)) {
            useShardvoid();
        } else if (plugin.gems().isGem(it)) {
            socket(it.asOne());
        } else {
            ItemStack previous = weapon;
            weapon = it.clone();
            weaponFromHand = false;
            player.getInventory().setItem(slot, previous);
            click(Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.2f);
            render();
        }
    }

    private void switchTab(Tab t) {
        tab = t;
        page = 0;
        click(Sound.UI_BUTTON_CLICK, 1.4f);
        render();
    }

    private void takeWeaponBack() {
        if (weapon == null) return;
        give(weapon);
        weapon = null;
        render();
    }

    private void socket(ItemStack gemItem) {
        SkillGem gem = plugin.gems().read(gemItem);
        if (gem == null || !checkWeaponHasRoom(gem.isPassive())) return;
        if (!gem.isPassive() && plugin.skills().getSkill(gem.skill()).isEmpty()) {
            fail("This gem's skill '" + gem.skill() + "' doesn't exist");
            return;
        }
        if (!new GemSocketEvent(player, weapon.clone(), gem.id(), gem.isPassive(),
                gem.isPassive() ? null : gem.skill()).callEvent()) {
            return;
        }
        if (!takeOne(gemItem)) return;
        plugin.sockets().add(weapon, gem.isPassive()
                ? SocketData.Socket.passive(gem.augment())
                : SocketData.Socket.active(gem.trigger(), gem.skill()));
        success("Socketed " + (gem.isPassive() ? "a rune" : gem.skill()), Sound.BLOCK_ANVIL_USE);
    }

    private void fuse(FusionRecipe r) {
        for (String ing : r.ingredients()) {
            if (countGem(ing) < needed(r, ing)) {
                fail("Missing ingredients for " + r.id());
                return;
            }
        }
        SocketData.Socket result = fusionSocket(r);
        if (result == null || !checkWeaponHasRoom(!result.isActive())) return;
        if (!new GemFuseEvent(player, r.id(), r.ingredients(), r.hasResultGem() ? r.resultGem() : r.skill()).callEvent()) {
            return;
        }
        for (String ing : r.ingredients()) {
            takeOneGem(ing);
        }
        plugin.sockets().add(weapon, result);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 40, 0.6, 0.8, 0.6, 0.08);
        success("✦ Fused " + r.id() + "!", Sound.BLOCK_BEACON_ACTIVATE);
    }

    private @Nullable SocketData.Socket fusionSocket(FusionRecipe r) {
        if (r.hasResultGem()) {
            GemDef def = plugin.gemDefs().get(r.resultGem()).orElse(null);
            if (def == null) {
                fail("Result gem '" + r.resultGem() + "' is not defined");
                return null;
            }
            return def.isPassive() ? SocketData.Socket.passive(def.augment())
                    : SocketData.Socket.active(def.trigger(), def.skill());
        }
        if (plugin.skills().getSkill(r.skill()).isEmpty()) {
            fail("Fusion skill '" + r.skill() + "' is missing");
            return null;
        }
        return SocketData.Socket.active(r.trigger(), r.skill());
    }

    private void unsocket(int index) {
        if (weapon == null || !plugin.getConfig().getBoolean("gems.allow-unsocket", true)) return;
        List<SocketData.Socket> sockets = new ArrayList<>(plugin.sockets().get(weapon));
        if (index >= sockets.size()) return;
        SocketData.Socket removed = sockets.remove(index);
        plugin.sockets().write(weapon, sockets);
        give(gemFor(removed));
        success("Took the gem out of socket " + (index + 1), Sound.BLOCK_GRINDSTONE_USE);
    }

    private void useShardvoid() {
        if (weapon == null) {
            fail("Choose a weapon first");
            return;
        }
        if (count(this::isShard) == 0) {
            fail("You have no Shardvoid");
            return;
        }
        if (!plugin.sockets().upgradeSlots(weapon, classBonus())) {
            fail("This item already has the most sockets");
            return;
        }
        takeFirst(this::isShard);
        success("Socket slot added (" + plugin.sockets().getSlots(weapon) + "/"
                + plugin.sockets().maxSlots(classBonus()) + ")", Sound.BLOCK_AMETHYST_BLOCK_RESONATE);
    }

    // ------------------------------------------------------------------ helpers

    private boolean checkWeaponHasRoom(boolean passive) {
        if (weapon == null) {
            fail("Choose a weapon first (click it in your inventory)");
            return false;
        }
        if (SocketData.isArmor(weapon) && !passive) {
            fail("Armour only accepts runes");
            return false;
        }
        if (plugin.sockets().get(weapon).size() >= plugin.sockets().getSlots(weapon)) {
            fail("No free socket - use a Shardvoid");
            return false;
        }
        return true;
    }

    /** The gem item a socket came from (its definition when one matches, else an auto gem). */
    private ItemStack gemFor(SocketData.Socket s) {
        for (String id : plugin.gemDefs().ids()) {
            GemDef d = plugin.gemDefs().get(id).orElse(null);
            if (d == null) continue;
            if (s.isActive() && !d.isPassive() && d.skill().equalsIgnoreCase(s.skill())) {
                return plugin.gems().create(new GemDef(d.id(), d.type(), d.skill(), d.tier(), s.trigger(),
                        d.augment(), d.material(), d.name(), d.lore(), d.model()));
            }
            if (!s.isActive() && d.isPassive() && d.augment().equals(s.augment())) {
                return plugin.gems().create(d);
            }
        }
        return s.isActive() ? plugin.gems().create(s.skill(), 3, s.trigger())
                : plugin.gems().create(new GemDef("", SocketType.PASSIVE, "", 3,
                        com.celestirift.trigger.TriggerType.RIGHT_CLICK, s.augment(), null, null, List.of()));
    }

    private int classBonus() {
        return plugin.profiles().rpgClass(player).map(com.celestirift.rpg.RpgClass::socketBonus).orElse(0);
    }

    private boolean isShard(@Nullable ItemStack it) {
        return plugin.gems().isShardVoid(it);
    }

    private boolean isWeaponCandidate(@Nullable ItemStack it) {
        return it != null && !it.getType().isAir() && !plugin.gems().isGem(it) && !plugin.gems().isShardVoid(it);
    }

    private int count(java.util.function.Predicate<ItemStack> test) {
        int n = 0;
        for (ItemStack it : player.getInventory().getStorageContents()) {
            if (it != null && test.test(it)) n += it.getAmount();
        }
        return n;
    }

    private int countGem(String id) {
        return count(it -> id.equalsIgnoreCase(plugin.gems().getId(it)));
    }

    private static int needed(FusionRecipe r, String ing) {
        return (int) r.ingredients().stream().filter(ing::equals).count();
    }

    private boolean takeOne(ItemStack like) {
        if (takeFirst(it -> it.isSimilar(like))) {
            return true;
        }
        fail("That gem is no longer in your inventory");
        return false;
    }

    private void takeOneGem(String id) {
        takeFirst(it -> id.equalsIgnoreCase(plugin.gems().getId(it)));
    }

    /** Remove one item matching {@code test} from the storage slots (by index, so it always sticks). */
    private boolean takeFirst(java.util.function.Predicate<ItemStack> test) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getStorageContents().length; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && !it.getType().isAir() && test.test(it)) {
                if (it.getAmount() <= 1) {
                    inv.setItem(i, null);
                } else {
                    it.setAmount(it.getAmount() - 1);
                    inv.setItem(i, it);
                }
                return true;
            }
        }
        return false;
    }

    private void give(ItemStack item) {
        for (ItemStack left : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), left);
        }
    }

    private void success(String msg, Sound sound) {
        plugin.messages().actionBar(player, ItemBuilder.mm("<green>" + msg));
        click(sound, 1.2f);
        render();
    }

    private void fail(String msg) {
        plugin.messages().actionBar(player, ItemBuilder.mm("<red>" + msg));
        click(Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f);
    }

    private void click(Sound sound, float pitch) {
        player.playSound(player, sound, 0.8f, pitch);
    }

    private ItemStack tabItem(Tab t, Material icon, String name, String... desc) {
        List<String> lore = new ArrayList<>();
        for (String d : desc) lore.add("<gray>" + d);
        lore.add(tab == t ? "<green>● Selected" : "<yellow>Click to open");
        return ItemBuilder.of(icon).name((tab == t ? "<bold>" : "") + name).loreMini(lore).glint(tab == t).build();
    }

    private static ItemStack pane(Material m) {
        return ItemBuilder.of(m).name(Component.text(" ")).build();
    }

    /** Give the weapon back when the menu closes (to the hand it came from if free). */
    public void close() {
        if (weapon == null) return;
        if (weaponFromHand && player.getInventory().getItemInMainHand().getType().isAir()) {
            player.getInventory().setItemInMainHand(weapon);
        } else {
            give(weapon);
        }
        weapon = null;
    }

    public Tab tab() {
        return tab;
    }
}

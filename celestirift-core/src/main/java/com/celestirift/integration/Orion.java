package com.celestirift.integration;

import com.celestirift.CelestiRift;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Everything CelestiRift uses from OrionRP (glyphs, custom items, GUI backgrounds, HUD). Safe to
 * call without OrionRP: every method has a plain fallback. This class never references the OrionRP
 * API itself; {@link OrionBridge} does, and is only loaded when OrionRP is enabled.
 */
public final class Orion {

    /** What CelestiRift needs from OrionRP. */
    interface Bridge {
        TagResolver tags();

        @Nullable Component glyph(String nameOrId);

        @Nullable ItemStack item(String id, int amount);

        @Nullable String itemId(@Nullable ItemStack item);

        @Nullable Component guiTitle(String background, Component title);

        void hud(Player player, String layer, @Nullable Component content, int x, int ticks);

        int width(Component text);
    }

    private static volatile @Nullable Bridge bridge;
    private static volatile @Nullable CelestiRift plugin;

    private Orion() {
    }

    /** Connect to OrionRP if it is enabled (call in onEnable; OrionRP is a softdepend, so it enabled first). */
    public static void install(CelestiRift p) {
        plugin = p;
        if (!p.getServer().getPluginManager().isPluginEnabled("OrionRP")) {
            bridge = null;
            return;
        }
        try {
            bridge = new OrionBridge(p);
            p.getLogger().info("OrionRP found: glyphs, OrionRP items, the 'skill' action, GUI backgrounds and the HUD are available.");
        } catch (LinkageError | RuntimeException e) {
            bridge = null;
            p.getLogger().warning("OrionRP is installed but its API could not be used (" + e
                    + ") - update OrionRP. Running without it.");
        }
    }

    public static boolean available() {
        return bridge != null;
    }

    /** MiniMessage resolver for {@code <glyph:name>}; empty without OrionRP. */
    public static TagResolver tags() {
        Bridge b = bridge;
        return b == null ? TagResolver.empty() : b.tags();
    }

    public static @Nullable Component glyph(String nameOrId) {
        Bridge b = bridge;
        return b == null ? null : b.glyph(nameOrId);
    }

    /** An OrionRP custom item ({@code ns:item}), or null if OrionRP or the item is missing. */
    public static @Nullable ItemStack item(String id, int amount) {
        Bridge b = bridge;
        return b == null ? null : b.item(id, amount);
    }

    public static @Nullable String itemId(@Nullable ItemStack item) {
        Bridge b = bridge;
        return b == null ? null : b.itemId(item);
    }

    /**
     * A GUI title with the background set at {@code orionrp.gui-backgrounds.<key>} in config.yml,
     * or {@code title} unchanged when none is set or OrionRP / the image is missing.
     */
    public static Component guiTitle(String key, Component title) {
        Bridge b = bridge;
        CelestiRift p = plugin;
        if (b == null || p == null) {
            return title;
        }
        String background = p.getConfig().getString("orionrp.gui-backgrounds." + key, "").trim();
        if (background.isEmpty()) {
            return title;
        }
        Component withBackground = b.guiTitle(background, title);
        return withBackground == null ? title : withBackground;
    }

    /** Whether action bar text goes through OrionRP's HUD ({@code orionrp.hud.enabled}). */
    public static boolean hud() {
        CelestiRift p = plugin;
        return bridge != null && p != null && p.getConfig().getBoolean("orionrp.hud.enabled", true);
    }

    public static void hud(Player player, String layer, @Nullable Component content, int x, int ticks) {
        Bridge b = bridge;
        if (b != null) {
            b.hud(player, layer, content, x, ticks);
        }
    }

    public static int width(Component text) {
        Bridge b = bridge;
        return b == null ? 0 : b.width(text);
    }
}

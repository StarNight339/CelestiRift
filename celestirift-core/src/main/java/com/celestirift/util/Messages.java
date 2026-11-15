package com.celestirift.util;

import com.celestirift.integration.Orion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Player-facing text from {@code messages.yml} (MiniMessage). Missing keys fall back
 * to the defaults bundled in the jar, so old message files keep working after updates.
 * An empty string disables a message.
 */
public final class Messages {

    private final Plugin plugin;
    private YamlConfiguration messages = new YamlConfiguration();
    private YamlConfiguration defaults = new YamlConfiguration();

    public Messages(Plugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
        var in = plugin.getResource("messages.yml");
        if (in != null) {
            defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            messages.setDefaults(defaults);
        }
    }

    /** Placeholder helper: {@code <name>} → value (inserted as plain text, not parsed). */
    public static TagResolver p(String name, Object value) {
        return Placeholder.unparsed(name, String.valueOf(value));
    }

    /** Placeholder whose value is itself MiniMessage (e.g. a coloured class name). */
    public static TagResolver mm(String name, String miniMessage) {
        return Placeholder.parsed(name, miniMessage == null ? "" : miniMessage);
    }

    public static String fmt(double d) {
        return String.format(java.util.Locale.ROOT, "%.0f", d);
    }

    public static String fmt1(double d) {
        return String.format(java.util.Locale.ROOT, "%.1f", d);
    }

    /** The rendered message, or null if it's disabled (empty). */
    public Component get(String key, TagResolver... placeholders) {
        // getString(path) (no explicit default) falls back to the bundled defaults.
        String raw = messages.getString(key);
        if (raw == null) {
            raw = key;
        }
        if (raw.isEmpty()) {
            return null;
        }
        return MiniMessage.miniMessage().deserialize(raw, TagResolver.resolver(TagResolver.resolver(placeholders), Orion.tags()));
    }

    public void send(CommandSender to, String key, TagResolver... placeholders) {
        Component c = get(key, placeholders);
        if (c != null) {
            to.sendMessage(c);
        }
    }

    public void actionBar(Player to, String key, TagResolver... placeholders) {
        Component c = get(key, placeholders);
        if (c != null) {
            actionBar(to, c);
        }
    }

    /**
     * An action bar message. With OrionRP's HUD on, it is a centered HUD layer for
     * {@code orionrp.hud.message-ticks}, so it and the mana bar don't overwrite each other.
     */
    public void actionBar(Player to, Component c) {
        if (Orion.hud()) {
            Orion.hud(to, "celestirift_message", c, -Orion.width(c) / 2,
                    Math.max(1, plugin.getConfig().getInt("orionrp.hud.message-ticks", 40)));
        } else {
            to.sendActionBar(c);
        }
    }
}

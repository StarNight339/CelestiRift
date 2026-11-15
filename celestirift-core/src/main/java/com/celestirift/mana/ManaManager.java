package com.celestirift.mana;

import com.celestirift.CelestiRift;
import com.celestirift.api.event.ManaChangeEvent;
import com.celestirift.data.PlayerDataStore;
import com.celestirift.integration.Orion;
import com.celestirift.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player mana pool. Values live in memory while a player is online and are
 * persisted through a {@link PlayerDataStore} on quit/shutdown, so mana survives
 * relogs and restarts. Every change to an online player's mana fires a
 * {@link ManaChangeEvent}.
 */
public final class ManaManager {

    private final CelestiRift plugin;
    private final PlayerDataStore store;
    private final Map<UUID, Double> current = new HashMap<>();

    private boolean enabled;
    private double maxMana;
    private double regenPerSecond;
    private boolean showActionBar;

    public ManaManager(CelestiRift plugin, PlayerDataStore store) {
        this.plugin = plugin;
        this.store = store;
        loadConfig();
    }

    /** (Re)read mana settings from config.yml. Called on enable and on /cr reload. */
    public void loadConfig() {
        var cfg = plugin.getConfig();
        this.enabled = cfg.getBoolean("mana.enabled", true);
        this.maxMana = cfg.getDouble("mana.max", 100.0);
        this.regenPerSecond = cfg.getDouble("mana.regen-per-second", 5.0);
        this.showActionBar = cfg.getBoolean("mana.show-actionbar", true);
    }

    public boolean enabled() { return enabled; }
    public boolean showActionBar() { return showActionBar; }
    public double regenPerSecond() { return regenPerSecond; }

    /** Global max mana (config). */
    public double max() { return maxMana; }

    /** A player's max mana: from their class (scaled by level) if they have one, else config. */
    public double max(UUID player) {
        var c = playerClass(player);
        return c != null ? Math.max(1, c.maxMana(c.level)) : maxMana;
    }

    /** A player's regen per second: from their class (scaled by level) if they have one, else config. */
    public double regenPerSecond(UUID player) {
        var c = playerClass(player);
        return c != null ? Math.max(0, c.regen(c.level)) : regenPerSecond;
    }

    /** Class + level of an online player, or null. */
    private @org.jetbrains.annotations.Nullable LeveledClass playerClass(UUID id) {
        if (plugin.profiles() == null) {
            return null;
        }
        var profile = plugin.profiles().getIfOnline(id);
        if (profile == null) {
            return null;
        }
        return plugin.classes().get(profile.classId())
                .map(c -> new LeveledClass(c, profile.level())).orElse(null);
    }

    private record LeveledClass(com.celestirift.rpg.RpgClass cls, int level) {
        double maxMana(int lvl) { return cls.maxMana(lvl); }
        double regen(int lvl) { return cls.regen(lvl); }
    }

    /** Current mana, loading the persisted value (or max) on first access. */
    public double get(UUID player) {
        return current.computeIfAbsent(player, id -> {
            Player online = Bukkit.getPlayer(id);
            double stored = online != null ? store.loadMana(online).orElse(max(id)) : max(id);
            return clamp(id, stored);
        });
    }

    public boolean has(UUID player, double cost) {
        return !enabled || get(player) >= cost;
    }

    /** Deduct mana (no-op when disabled or cost <= 0). Never drops below zero. */
    public void consume(UUID player, double cost) {
        if (!enabled || cost <= 0) {
            return;
        }
        change(player, get(player) - cost, ManaChangeEvent.Reason.CAST);
    }

    /** Set mana from a command or the API. */
    public void set(UUID player, double amount) {
        change(player, amount, ManaChangeEvent.Reason.SET);
    }

    /** Called once per second by the regen task. Returns true if mana went up. */
    public boolean regen(Player player) {
        if (!enabled) {
            return false;
        }
        UUID id = player.getUniqueId();
        double now = get(id);
        if (now >= max(id)) {
            return false;
        }
        return change(id, now + regenPerSecond(id), ManaChangeEvent.Reason.REGEN);
    }

    /** Apply a change through {@link ManaChangeEvent}. Returns true if the value changed. */
    private boolean change(UUID id, double target, ManaChangeEvent.Reason reason) {
        double old = get(id);
        double next = clamp(id, target);
        Player online = Bukkit.getPlayer(id);
        if (online != null) {
            ManaChangeEvent event = new ManaChangeEvent(online, old, next, reason);
            if (!event.callEvent()) {
                return false;
            }
            next = clamp(id, event.getNewMana());
        }
        current.put(id, next);
        return next != old;
    }

    private double clamp(UUID id, double v) {
        return Math.max(0, Math.min(max(id), v));
    }

    // ------------------------------------------------------------------ persistence

    /** Persist and forget a player's mana (on quit). */
    public void unload(Player player) {
        Double v = current.remove(player.getUniqueId());
        if (v != null) {
            store.saveMana(player, v);
        }
    }

    /** Persist all online players (on shutdown). */
    public void saveAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Double v = current.get(p.getUniqueId());
            if (v != null) {
                store.saveMana(p, v);
            }
        }
    }

    /**
     * Show the mana action bar to a player. With OrionRP's HUD and the mana glyphs
     * ({@code orionrp.hud.mana-glyphs}), it is a bar of icons on its own HUD layer;
     * otherwise the {@code mana.actionbar} message.
     */
    public void showBar(Player player) {
        if (Orion.hud()) {
            Component bar = glyphBar(get(player.getUniqueId()), max(player.getUniqueId()));
            if (bar != null) {
                var cfg = plugin.getConfig();
                int x = cfg.isInt("orionrp.hud.mana-x") ? cfg.getInt("orionrp.hud.mana-x") : -Orion.width(bar) / 2;
                Orion.hud(player, "celestirift_mana", bar, x, 50);
                return;
            }
        }
        plugin.messages().actionBar(player, "mana.actionbar",
                Messages.p("mana", Messages.fmt(get(player.getUniqueId()))),
                Messages.p("max", Messages.fmt(max(player.getUniqueId()))));
    }

    /** Icons for {@code mana} out of {@code max}, or null if the full/empty glyphs are missing. */
    private @Nullable Component glyphBar(double mana, double max) {
        var cfg = plugin.getConfig();
        Component full = Orion.glyph(cfg.getString("orionrp.hud.mana-glyphs.full", "mana_full"));
        Component empty = Orion.glyph(cfg.getString("orionrp.hud.mana-glyphs.empty", "mana_empty"));
        Component half = Orion.glyph(cfg.getString("orionrp.hud.mana-glyphs.half", "mana_half"));
        if (full == null || empty == null || max <= 0) {
            return null;
        }
        int icons = Math.clamp(cfg.getInt("orionrp.hud.mana-icons", 10), 1, 40);
        double per = max / icons;
        TextComponent.Builder bar = Component.text();
        for (int i = 0; i < icons; i++) {
            double left = mana - i * per;
            bar.append(left >= per ? full : left >= per / 2 && half != null ? half : empty);
        }
        return bar.build();
    }
}

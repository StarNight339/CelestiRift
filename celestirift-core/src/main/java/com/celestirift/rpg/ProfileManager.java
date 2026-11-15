package com.celestirift.rpg;

import com.celestirift.CelestiRift;
import com.celestirift.api.event.PlayerClassChangeEvent;
import com.celestirift.api.event.PlayerLevelUpEvent;
import com.celestirift.api.event.PlayerXpGainEvent;
import com.celestirift.data.PlayerDataStore;
import com.celestirift.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Levels, XP and classes of online players. Profiles load on first access and are
 * saved through the {@link PlayerDataStore} on quit and shutdown.
 */
public final class ProfileManager {

    private final CelestiRift plugin;
    private final PlayerDataStore store;
    private final Map<UUID, PlayerProfile> profiles = new HashMap<>();

    private boolean enabled;
    private LevelCurve curve = new LevelCurve(100, 1.5, 50);
    private double xpPerCast;
    private boolean restoreMana;
    private boolean allowClassChange;
    private @Nullable String levelUpSound;

    public ProfileManager(CelestiRift plugin, PlayerDataStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public void loadConfig() {
        var c = plugin.getConfig();
        enabled = c.getBoolean("leveling.enabled", true);
        curve = new LevelCurve(c.getDouble("leveling.xp-curve.base", 100),
                c.getDouble("leveling.xp-curve.exponent", 1.5), c.getInt("leveling.max-level", 50));
        xpPerCast = c.getDouble("leveling.xp.skill-cast", 1);
        restoreMana = c.getBoolean("leveling.level-up.restore-mana", true);
        levelUpSound = c.getString("leveling.level-up.sound", "ui.toast.challenge_complete");
        allowClassChange = c.getBoolean("classes.allow-change", false);
    }

    public boolean enabled() { return enabled; }
    public LevelCurve curve() { return curve; }
    public double xpPerCast() { return xpPerCast; }
    public boolean allowClassChange() { return allowClassChange; }

    public PlayerProfile get(Player player) {
        return profiles.computeIfAbsent(player.getUniqueId(), id -> store.loadProfile(player));
    }

    /** Profile of an online player by id, or null. */
    public @Nullable PlayerProfile getIfOnline(UUID id) {
        Player p = Bukkit.getPlayer(id);
        return p == null ? null : get(p);
    }

    public int level(Player player) {
        return get(player).level();
    }

    public Optional<RpgClass> rpgClass(Player player) {
        return plugin.classes().get(get(player).classId());
    }

    // ------------------------------------------------------------------ XP & levels

    /** Add XP through {@link PlayerXpGainEvent}; handles level-ups. */
    public void addXp(Player player, double amount, PlayerXpGainEvent.Source source) {
        if (!enabled || amount <= 0) {
            return;
        }
        PlayerXpGainEvent event = new PlayerXpGainEvent(player, amount, source);
        if (!event.callEvent() || event.getAmount() <= 0) {
            return;
        }
        PlayerProfile profile = get(player);
        int before = profile.level();
        LevelCurve.Result r = curve.add(before, profile.xp(), event.getAmount());
        profile.level(r.level());
        profile.xp(r.xp());

        if (source != PlayerXpGainEvent.Source.SKILL_CAST) {
            plugin.messages().actionBar(player, "rpg.xp-gain",
                    Messages.p("xp", Messages.fmt(event.getAmount())),
                    Messages.p("current", Messages.fmt(r.xp())),
                    Messages.p("needed", needed(r.level())));
        }
        for (int lvl = before + 1; lvl <= r.level(); lvl++) {
            new PlayerLevelUpEvent(player, lvl - 1, lvl).callEvent();
        }
        if (r.levelsGained() > 0) {
            celebrate(player, r.level());
        }
    }

    /** Set a level directly (XP resets to 0). */
    public void setLevel(Player player, int level) {
        PlayerProfile profile = get(player);
        profile.level(Math.min(curve.maxLevel(), Math.max(1, level)));
        profile.xp(0);
        plugin.mana().set(player.getUniqueId(), plugin.mana().get(player.getUniqueId())); // re-clamp to new max
    }

    private String needed(int level) {
        double n = curve.xpToNext(level);
        return Double.isInfinite(n) ? "MAX" : Messages.fmt(n);
    }

    /** Level-up feedback: one action-bar line + a sound (no titles or chat spam). */
    private void celebrate(Player player, int level) {
        if (restoreMana) {
            plugin.mana().set(player.getUniqueId(), plugin.mana().max(player.getUniqueId()));
        }
        plugin.messages().actionBar(player, "rpg.level-up", Messages.p("level", level));
        if (levelUpSound != null && !levelUpSound.isBlank()) {
            player.playSound(player.getLocation(), levelUpSound, 1f, 1f);
        }
    }

    // ------------------------------------------------------------------ classes

    /**
     * Change a player's class. {@code giveStartingGems} hands out the class's starting gems
     * (when choosing, not when an admin resets). Returns false if cancelled/unknown.
     */
    public boolean setClass(Player player, @Nullable String classId, boolean giveStartingGems) {
        PlayerProfile profile = get(player);
        RpgClass target = null;
        if (classId != null) {
            target = plugin.classes().get(classId).orElse(null);
            if (target == null) {
                return false;
            }
        }
        String newId = target == null ? null : target.id();
        if (java.util.Objects.equals(profile.classId(), newId)) {
            return false;
        }
        if (!new PlayerClassChangeEvent(player, profile.classId(), newId).callEvent()) {
            return false;
        }
        profile.classId(newId);
        plugin.mana().set(player.getUniqueId(), plugin.mana().max(player.getUniqueId()));
        if (target != null && giveStartingGems) {
            for (String gemId : target.startingGems()) {
                plugin.gemDefs().get(gemId).ifPresent(def -> {
                    for (var leftover : player.getInventory().addItem(plugin.gems().create(def)).values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    }
                });
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ persistence

    public void unload(Player player) {
        PlayerProfile p = profiles.remove(player.getUniqueId());
        if (p != null) {
            store.saveProfile(player, p);
        }
    }

    public void saveAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = profiles.get(p.getUniqueId());
            if (profile != null) {
                store.saveProfile(p, profile);
            }
        }
    }
}

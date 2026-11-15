package com.celestirift.data;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.OptionalDouble;

/** {@link PlayerDataStore} backed by the player's persistent data container. */
public final class PdcPlayerDataStore implements PlayerDataStore {

    private final NamespacedKey manaKey;
    private final NamespacedKey levelKey;
    private final NamespacedKey xpKey;
    private final NamespacedKey classKey;

    public PdcPlayerDataStore(Plugin plugin) {
        this.manaKey = new NamespacedKey(plugin, "mana");
        this.levelKey = new NamespacedKey(plugin, "level");
        this.xpKey = new NamespacedKey(plugin, "xp");
        this.classKey = new NamespacedKey(plugin, "class");
    }

    @Override
    public com.celestirift.rpg.PlayerProfile loadProfile(Player player) {
        var pdc = player.getPersistentDataContainer();
        return new com.celestirift.rpg.PlayerProfile(
                pdc.getOrDefault(levelKey, PersistentDataType.INTEGER, 1),
                pdc.getOrDefault(xpKey, PersistentDataType.DOUBLE, 0.0),
                pdc.get(classKey, PersistentDataType.STRING));
    }

    @Override
    public void saveProfile(Player player, com.celestirift.rpg.PlayerProfile profile) {
        var pdc = player.getPersistentDataContainer();
        pdc.set(levelKey, PersistentDataType.INTEGER, profile.level());
        pdc.set(xpKey, PersistentDataType.DOUBLE, profile.xp());
        if (profile.classId() == null) {
            pdc.remove(classKey);
        } else {
            pdc.set(classKey, PersistentDataType.STRING, profile.classId());
        }
    }

    @Override
    public OptionalDouble loadMana(Player player) {
        Double v = player.getPersistentDataContainer().get(manaKey, PersistentDataType.DOUBLE);
        return v == null ? OptionalDouble.empty() : OptionalDouble.of(v);
    }

    @Override
    public void saveMana(Player player, double mana) {
        player.getPersistentDataContainer().set(manaKey, PersistentDataType.DOUBLE, mana);
    }
}

package com.celestirift.data;

import org.bukkit.entity.Player;

import java.util.OptionalDouble;

/**
 * Persistent per-player data. The default implementation stores values on the
 * player's own persistent data container (saved with the vanilla playerdata file),
 * so no database is needed. Swap in an SQL implementation for networks later.
 */
public interface PlayerDataStore {

    OptionalDouble loadMana(Player player);

    void saveMana(Player player, double mana);

    com.celestirift.rpg.PlayerProfile loadProfile(Player player);

    void saveProfile(Player player, com.celestirift.rpg.PlayerProfile profile);
}

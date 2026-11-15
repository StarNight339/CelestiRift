package com.celestirift.summon;

import com.celestirift.CelestiRift;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Tags summoned entities with the UUID of whoever summoned them (persistent
 * data, so it survives chunk unloads and restarts). {@code SummonListener} uses
 * the tag to keep minions from mauling their own summoner.
 */
public final class Summons {

    private final NamespacedKey ownerKey;

    public Summons(CelestiRift plugin) {
        this.ownerKey = new NamespacedKey(plugin, "summon_owner");
    }

    /** Mark {@code entity} as a summon belonging to {@code owner}. */
    public void setOwner(Entity entity, UUID owner) {
        entity.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.toString());
    }

    /** The UUID that summoned this entity, or null if it isn't a summon. */
    public @Nullable UUID owner(Entity entity) {
        String raw = entity.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** True if {@code entity} is a summon owned by {@code owner}. */
    public boolean isOwnedBy(Entity entity, UUID owner) {
        return owner.equals(owner(entity));
    }
}

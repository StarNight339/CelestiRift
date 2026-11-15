package com.celestirift.util;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Registry-backed lookups for names written in YAML. Accepts both the legacy
 * enum style ({@code INCREASE_DAMAGE}, {@code FLAME}) and namespaced keys
 * ({@code minecraft:strength}, {@code mypack:custom}), so configs keep working
 * across versions and with datapack-added entries.
 */
public final class Registries {

    private Registries() {}

    /** Old Bukkit potion names that differ from their vanilla keys. */
    private static final Map<String, String> POTION_ALIASES = Map.ofEntries(
            Map.entry("increase_damage", "strength"),
            Map.entry("damage_resistance", "resistance"),
            Map.entry("slow", "slowness"),
            Map.entry("fast_digging", "haste"),
            Map.entry("slow_digging", "mining_fatigue"),
            Map.entry("jump", "jump_boost"),
            Map.entry("confusion", "nausea"),
            Map.entry("heal", "instant_health"),
            Map.entry("harm", "instant_damage"));

    private static final Map<String, String> PARTICLE_ALIASES = Map.of(
            "redstone", "dust");

    public static Optional<PotionEffectType> potion(@Nullable String name) {
        return lookup(RegistryKey.MOB_EFFECT, name, POTION_ALIASES);
    }

    public static Optional<Particle> particle(@Nullable String name) {
        return lookup(RegistryKey.PARTICLE_TYPE, name, PARTICLE_ALIASES);
    }

    public static Optional<EntityType> entityType(@Nullable String name) {
        return lookup(RegistryKey.ENTITY_TYPE, name, Map.of());
    }

    /** An item material (blocks that aren't items are rejected). */
    public static Optional<Material> itemMaterial(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        Material m = Material.matchMaterial(name.trim());
        return (m != null && m.isItem()) ? Optional.of(m) : Optional.empty();
    }

    /** Parse a namespaced key; a bare value gets the {@code minecraft:} namespace. */
    public static Optional<Key> key(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Key.key(raw.trim().toLowerCase(Locale.ROOT)));
        } catch (InvalidKeyException e) {
            return Optional.empty();
        }
    }

    private static <T extends Keyed> Optional<T> lookup(RegistryKey<T> registry, @Nullable String name,
                                                        Map<String, String> aliases) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        normalized = aliases.getOrDefault(normalized, normalized);
        Optional<Key> key = key(normalized);
        if (key.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(RegistryAccess.registryAccess().getRegistry(registry).get(key.get()));
    }
}

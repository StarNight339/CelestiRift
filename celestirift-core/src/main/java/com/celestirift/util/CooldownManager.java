package com.celestirift.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tracks per-player, per-skill cooldowns in memory. */
public final class CooldownManager {

    // playerId -> (skillName -> readyAtEpochMillis)
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    /** Remaining cooldown in seconds, or 0 if ready. */
    public double remaining(UUID player, String skill) {
        Map<String, Long> perSkill = cooldowns.get(player);
        if (perSkill == null) {
            return 0;
        }
        Long readyAt = perSkill.get(skill.toLowerCase());
        if (readyAt == null) {
            return 0;
        }
        long diff = readyAt - System.currentTimeMillis();
        return diff <= 0 ? 0 : diff / 1000.0;
    }

    public boolean isReady(UUID player, String skill) {
        return remaining(player, skill) <= 0;
    }

    public void set(UUID player, String skill, double seconds) {
        if (seconds <= 0) {
            return;
        }
        cooldowns.computeIfAbsent(player, k -> new HashMap<>())
                .put(skill.toLowerCase(), System.currentTimeMillis() + (long) (seconds * 1000));
    }

    public void clear(UUID player) {
        cooldowns.remove(player);
    }
}

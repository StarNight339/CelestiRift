package com.celestirift.aura;

import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.NumberValue;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Settings of one {@code aura} mechanic line, e.g.
 * <pre>
 * - aura: { id: Mark, duration: 100, maxstacks: 5, type: debuff, damage-taken: 0.05 }
 *   target: target
 * </pre>
 * Modifiers are per stack: {@code damage-taken: 0.05} with 3 stacks = +15% damage taken.
 */
public record AuraSpec(
        String id,
        String type,
        NumberValue duration,
        int stacks,
        int maxStacks,
        boolean refresh,
        int interval,
        @Nullable String onTick,
        @Nullable String onHit,
        @Nullable String onDamaged,
        @Nullable String onEnd,
        int onHitConsume,
        double damageDealt,
        double damageTaken,
        double speed,
        NumberValue shield,
        @Nullable String particle) {

    public static final List<String> TYPES = List.of("buff", "debuff");

    public static AuraSpec read(ConfigMap cfg) {
        String id = cfg.getString("aura", "id", "name").toLowerCase(Locale.ROOT);
        return new AuraSpec(
                id,
                cfg.getChoice("buff", TYPES, "type"),
                cfg.getNumber(100, "duration", "d", "ticks"),
                Math.max(1, cfg.getInt(1, "stacks", "add")),
                Math.max(1, cfg.getInt(1, "maxstacks", "max")),
                cfg.getBoolean(true, "refresh"),
                Math.max(0, cfg.getInt(20, "interval", "every")),
                blank(cfg.getString("", "ontick")),
                blank(cfg.getString("", "onhit")),
                blank(cfg.getString("", "ondamaged")),
                blank(cfg.getString("", "onend", "onexpire")),
                Math.max(0, cfg.getInt(0, "onhit-consume", "consume")),
                cfg.getDouble(0, "damage-dealt", "dealt"),
                cfg.getDouble(0, "damage-taken", "taken"),
                cfg.getDouble(0, "speed"),
                cfg.getNumber(0, "shield"),
                blank(cfg.getString("", "particle")));
    }

    private static @Nullable String blank(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}

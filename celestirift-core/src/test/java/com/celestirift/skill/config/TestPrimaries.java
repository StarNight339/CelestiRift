package com.celestirift.skill.config;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

/**
 * Shorthand attributes and alias canonicalisation matching the real built-in
 * components (their first-read attribute), without needing a running server.
 */
final class TestPrimaries implements StructuredLine.Primaries {

    static final Map<String, String> MECHANICS = Map.ofEntries(
            Map.entry("message", "msg"), Map.entry("msg", "msg"),
            Map.entry("heal", "amount"), Map.entry("damage", "amount"),
            Map.entry("delay", "ticks"), Map.entry("skill", "skill"),
            Map.entry("sound", "sound"), Map.entry("particle", "type"),
            Map.entry("potion", "type"), Map.entry("projectile", "type"),
            Map.entry("lightning", "damage"), Map.entry("leap", "forward"),
            Map.entry("fly", "duration"), Map.entry("summon", "type"),
            Map.entry("stealitem", "slot"), Map.entry("push", "velocity"));

    static final Map<String, String> TARGETERS = Map.of(
            "nearbyentities", "radius", "nearby", "radius",
            "target", "range", "targetlocation", "range");

    static final Map<String, String> TARGETER_ALIASES = Map.of("nearby", "nearbyentities");

    @Override
    public @Nullable String mechanic(String name) {
        return MECHANICS.get(name.toLowerCase(Locale.ROOT));
    }

    @Override
    public @Nullable String targeter(String name) {
        return TARGETERS.get(name.toLowerCase(Locale.ROOT));
    }

    @Override
    public @Nullable String condition(String name) {
        return name.equalsIgnoreCase("health") ? "value" : null;
    }

    /** Canonical targeter name: lowercase, aliases resolved, absent = self. */
    static String canonicalTargeter(@Nullable String name) {
        if (name == null) {
            return "self";
        }
        String n = name.toLowerCase(Locale.ROOT);
        return TARGETER_ALIASES.getOrDefault(n, n);
    }
}

package com.celestirift.skill.config;

import com.celestirift.api.skill.ConfigMap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Parses the structured (map) form of a skill line into a {@link SkillLine}:
 * <pre>
 * - damage: { amount: 6, ignorearmor: true }   # mechanic + attributes
 *   target: { nearby: { radius: 4 } }          # or: target: self  /  target: "@target{range=10}"
 *   if:                                        # conditions (all must pass)
 *     - health: "&gt;10"                       # scalar = the condition's primary attribute
 *     - "!permission{perm=vip.bypass}"         # string form also works; ! negates
 *   chance: 0.5
 * - delay: 20                                  # scalar = the mechanic's primary attribute
 * - message: "&lt;gold&gt;Done!"
 * </pre>
 * Exactly one non-reserved key names the mechanic. Reserved keys: {@code target}
 * ({@code targeter}, {@code @}), {@code if} ({@code conditions}), {@code chance},
 * {@code trigger} ({@code ~}).
 */
public final class StructuredLine {

    /** Resolves the primary (shorthand) attribute of a registered component, or null. */
    public interface Primaries {
        @Nullable String mechanic(String name);

        @Nullable String targeter(String name);

        @Nullable String condition(String name);
    }

    private static final Set<String> TARGET_KEYS = Set.of("target", "targeter", "@");
    private static final Set<String> IF_KEYS = Set.of("if", "conditions", "condition");
    private static final Set<String> TRIGGER_KEYS = Set.of("trigger", "~");
    private static final String CHANCE = "chance";

    /** Attribute key used when a shorthand value was given to a component that has no shorthand. */
    public static final String SHORTHAND_KEY = "(shorthand)";

    private StructuredLine() {}

    /** True for keys a structured line reserves (not mechanic names). */
    public static boolean isReserved(String key) {
        String k = key.toLowerCase(Locale.ROOT);
        return TARGET_KEYS.contains(k) || IF_KEYS.contains(k) || TRIGGER_KEYS.contains(k) || k.equals(CHANCE);
    }

    public static SkillLine parse(Map<?, ?> raw, Primaries primaries) {
        String mechanic = null;
        Object mechanicValue = null;
        Object target = null;
        Object conditions = null;
        Object chance = null;
        String trigger = null;
        List<String> mechanicKeys = new ArrayList<>();

        for (Map.Entry<?, ?> e : raw.entrySet()) {
            String key = String.valueOf(e.getKey()).trim();
            String k = key.toLowerCase(Locale.ROOT);
            if (TARGET_KEYS.contains(k)) {
                target = e.getValue();
            } else if (IF_KEYS.contains(k)) {
                conditions = e.getValue();
            } else if (TRIGGER_KEYS.contains(k)) {
                trigger = e.getValue() == null ? null : String.valueOf(e.getValue());
            } else if (k.equals(CHANCE)) {
                chance = e.getValue();
            } else {
                mechanicKeys.add(key);
                mechanic = key;
                mechanicValue = e.getValue();
            }
        }

        if (mechanicKeys.size() != 1) {
            throw new IllegalArgumentException(mechanicKeys.isEmpty()
                    ? "line has no mechanic (expected e.g. '- damage: { amount: 5 }')"
                    : "line has more than one mechanic key " + mechanicKeys
                    + " - put each mechanic in its own '- ' list item");
        }

        Map<String, Object> mechAttrs = attributes(mechanic, mechanicValue, primaries.mechanic(mechanic), "mechanic");
        if (chance != null) {
            mechAttrs.put(CHANCE, chance);
        }

        String targeterName = null;
        ConfigMap targeterConfig = null;
        if (target != null) {
            Named t = named(target, "target");
            targeterName = t.name();
            targeterConfig = t.raw() != null ? ConfigMap.parse(t.raw())
                    : ConfigMap.of(attributes(t.name(), t.value(), primaries.targeter(t.name()), "targeter"));
        }

        List<SkillLine.ConditionRef> refs = parseConditions(conditions, primaries);
        return SkillLine.of(mechanic, ConfigMap.of(mechAttrs), targeterName, targeterConfig, refs, trigger);
    }

    /** Parse an {@code if:} value (single entry or list) into condition refs. */
    public static List<SkillLine.ConditionRef> parseConditions(@Nullable Object value, Primaries primaries) {
        List<SkillLine.ConditionRef> out = new ArrayList<>();
        if (value == null) {
            return out;
        }
        List<?> entries = value instanceof List<?> l ? l : List.of(value);
        for (Object entry : entries) {
            if (entry instanceof String s) {
                out.add(SkillLine.parseCondition(s));
                continue;
            }
            if (!(entry instanceof Map<?, ?> m) || m.size() != 1) {
                throw new IllegalArgumentException("condition must be a string like 'health{value=>10}' "
                        + "or a single-key map like '{ health: \">10\" }', got: " + entry);
            }
            var e = m.entrySet().iterator().next();
            String name = String.valueOf(e.getKey()).trim();
            Object v = e.getValue();
            boolean negated = false;
            if (name.equalsIgnoreCase("not") && v instanceof Map<?, ?> inner && inner.size() == 1) {
                var ie = inner.entrySet().iterator().next();
                name = String.valueOf(ie.getKey()).trim();
                v = ie.getValue();
                negated = true;
            } else if (name.startsWith("!")) {
                name = name.substring(1).trim();
                negated = true;
            }
            out.add(new SkillLine.ConditionRef(name,
                    ConfigMap.of(attributes(name, v, primaries.condition(name), "condition")), negated));
        }
        return out;
    }

    // ------------------------------------------------------------------ helpers

    /** A targeter written as a string ({@code self}, {@code @target{range=5}}) or a one-key map. */
    private record Named(String name, @Nullable Object value, @Nullable String raw) {}

    private static Named named(Object v, String what) {
        if (v instanceof String s) {
            String body = s.trim();
            if (body.startsWith("@")) {
                body = body.substring(1);
            }
            String[] nc = SkillLine.splitToken(body);
            return new Named(nc[0].trim(), null, nc[1]);
        }
        if (v instanceof Map<?, ?> m && m.size() == 1) {
            var e = m.entrySet().iterator().next();
            String name = String.valueOf(e.getKey()).trim();
            if (name.startsWith("@")) {
                name = name.substring(1);
            }
            return new Named(name, e.getValue(), null);
        }
        throw new IllegalArgumentException(what + " must be a name like 'self' or a one-key map like "
                + "'{ nearby: { radius: 4 } }', got: " + v);
    }

    /** Turn a component's value into its attribute map: map → as-is, scalar → primary attribute. */
    private static Map<String, Object> attributes(String name, @Nullable Object value,
                                                  @Nullable String primary, String what) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (value == null) {
            return out;
        }
        if (value instanceof Map<?, ?> m) {
            for (var e : m.entrySet()) {
                Object v = e.getValue();
                if (v instanceof Map<?, ?> || v instanceof List<?>) {
                    throw new IllegalArgumentException(what + " '" + name + "' attribute '" + e.getKey()
                            + "' must be a single value, not a list or map");
                }
                out.put(String.valueOf(e.getKey()), v);
            }
            return out;
        }
        if (value instanceof List<?>) {
            throw new IllegalArgumentException(what + " '" + name + "' value must be a map of attributes "
                    + "or a single value, not a list");
        }
        // Unknown components (typos) have no primary either; keep the value under a marker
        // key so the loader can report "unknown X, did you mean Y" or "no shorthand" itself.
        out.put(primary != null ? primary : SHORTHAND_KEY, value);
        return out;
    }
}

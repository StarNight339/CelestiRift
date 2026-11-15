package com.celestirift.api.skill;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The attributes of one mechanic/targeter/condition, e.g. {@code amount=5;element=fire}
 * from {@code damage{amount=5;element=fire}} or the map form
 * {@code damage: { amount: 5 }}. Lookups are case-insensitive and accept aliases
 * ({@code amount}, {@code a}, {@code damage}), MythicMobs-style.
 *
 * <p>Every getter also <b>records</b> the attribute it was asked for (name, aliases,
 * type, default). CelestiRift uses that record to validate skills (unknown or
 * mistyped attributes, "did you mean") and to generate the JSON schema and docs —
 * so a mechanic only has to read its attributes in its constructor to be documented.
 * The first attribute read is the mechanic's <i>primary</i> attribute, used by the
 * shorthand {@code - delay: 20}.
 */
public final class ConfigMap {

    /** Value type of an attribute. */
    public enum Type {
        STRING, INT, DOUBLE, BOOLEAN,
        /** A number or an expression evaluated per cast ({@link #getNumber}). */
        NUMBER
    }

    /**
     * An attribute a mechanic read.
     *
     * @param name         primary name
     * @param aliases      other accepted names
     * @param type         value type
     * @param defaultValue value used when absent (may be null)
     */
    public record Attribute(String name, List<String> aliases, Type type, @Nullable Object defaultValue) {
        /** @return the primary name followed by all aliases */
        public List<String> allNames() {
            List<String> out = new ArrayList<>(aliases.size() + 1);
            out.add(name);
            out.addAll(aliases);
            return out;
        }
    }

    /**
     * A provided value that couldn't be parsed as the expected type (the default was used).
     *
     * @param key      the key as written
     * @param value    the raw value
     * @param expected the type the mechanic wanted
     * @param detail   why it failed (e.g. an expression error), or null
     */
    public record Problem(String key, String value, Type expected, @Nullable String detail) {
        /**
         * @param key      the key as written
         * @param value    the raw value
         * @param expected the type the mechanic wanted
         */
        public Problem(String key, String value, Type expected) {
            this(key, value, expected, null);
        }
    }

    private final Map<String, String> values = new LinkedHashMap<>();
    private final Map<String, Attribute> accessed = new LinkedHashMap<>();
    private final List<Problem> problems = new ArrayList<>();

    private ConfigMap() {}

    /**
     * Parse the raw content found between {@code { }}. May be empty.
     *
     * @param raw e.g. {@code amount=5;msg="hi; there"}
     * @return the parsed map
     */
    public static ConfigMap parse(@Nullable String raw) {
        ConfigMap map = new ConfigMap();
        if (raw == null || raw.isBlank()) {
            return map;
        }
        for (String pair : splitRespectingQuotes(raw)) {
            if (pair.isBlank()) {
                continue;
            }
            int eq = pair.indexOf('=');
            if (eq < 0) {
                // Bare flag, e.g. "silent" -> silent=true
                map.values.put(pair.trim().toLowerCase(Locale.ROOT), "true");
                continue;
            }
            String key = pair.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            String value = unquote(pair.substring(eq + 1).trim());
            map.values.put(key, value);
        }
        return map;
    }

    /**
     * Build from already-structured values (YAML map form). Values are converted with
     * {@link String#valueOf}; null values become empty strings.
     *
     * @param raw attribute name to value
     * @return the map
     */
    public static ConfigMap of(Map<String, ?> raw) {
        ConfigMap map = new ConfigMap();
        for (Map.Entry<String, ?> e : raw.entrySet()) {
            Object v = e.getValue();
            map.values.put(e.getKey().trim().toLowerCase(Locale.ROOT), v == null ? "" : String.valueOf(v));
        }
        return map;
    }

    private static List<String> splitRespectingQuotes(String raw) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                sb.append(c);
            } else if (c == ';' && !inQuotes) {
                out.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        out.add(sb.toString());
        return out;
    }

    private static String unquote(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    // ------------------------------------------------------------------ getters

    private void record(Type type, @Nullable Object def, String... keys) {
        if (keys.length == 0) {
            return;
        }
        String name = keys[0].toLowerCase(Locale.ROOT);
        if (!accessed.containsKey(name)) {
            List<String> aliases = new ArrayList<>(keys.length - 1);
            for (int i = 1; i < keys.length; i++) {
                aliases.add(keys[i].toLowerCase(Locale.ROOT));
            }
            accessed.put(name, new Attribute(name, List.copyOf(aliases), type, def));
        }
    }

    private @Nullable String raw(String... keys) {
        for (String key : keys) {
            String v = values.get(key.toLowerCase(Locale.ROOT));
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private @Nullable String rawKey(String... keys) {
        for (String key : keys) {
            if (values.containsKey(key.toLowerCase(Locale.ROOT))) {
                return key.toLowerCase(Locale.ROOT);
            }
        }
        return null;
    }

    /**
     * @param def  value when absent
     * @param keys primary name then aliases
     * @return the value or {@code def}
     */
    public String getString(String def, String... keys) {
        record(Type.STRING, def, keys);
        String v = raw(keys);
        return v != null ? v : def;
    }

    /**
     * One of a fixed set of words (case-insensitive), e.g. {@code filter: enemies}.
     * Anything else is reported by the validator and {@code def} is used.
     *
     * @param def     value when absent or not one of {@code choices}
     * @param choices allowed values (lowercase)
     * @param keys    primary name then aliases
     * @return the chosen value in lowercase
     */
    public String getChoice(String def, List<String> choices, String... keys) {
        record(Type.STRING, def, keys);
        String v = raw(keys);
        if (v == null) return def;
        String s = v.trim().toLowerCase(Locale.ROOT);
        if (choices.contains(s)) {
            return s;
        }
        problems.add(new Problem(rawKey(keys), v, Type.STRING, "expected one of " + String.join(", ", choices)));
        return def;
    }

    /**
     * @param def  value when absent or not an integer
     * @param keys primary name then aliases
     * @return the value or {@code def}
     */
    public int getInt(int def, String... keys) {
        record(Type.INT, def, keys);
        String v = raw(keys);
        if (v == null) return def;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            // Accept "20.0" from YAML numbers written as decimals.
            try {
                double d = Double.parseDouble(v.trim());
                if (d == Math.rint(d)) {
                    return (int) d;
                }
            } catch (NumberFormatException ignored) {
                // fall through
            }
            problems.add(new Problem(rawKey(keys), v, Type.INT));
            return def;
        }
    }

    /**
     * @param def  value when absent or not a number
     * @param keys primary name then aliases
     * @return the value or {@code def}
     */
    public double getDouble(double def, String... keys) {
        record(Type.DOUBLE, def, keys);
        String v = raw(keys);
        if (v == null) return def;
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            // Constant arithmetic like "2 * 3" is fine here; per-cast variables are not.
            try {
                Expression ex = Expression.compile(v);
                if (ex.isConstant()) {
                    return ex.eval(name -> 0);
                }
                problems.add(new Problem(rawKey(keys), v, Type.DOUBLE,
                        "this attribute is fixed when the skill loads, so it can't use <variables> or random()"));
            } catch (Expression.ParseException pe) {
                problems.add(new Problem(rawKey(keys), v, Type.DOUBLE));
            }
            return def;
        }
    }

    /**
     * A number that may be an expression evaluated on every cast, e.g.
     * {@code "6 + <caster.level> * 0.5"}. Use this for values that should scale
     * (damage, heal, duration...).
     *
     * @param def  value when absent or invalid
     * @param keys primary name then aliases
     * @return the value, constant or per cast
     */
    public NumberValue getNumber(double def, String... keys) {
        record(Type.NUMBER, def, keys);
        String v = raw(keys);
        if (v == null) return NumberValue.constant(def);
        try {
            return NumberValue.constant(Double.parseDouble(v.trim()));
        } catch (NumberFormatException ignored) {
            // an expression
        }
        try {
            Expression ex = Expression.compile(v);
            for (String var : ex.variables()) {
                if (!Variables.isKnown(var)) {
                    problems.add(new Problem(rawKey(keys), v, Type.NUMBER, "unknown variable <" + var + ">"));
                }
            }
            return NumberValue.of(ex);
        } catch (Expression.ParseException pe) {
            problems.add(new Problem(rawKey(keys), v, Type.NUMBER, pe.getMessage()));
            return NumberValue.constant(def);
        }
    }

    /**
     * @param def  value when absent or not a number
     * @param keys primary name then aliases
     * @return the value or {@code def}
     */
    public float getFloat(float def, String... keys) {
        return (float) getDouble(def, keys);
    }

    /**
     * Accepts true/false, yes/no, on/off, 1/0.
     *
     * @param def  value when absent or unrecognised
     * @param keys primary name then aliases
     * @return the value or {@code def}
     */
    public boolean getBoolean(boolean def, String... keys) {
        record(Type.BOOLEAN, def, keys);
        String v = raw(keys);
        if (v == null) return def;
        String s = v.trim().toLowerCase(Locale.ROOT);
        return switch (s) {
            case "true", "yes", "on", "1" -> true;
            case "false", "no", "off", "0" -> false;
            default -> {
                problems.add(new Problem(rawKey(keys), v, Type.BOOLEAN));
                yield def;
            }
        };
    }

    /**
     * @param key attribute name
     * @return true if the attribute was provided
     */
    public boolean has(String key) {
        return values.containsKey(key.toLowerCase(Locale.ROOT));
    }

    // ------------------------------------------------------------------ introspection

    /** @return provided keys (lowercase) and raw values, in order */
    public Map<String, String> asMap() {
        return Collections.unmodifiableMap(values);
    }

    /** @return attributes the owner has read so far, in read order */
    public List<Attribute> accessedAttributes() {
        return List.copyOf(accessed.values());
    }

    /** @return provided values that failed to parse */
    public List<Problem> problems() {
        return List.copyOf(problems);
    }

    /**
     * @param ignore extra names to treat as known (e.g. {@code chance})
     * @return provided keys that no getter asked for — usually typos
     */
    public Set<String> unusedKeys(String... ignore) {
        Set<String> known = new HashSet<>(Arrays.asList(ignore));
        for (Attribute a : accessed.values()) {
            known.addAll(a.allNames());
        }
        Set<String> out = new java.util.LinkedHashSet<>();
        for (String k : values.keySet()) {
            if (!known.contains(k)) {
                out.add(k);
            }
        }
        return out;
    }

    @Override
    public String toString() {
        return values.toString();
    }
}

package com.celestirift.skill.config;

import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.SkillTargets;
import com.celestirift.api.skill.targeter.Targeter;

import java.util.ArrayList;
import java.util.List;

/**
 * A single parsed skill line, e.g.:
 * <pre>
 *   damage{amount=6} @target ?health{value=&gt;10} ~onAttack
 * </pre>
 * Grammar (whitespace separated, braces may contain spaces):
 * <ul>
 *   <li>{@code mechanic{attrs}}        (required, first token)</li>
 *   <li>{@code @targeter{attrs}}       (optional)</li>
 *   <li>{@code ?condition{attrs}}      (optional, repeatable; {@code ?!} negates)</li>
 *   <li>{@code ~trigger}               (optional; used for event bindings)</li>
 * </ul>
 */
public final class SkillLine {

    public record ConditionRef(String name, ConfigMap config, boolean negated) {}

    private final String mechanicName;
    private final ConfigMap mechanicConfig;
    private final String targeterName;      // null = default targeter (@self)
    private final ConfigMap targeterConfig;
    private final List<ConditionRef> conditions;
    private final String trigger;           // null if none

    /** Build a line directly (used by the structured YAML form). */
    public static SkillLine of(String mechanicName, ConfigMap mechanicConfig,
                               String targeterName, ConfigMap targeterConfig,
                               List<ConditionRef> conditions, String trigger) {
        return new SkillLine(mechanicName, mechanicConfig, targeterName,
                targeterConfig == null ? ConfigMap.parse("") : targeterConfig, conditions, trigger);
    }

    /**
     * Parse a condition entry such as {@code health{value=<10}}, {@code ?health{...}},
     * or a negated {@code !health{...}} / {@code ?!health{...}}.
     */
    public static ConditionRef parseCondition(String entry) {
        String body = entry.trim();
        boolean negated = false;
        if (body.startsWith("?!") || body.startsWith("!?")) {
            negated = true;
            body = body.substring(2);
        } else if (body.startsWith("!")) {
            negated = true;
            body = body.substring(1);
        } else if (body.startsWith("?")) {
            body = body.substring(1);
        }
        String[] nc = splitNameAndBraces(body.trim());
        return new ConditionRef(nc[0].trim(), ConfigMap.parse(nc[1]), negated);
    }

    /** Split {@code name{a=b}} into name and attributes (braces optional). */
    public static String[] splitToken(String token) {
        return splitNameAndBraces(token.trim());
    }

    private SkillLine(String mechanicName, ConfigMap mechanicConfig,
                      String targeterName, ConfigMap targeterConfig,
                      List<ConditionRef> conditions, String trigger) {
        this.mechanicName = mechanicName;
        this.mechanicConfig = mechanicConfig;
        this.targeterName = targeterName;
        this.targeterConfig = targeterConfig;
        this.conditions = conditions;
        this.trigger = trigger;
    }

    public static SkillLine parse(String raw) {
        List<String> tokens = tokenize(raw.trim());
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Empty skill line");
        }

        String mechanicName = null;
        ConfigMap mechanicConfig = ConfigMap.parse("");
        String targeterName = null;
        ConfigMap targeterConfig = ConfigMap.parse("");
        List<ConditionRef> conditions = new ArrayList<>();
        String trigger = null;

        for (String token : tokens) {
            if (token.startsWith("@")) {
                String[] nc = splitNameAndBraces(token.substring(1));
                targeterName = nc[0];
                targeterConfig = ConfigMap.parse(nc[1]);
            } else if (token.startsWith("?")) {
                boolean negated = token.startsWith("?!");
                String body = token.substring(negated ? 2 : 1);
                String[] nc = splitNameAndBraces(body);
                conditions.add(new ConditionRef(nc[0], ConfigMap.parse(nc[1]), negated));
            } else if (token.startsWith("~")) {
                trigger = token.substring(1);
            } else if (mechanicName == null) {
                String[] nc = splitNameAndBraces(token);
                mechanicName = nc[0];
                mechanicConfig = ConfigMap.parse(nc[1]);
            }
        }

        if (mechanicName == null) {
            throw new IllegalArgumentException("Skill line has no mechanic: " + raw);
        }
        return new SkillLine(mechanicName, mechanicConfig, targeterName,
                targeterConfig, conditions, trigger);
    }

    /** Splits "name{a=b}" into ["name", "a=b"]. Braces optional. */
    private static String[] splitNameAndBraces(String token) {
        int brace = token.indexOf('{');
        if (brace < 0) {
            return new String[]{token, ""};
        }
        String name = token.substring(0, brace);
        int end = token.lastIndexOf('}');
        String inner = (end > brace) ? token.substring(brace + 1, end) : token.substring(brace + 1);
        return new String[]{name, inner};
    }

    /** Split by spaces, but never inside {@code { }}. */
    private static List<String> tokenize(String raw) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int depth = 0;
        boolean inQuotes = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                sb.append(c);
            } else if (c == '{' && !inQuotes) {
                depth++;
                sb.append(c);
            } else if (c == '}' && !inQuotes) {
                depth--;
                sb.append(c);
            } else if (Character.isWhitespace(c) && depth == 0 && !inQuotes) {
                if (sb.length() > 0) {
                    out.add(sb.toString());
                    sb.setLength(0);
                }
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) {
            out.add(sb.toString());
        }
        return out;
    }

    public String mechanicName() { return mechanicName; }
    public ConfigMap mechanicConfig() { return mechanicConfig; }
    public String targeterName() { return targeterName; }
    public ConfigMap targeterConfig() { return targeterConfig; }
    public List<ConditionRef> conditions() { return conditions; }
    public String trigger() { return trigger; }
}

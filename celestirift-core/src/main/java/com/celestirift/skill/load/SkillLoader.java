package com.celestirift.skill.load;

import com.celestirift.CelestiRift;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.condition.Condition;
import com.celestirift.api.skill.mechanic.Mechanic;
import com.celestirift.api.skill.targeter.Targeter;
import com.celestirift.skill.Skill;
import com.celestirift.skill.SkillManager;
import com.celestirift.skill.config.SkillLine;
import com.celestirift.skill.config.StructuredLine;
import com.celestirift.skill.mechanic.impl.DelayMechanic;
import com.celestirift.skill.registry.ComponentRegistry;
import com.celestirift.validate.Diagnostics;
import com.celestirift.validate.LineLocator;
import com.celestirift.validate.Suggest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Loads every {@code skills/**.yml} file (sub-folders included), resolves
 * {@code extends:} and {@code variables:}, compiles both line syntaxes and reports
 * every problem with file and line to {@link Diagnostics}.
 *
 * <pre>
 * BaseBolt:
 *   cooldown: 2
 *   variables: { dmg: 4 }
 *   skills:
 *   - damage: { amount: "${dmg}" }
 *     target: { target: { range: 20 } }
 * FireBolt:
 *   extends: BaseBolt          # inherits cooldown/mana/skills/... unless overridden
 *   variables: { dmg: 8 }      # child variables override the parent's
 * </pre>
 */
public final class SkillLoader {

    /** Recognised skill keys (case-insensitive). */
    private static final List<String> KEYS = List.of(
            "cooldown", "mana", "skills", "conditions", "item", "description", "extends", "variables",
            "cast-time", "cast-interrupt");

    private static final Pattern VAR = Pattern.compile("\\$\\{([A-Za-z0-9_.-]+)}");

    /** Raw definition of one skill before compilation. */
    private record Raw(String name, String file, LineLocator lines, ConfigurationSection section) {
        @Nullable Object own(String key) {
            for (String k : section.getKeys(false)) {
                if (k.equalsIgnoreCase(key)) {
                    return section.get(k);
                }
            }
            return null;
        }
    }

    /** A skill reference to verify once everything is loaded. */
    private record SkillRef(String from, String file, int line, String target) {}

    private final CelestiRift plugin;
    private final SkillManager manager;
    private final Diagnostics diag;
    private final Map<String, Raw> raws = new LinkedHashMap<>();
    private final List<SkillRef> refs = new ArrayList<>();

    public SkillLoader(CelestiRift plugin, SkillManager manager, Diagnostics diag) {
        this.plugin = plugin;
        this.manager = manager;
        this.diag = diag;
    }

    public Map<String, Skill> load() {
        Path root = plugin.getDataFolder().toPath();
        Path dir = root.resolve("skills");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not create skills directory: " + e.getMessage());
        }

        for (Path file : yamlFiles(dir)) {
            readFile(root, file);
        }

        Map<String, Skill> out = new HashMap<>();
        for (Raw raw : raws.values()) {
            try {
                Skill skill = compile(raw);
                out.put(raw.name().toLowerCase(Locale.ROOT), skill);
            } catch (RuntimeException e) {
                diag.error(raw.file(), raw.lines().key(raw.name()), raw.name(),
                        "failed to load: " + e.getMessage());
            }
        }

        for (SkillRef ref : refs) {
            if (!out.containsKey(ref.target().toLowerCase(Locale.ROOT))) {
                diag.warn(ref.file(), ref.line(), ref.from(), "references unknown skill '" + ref.target() + "'",
                        Suggest.closest(ref.target(), raws.values().stream().map(Raw::name).toList()));
            }
        }
        plugin.getLogger().info("Loaded " + out.size() + " skill(s).");
        return out;
    }

    /** All .yml/.yaml files under {@code dir}, recursively, in a stable order. */
    public static List<Path> yamlFiles(Path dir) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        return n.endsWith(".yml") || n.endsWith(".yaml");
                    })
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    /** Load a YAML file, reporting syntax errors. Returns null on failure. */
    public static @Nullable YamlConfiguration readYaml(Path file, String rel, Diagnostics diag) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file.toFile());
            return yaml;
        } catch (InvalidConfigurationException e) {
            diag.error(rel, lineOf(e.getMessage()), Diagnostics.FILE, "YAML syntax error: " + firstLine(e.getMessage()));
        } catch (IOException e) {
            diag.error(rel, 0, Diagnostics.FILE, "could not read file: " + e.getMessage());
        }
        return null;
    }

    private void readFile(Path root, Path file) {
        String rel = root.relativize(file).toString().replace('\\', '/');
        YamlConfiguration yaml = readYaml(file, rel, diag);
        if (yaml == null) {
            return;
        }
        LineLocator lines = new LineLocator(file);
        for (String name : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(name);
            if (section == null) {
                diag.warn(rel, lines.key(name), name, "is not a skill (expected a block with 'skills:')");
                continue;
            }
            Raw previous = raws.put(name.toLowerCase(Locale.ROOT), new Raw(name, rel, lines, section));
            if (previous != null) {
                diag.warn(rel, lines.key(name), name, "is also defined in " + previous.file()
                        + " - this definition replaces it");
            }
        }
    }

    // ------------------------------------------------------------------ inheritance

    /** A property, inherited through {@code extends:} when the skill doesn't set it. */
    private @Nullable Object prop(Raw raw, String key) {
        return prop(raw, key, new HashSet<>());
    }

    private @Nullable Object prop(Raw raw, String key, Set<String> seen) {
        Object own = raw.own(key);
        if (own != null) {
            return own;
        }
        Raw parent = parent(raw, seen);
        return parent == null ? null : prop(parent, key, seen);
    }

    private @Nullable Raw parent(Raw raw, Set<String> seen) {
        Object ext = raw.own("extends");
        if (ext == null) {
            return null;
        }
        if (!seen.add(raw.name().toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("'extends' loop involving " + raw.name());
        }
        Raw parent = raws.get(String.valueOf(ext).toLowerCase(Locale.ROOT));
        if (parent == null) {
            throw new IllegalStateException("extends unknown skill '" + ext + "'"
                    + hint(Suggest.closest(String.valueOf(ext), raws.values().stream().map(Raw::name).toList())));
        }
        return parent;
    }

    /** Variables from the whole extends chain; children override parents. */
    private Map<String, String> variables(Raw raw) {
        List<Raw> chain = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Raw r = raw; r != null; r = parent(r, seen)) {
            chain.addFirst(r);
        }
        Map<String, String> vars = new HashMap<>();
        for (Raw r : chain) {
            Object v = r.own("variables");
            if (v instanceof ConfigurationSection s) {
                for (String k : s.getKeys(false)) {
                    vars.put(k, String.valueOf(s.get(k)));
                }
            } else if (v != null) {
                diag.warn(r.file(), r.lines().find(r.name(), "variables"), r.name(),
                        "'variables' must be a map like '{ dmg: 6 }'");
            }
        }
        return vars;
    }

    // ------------------------------------------------------------------ compile

    private Skill compile(Raw raw) {
        String name = raw.name();
        for (String key : raw.section().getKeys(false)) {
            if (!KEYS.contains(key.toLowerCase(Locale.ROOT))) {
                diag.warn(raw.file(), raw.lines().find(name, key + ":"), name,
                        "unknown key '" + key + "' (ignored)", Suggest.closest(key, KEYS));
            }
        }

        Map<String, String> vars = variables(raw);
        Context ctx = new Context(raw, vars);

        double cooldown = number(ctx, "cooldown", prop(raw, "cooldown"));
        double mana = number(ctx, "mana", prop(raw, "mana"));

        List<Skill.Line> lines = new ArrayList<>();
        Object rawLines = prop(raw, "skills");
        if (rawLines == null) {
            diag.warn(raw.file(), raw.lines().key(name), name, "has no 'skills:' list - it does nothing");
        } else if (!(rawLines instanceof List<?> list)) {
            diag.error(raw.file(), raw.lines().find(name, "skills"), name, "'skills' must be a list of lines");
        } else {
            for (Object entry : list) {
                Skill.Line line = compileEntry(ctx, entry);
                if (line != null) {
                    lines.add(line);
                }
            }
        }

        List<Skill.CompiledCondition> conditions = new ArrayList<>();
        Object rawConds = prop(raw, "conditions");
        if (rawConds != null) {
            List<?> entries = rawConds instanceof List<?> l ? l : List.of(rawConds);
            for (Object entry : entries) {
                try {
                    Object e = ctx.subst(entry);
                    List<SkillLine.ConditionRef> parsed = e instanceof String s
                            ? List.of(SkillLine.parseCondition(s))
                            : StructuredLine.parseConditions(e, manager.primaries());
                    for (SkillLine.ConditionRef ref : parsed) {
                        Skill.CompiledCondition cc = compileCondition(ctx, ref);
                        if (cc != null) {
                            conditions.add(cc);
                        }
                    }
                } catch (RuntimeException ex) {
                    ctx.error("conditions", "bad condition: " + ex.getMessage());
                }
            }
        }

        Object description = prop(raw, "description");
        int castTicks = (int) Math.max(0, number(ctx, "cast-time", prop(raw, "cast-time")));
        boolean interruptMove = true;
        boolean interruptDamage = true;
        Object interrupt = prop(raw, "cast-interrupt");
        if (interrupt != null) {
            List<?> list = interrupt instanceof List<?> l ? l : List.of(interrupt);
            interruptMove = false;
            interruptDamage = false;
            for (Object o : list) {
                switch (String.valueOf(o).trim().toLowerCase(Locale.ROOT)) {
                    case "move" -> interruptMove = true;
                    case "damage" -> interruptDamage = true;
                    case "none", "" -> { }
                    default -> ctx.warn("cast-interrupt", "unknown cast-interrupt '" + o + "' - use move, damage or none");
                }
            }
        }
        return new Skill(name, cooldown, mana, lines, itemStyle(prop(raw, "item")), conditions,
                description == null ? "" : String.valueOf(ctx.subst(description)))
                .channel(new Skill.Channel(castTicks, interruptMove, interruptDamage));
    }

    private double number(Context ctx, String key, @Nullable Object value) {
        if (value == null) {
            return 0;
        }
        Object v = ctx.subst(value);
        try {
            return Double.parseDouble(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            ctx.warn(key, "'" + key + "' must be a number, got '" + v + "' (using 0)");
            return 0;
        }
    }

    private Skill.ItemStyle itemStyle(@Nullable Object item) {
        if (!(item instanceof ConfigurationSection s)) {
            return Skill.ItemStyle.EMPTY;
        }
        return new Skill.ItemStyle(
                s.getString("name-gradient.from"), s.getString("name-gradient.to"),
                s.getString("mana-gradient.from"), s.getString("mana-gradient.to"),
                s.getString("model"));
    }

    private @Nullable Skill.Line compileEntry(Context ctx, Object entry) {
        String needle = entry instanceof Map<?, ?> m && !m.isEmpty()
                ? String.valueOf(m.keySet().iterator().next()) : String.valueOf(entry).split("[{\\s]")[0];
        SkillLine parsed;
        try {
            Object e = ctx.subst(entry);
            if (e instanceof String s) {
                if (s.isBlank()) {
                    return null;
                }
                parsed = SkillLine.parse(s.trim());
            } else if (e instanceof Map<?, ?> map) {
                parsed = StructuredLine.parse(map, manager.primaries());
            } else {
                ctx.error(needle, "a line must be a string like 'damage{amount=5} @target' "
                        + "or a map like '- damage: { amount: 5 }'");
                return null;
            }
        } catch (RuntimeException ex) {
            ctx.error(needle, "bad line: " + ex.getMessage());
            return null;
        }
        return compileLine(ctx, parsed);
    }

    private @Nullable Skill.Line compileLine(Context ctx, SkillLine parsed) {
        String mech = parsed.mechanicName();
        ComponentRegistry<Mechanic> mechanics = manager.mechanics();
        if (!mechanics.has(mech)) {
            ctx.error(mech, "unknown mechanic '" + mech + "' - line skipped"
                    + hint(Suggest.closest(mech, mechanics.names())));
            return null;
        }
        Mechanic mechanic;
        try {
            mechanic = mechanics.create(mech, parsed.mechanicConfig());
        } catch (RuntimeException ex) {
            ctx.error(mech, "mechanic '" + mech + "' failed to load: " + ex.getMessage());
            return null;
        }
        double chance = parsed.mechanicConfig().getDouble(1.0, "chance", "c");
        checkAttributes(ctx, "mechanic", mech, parsed.mechanicConfig());
        collectSkillRefs(ctx, mech, parsed.mechanicConfig());

        Targeter targeter = null;
        if (parsed.targeterName() != null) {
            String tn = parsed.targeterName();
            if (!manager.targeters().has(tn)) {
                ctx.warn(tn, "unknown targeter '@" + tn + "' - using @self"
                        + hint(Suggest.closest(tn, manager.targeters().names())));
            } else {
                try {
                    targeter = manager.targeters().create(tn, parsed.targeterConfig());
                    checkAttributes(ctx, "targeter", "@" + tn, parsed.targeterConfig());
                } catch (RuntimeException ex) {
                    ctx.warn(tn, "targeter '@" + tn + "' failed to load (" + ex.getMessage() + ") - using @self");
                }
            }
        }

        List<Skill.CompiledCondition> conditions = new ArrayList<>();
        for (SkillLine.ConditionRef ref : parsed.conditions()) {
            Skill.CompiledCondition cc = compileCondition(ctx, ref);
            if (cc != null) {
                conditions.add(cc);
            }
        }

        int delay = mechanic instanceof DelayMechanic dm ? dm.ticks() : 0;
        return new Skill.Line(mechanic, targeter, conditions, chance, delay);
    }

    private @Nullable Skill.CompiledCondition compileCondition(Context ctx, SkillLine.ConditionRef ref) {
        ComponentRegistry<Condition> conditions = manager.conditions();
        if (!conditions.has(ref.name())) {
            ctx.warn(ref.name(), "unknown condition '" + ref.name() + "' - ignored"
                    + hint(Suggest.closest(ref.name(), conditions.names())));
            return null;
        }
        try {
            Condition c = conditions.create(ref.name(), ref.config());
            checkAttributes(ctx, "condition", ref.name(), ref.config());
            return new Skill.CompiledCondition(c, ref.negated());
        } catch (RuntimeException ex) {
            ctx.warn(ref.name(), "condition '" + ref.name() + "' failed to load: " + ex.getMessage());
            return null;
        }
    }

    /** Unknown attributes (typos) and values of the wrong type. */
    private void checkAttributes(Context ctx, String kind, String owner, ConfigMap cfg) {
        List<String> known = new ArrayList<>();
        for (ConfigMap.Attribute a : cfg.accessedAttributes()) {
            known.addAll(a.allNames());
        }
        for (String key : cfg.unusedKeys("chance", "c")) {
            if (key.equals(StructuredLine.SHORTHAND_KEY)) {
                ctx.warn(owner, kind + " '" + owner + "' has no shorthand value (ignored) - write it as a map, "
                        + "e.g. '" + owner.replace("@", "") + ": { attribute: value }'");
                continue;
            }
            ctx.warn(key, kind + " '" + owner + "' has no attribute '" + key + "' (ignored)"
                    + hint(Suggest.closest(key, known)));
        }
        for (ConfigMap.Problem p : cfg.problems()) {
            if (p.value().contains("${")) {
                continue; // already reported as an unknown variable
            }
            if (p.detail() != null) {
                ctx.warn(p.key(), kind + " '" + owner + "': '" + p.key() + "' = '" + p.value() + "': " + p.detail()
                        + " (using default)");
            } else {
                ctx.warn(p.key(), kind + " '" + owner + "': '" + p.key() + "' expects "
                        + p.expected().name().toLowerCase(Locale.ROOT) + " but got '" + p.value() + "' (using default)");
            }
        }
    }

    /** Remember attributes that name other skills, to check after loading. */
    private void collectSkillRefs(Context ctx, String mechanic, ConfigMap cfg) {
        ComponentRegistry.Description d = manager.mechanics().describe(mechanic);
        if (d == null) {
            return;
        }
        Map<String, String> values = cfg.asMap();
        for (String attr : skillRefAttributes(d.name())) {
            String v = values.get(attr);
            if (v != null && !v.isBlank() && !v.contains("<")) {
                refs.add(new SkillRef(ctx.raw.name(), ctx.raw.file(), ctx.line(v), v));
            }
        }
    }

    private static List<String> skillRefAttributes(String mechanic) {
        return switch (mechanic) {
            case "skill" -> List.of("skill", "s", "name");
            case "projectiletrail" -> List.of("onhit", "hitskill");
            case "summon" -> List.of("onsummon", "summonskill");
            case "telegraph" -> List.of("skill", "s", "then");
            case "display" -> List.of("onend");
            case "meteor" -> List.of("onland", "skill", "s");
            case "chain" -> List.of("onhit");
            case "aura" -> List.of("ontick", "onhit", "ondamaged", "onend", "onexpire");
            default -> List.of();
        };
    }

    private static String hint(@Nullable String suggestion) {
        return suggestion == null ? "" : " - did you mean '" + suggestion + "'?";
    }

    private static int lineOf(@Nullable String message) {
        if (message == null) {
            return 0;
        }
        Matcher m = Pattern.compile("line (\\d+)").matcher(message);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private static String firstLine(@Nullable String message) {
        if (message == null) {
            return "";
        }
        String s = message.strip();
        int nl = s.indexOf('\n');
        return nl < 0 ? s : s.substring(0, nl);
    }

    // ------------------------------------------------------------------ per-skill context

    /** Variables + diagnostics helpers for the skill being compiled. */
    private final class Context {
        final Raw raw;
        final Map<String, String> vars;

        Context(Raw raw, Map<String, String> vars) {
            this.raw = raw;
            this.vars = vars;
        }

        int line(String needle) {
            return raw.lines().find(raw.name(), needle);
        }

        void warn(String needle, String message) {
            diag.warn(raw.file(), line(needle), raw.name(), message);
        }

        void error(String needle, String message) {
            diag.error(raw.file(), line(needle), raw.name(), message);
        }

        /** Replace {@code ${var}} in strings, recursively through lists and maps. */
        Object subst(Object value) {
            if (value instanceof String s) {
                if (!s.contains("${")) {
                    return s;
                }
                Matcher m = VAR.matcher(s);
                StringBuilder sb = new StringBuilder();
                while (m.find()) {
                    String v = vars.get(m.group(1));
                    if (v == null) {
                        warn(m.group(0), "unknown variable '" + m.group(1) + "'"
                                + hint(Suggest.closest(m.group(1), vars.keySet())));
                        v = m.group(0);
                    }
                    m.appendReplacement(sb, Matcher.quoteReplacement(v));
                }
                m.appendTail(sb);
                return sb.toString();
            }
            if (value instanceof ConfigurationSection s) {
                return subst(s.getValues(false));
            }
            if (value instanceof Map<?, ?> map) {
                Map<Object, Object> out = new LinkedHashMap<>();
                for (var e : map.entrySet()) {
                    out.put(e.getKey(), subst(e.getValue()));
                }
                return out;
            }
            if (value instanceof List<?> list) {
                List<Object> out = new ArrayList<>(list.size());
                for (Object o : list) {
                    out.add(subst(o));
                }
                return out;
            }
            return value;
        }
    }
}

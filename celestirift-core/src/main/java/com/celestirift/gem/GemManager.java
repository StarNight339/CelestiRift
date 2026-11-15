package com.celestirift.gem;

import com.celestirift.CelestiRift;
import com.celestirift.skill.load.ContentFiles;
import com.celestirift.trigger.TriggerType;
import com.celestirift.validate.Diagnostics;
import com.celestirift.validate.Suggest;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Loads named gem templates from {@code gems.yml} so gems can be authored in YAML
 * (just like skills) and handed out by id with {@code /cr gem <id>}.
 */
public final class GemManager implements com.celestirift.util.Restorable {

    private final CelestiRift plugin;
    private final Map<String, GemDef> gems = new LinkedHashMap<>();

    public GemManager(CelestiRift plugin) {
        this.plugin = plugin;
    }

    public Optional<GemDef> get(String id) {
        return Optional.ofNullable(gems.get(id.toLowerCase(Locale.ROOT)));
    }

    public Set<String> ids() {
        Set<String> out = new TreeSet<>();
        for (GemDef def : gems.values()) {
            out.add(def.id());
        }
        return out;
    }

    /** (Re)load gem definitions from gems.yml and gems/**.yml. Returns the number loaded. */
    @Override
    public Runnable snapshot() {
        var copy = new LinkedHashMap<>(gems);
        return () -> { gems.clear(); gems.putAll(copy); };
    }

    public int reload(Diagnostics diag) {
        gems.clear();
        for (ContentFiles.File file : ContentFiles.load(plugin, "gems.yml", "gems", diag)) {
            load(file, diag);
        }
        plugin.getLogger().info("Loaded " + gems.size() + " gem definition(s).");
        return gems.size();
    }

    private void load(ContentFiles.File file, Diagnostics diag) {
        var yaml = file.yaml();
        for (String id : yaml.getKeys(false)) {
            ConfigurationSection sec = yaml.getConfigurationSection(id);
            int line = file.lines().key(id);
            if (sec == null) {
                diag.warn(file.path(), line, id, "is not a gem (expected a block)");
                continue;
            }
            for (String key : sec.getKeys(false)) {
                if (!KEYS.contains(key.toLowerCase(Locale.ROOT))) {
                    diag.warn(file.path(), file.lines().find(id, key + ":"), id,
                            "unknown key '" + key + "' (ignored)", Suggest.closest(key, KEYS));
                }
            }
            SocketType type = SocketType.fromString(sec.getString("type"), SocketType.ACTIVE);
            String skill = sec.getString("skill", "");
            if (type == SocketType.ACTIVE && skill.isBlank()) {
                diag.error(file.path(), line, id, "active gem has no 'skill' - skipped");
                continue;
            }
            if (type == SocketType.ACTIVE && plugin.skills().getSkill(skill).isEmpty()) {
                diag.warn(file.path(), file.lines().find(id, "skill"), id, "uses unknown skill '" + skill + "'",
                        Suggest.closest(skill, plugin.skills().skillNames()));
            }
            if (gems.containsKey(id.toLowerCase(Locale.ROOT))) {
                diag.warn(file.path(), line, id, "is defined twice - this definition replaces the earlier one");
            }
            int tier = Math.max(1, Math.min(5, sec.getInt("tier", 1)));
            TriggerType trigger = TriggerType.fromString(sec.getString("trigger", "RIGHT_CLICK"));
            if (trigger == null) {
                diag.warn(file.path(), file.lines().find(id, "trigger"), id, "unknown trigger '"
                        + sec.getString("trigger") + "' - using RIGHT_CLICK", Suggest.closest(
                        String.valueOf(sec.getString("trigger")),
                        java.util.Arrays.stream(TriggerType.values()).map(Enum::name).toList()));
                trigger = TriggerType.RIGHT_CLICK;
            }
            Augment augment = Augment.NONE;
            if (type == SocketType.PASSIVE) {
                augment = new Augment(
                        sec.getDouble("power", 1.0),
                        sec.getDouble("range", 1.0),
                        sec.getDouble("cooldown", 1.0),
                        sec.getString("buff.type", ""),
                        sec.getInt("buff.duration", 0),
                        sec.getInt("buff.level", 1));
            }
            String material = sec.getString("material");
            String name = sec.getString("name");
            List<String> lore = sec.getStringList("lore");
            gems.put(id.toLowerCase(Locale.ROOT),
                    new GemDef(id, type, skill, tier, trigger, augment, material, name, lore,
                            sec.getString("model")));
        }
    }

    private static final List<String> KEYS = List.of("type", "skill", "tier", "trigger", "material", "name",
            "lore", "model", "power", "range", "cooldown", "buff");
}

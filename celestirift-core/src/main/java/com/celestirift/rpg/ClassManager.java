package com.celestirift.rpg;

import com.celestirift.CelestiRift;
import com.celestirift.skill.load.ContentFiles;
import com.celestirift.util.Registries;
import com.celestirift.validate.Diagnostics;
import com.celestirift.validate.Suggest;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/** Loads classes from classes.yml and classes/**.yml. */
public final class ClassManager implements com.celestirift.util.Restorable {

    private static final List<String> KEYS = List.of("display", "description", "icon", "mana", "mana-regen",
            "socket-bonus", "starting-gems", "permission", "passives");

    private final CelestiRift plugin;
    private final Map<String, RpgClass> classes = new LinkedHashMap<>();

    public ClassManager(CelestiRift plugin) {
        this.plugin = plugin;
    }

    public Optional<RpgClass> get(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(classes.get(id.toLowerCase(Locale.ROOT)));
    }

    public Set<String> ids() {
        Set<String> out = new TreeSet<>();
        classes.values().forEach(c -> out.add(c.id()));
        return out;
    }

    public java.util.Collection<RpgClass> all() {
        return classes.values();
    }

    @Override
    public Runnable snapshot() {
        var copy = new LinkedHashMap<>(classes);
        return () -> { classes.clear(); classes.putAll(copy); };
    }

    private static double num(Object o, double def) {
        if (o == null) return def;
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public int reload(Diagnostics diag) {
        classes.clear();
        for (ContentFiles.File file : ContentFiles.load(plugin, "classes.yml", "classes", diag)) {
            for (String id : file.yaml().getKeys(false)) {
                ConfigurationSection s = file.yaml().getConfigurationSection(id);
                int line = file.lines().key(id);
                if (s == null) {
                    diag.warn(file.path(), line, id, "is not a class (expected a block)");
                    continue;
                }
                for (String key : s.getKeys(false)) {
                    if (!KEYS.contains(key.toLowerCase(Locale.ROOT))) {
                        diag.warn(file.path(), file.lines().find(id, key + ":"), id,
                                "unknown key '" + key + "' (ignored)", Suggest.closest(key, KEYS));
                    }
                }
                String iconName = s.getString("icon", "BOOK");
                Material icon = Registries.itemMaterial(iconName).orElseGet(() -> {
                    diag.warn(file.path(), file.lines().find(id, "icon"), id, "unknown icon '" + iconName
                            + "' - using BOOK");
                    return Material.BOOK;
                });
                List<String> gems = s.getStringList("starting-gems");
                for (String g : gems) {
                    if (plugin.gemDefs().get(g).isEmpty()) {
                        diag.warn(file.path(), file.lines().find(id, g), id, "starting gem '" + g
                                + "' is not in gems.yml", Suggest.closest(g, plugin.gemDefs().ids()));
                    }
                }
                List<RpgClass.Passive> passives = new java.util.ArrayList<>();
                for (Map<?, ?> m : s.getMapList("passives")) {
                    String trig = String.valueOf(m.get("trigger"));
                    var type = com.celestirift.trigger.TriggerType.fromString(trig);
                    Object skillObj = m.get("skill");
                    if (type == null) {
                        diag.warn(file.path(), file.lines().find(id, trig), id, "passive: unknown trigger '" + trig + "'",
                                Suggest.closest(trig, java.util.Arrays.stream(com.celestirift.trigger.TriggerType.values())
                                        .map(t -> t.name().toLowerCase(Locale.ROOT)).toList()));
                        continue;
                    }
                    if (skillObj == null || plugin.skills().getSkill(String.valueOf(skillObj)).isEmpty()) {
                        String sk = String.valueOf(skillObj);
                        diag.warn(file.path(), file.lines().find(id, sk), id, "passive: skill '" + sk + "' does not exist",
                                Suggest.closest(sk, plugin.skills().skillNames()));
                        continue;
                    }
                    passives.add(new RpgClass.Passive(type, String.valueOf(skillObj),
                            num(m.get("chance"), 1.0), (int) Math.max(1, num(m.get("every"), 1)),
                            (int) Math.max(1, num(m.get("min-level"), 1))));
                }
                double defMana = plugin.getConfig().getDouble("mana.max", 100.0);
                double defRegen = plugin.getConfig().getDouble("mana.regen-per-second", 5.0);
                classes.put(id.toLowerCase(Locale.ROOT), new RpgClass(id,
                        s.getString("display", id), s.getString("description", ""), icon,
                        s.getDouble("mana.base", defMana), s.getDouble("mana.per-level", 0),
                        s.getDouble("mana-regen.base", defRegen), s.getDouble("mana-regen.per-level", 0),
                        Math.max(0, s.getInt("socket-bonus", 0)), gems, s.getString("permission"),
                        List.copyOf(passives)));
            }
        }
        plugin.getLogger().info("Loaded " + classes.size() + " class(es).");
        return classes.size();
    }
}

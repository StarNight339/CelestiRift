package com.celestirift.trigger;

import com.celestirift.CelestiRift;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and holds the {@code triggers:} section of config.yml, e.g.:
 * <pre>
 * triggers:
 *   SWAP_HANDS:
 *     - skill: FrostNova
 *       permission: celestirift.use
 *   SNEAK:
 *     - skill: Leap
 *       cancel: false
 * </pre>
 */
public final class TriggerBindings implements com.celestirift.util.Restorable {

    private final CelestiRift plugin;
    private final Map<TriggerType, List<TriggerBinding>> bindings = new EnumMap<>(TriggerType.class);

    public TriggerBindings(CelestiRift plugin) {
        this.plugin = plugin;
    }

    public List<TriggerBinding> get(TriggerType type) {
        return bindings.getOrDefault(type, List.of());
    }

    public boolean isEmpty() {
        return bindings.isEmpty();
    }

    @Override
    public Runnable snapshot() {
        var copy = new EnumMap<>(bindings);
        return () -> { bindings.clear(); bindings.putAll(copy); };
    }

    /** (Re)load bindings from config.yml. Called on enable and on /cr reload. */
    public void reload() {
        bindings.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("triggers");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            TriggerType type = TriggerType.fromString(key);
            if (type == null) {
                plugin.getLogger().warning("Unknown trigger type in config: '" + key + "' — skipping.");
                continue;
            }
            List<TriggerBinding> list = new ArrayList<>();
            for (Map<?, ?> entry : root.getMapList(key)) {
                Object skill = entry.get("skill");
                if (skill == null) {
                    continue;
                }
                Object perm = entry.get("permission");
                boolean cancel = entry.containsKey("cancel")
                        ? Boolean.parseBoolean(String.valueOf(entry.get("cancel")))
                        : type.cancellableByDefault();
                boolean requireItem = entry.containsKey("require-item")
                        && Boolean.parseBoolean(String.valueOf(entry.get("require-item")));
                list.add(new TriggerBinding(String.valueOf(skill),
                        perm == null ? null : String.valueOf(perm), cancel, requireItem));
            }
            if (!list.isEmpty()) {
                bindings.put(type, list);
            }
        }
        int total = bindings.values().stream().mapToInt(List::size).sum();
        plugin.getLogger().info("Loaded " + total + " trigger binding(s).");
    }
}

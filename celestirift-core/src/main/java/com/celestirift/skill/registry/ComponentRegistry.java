package com.celestirift.skill.registry;

import com.celestirift.api.skill.ConfigMap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Name → factory registry for mechanics, targeters or conditions. Several names
 * (aliases) can share one factory.
 *
 * <p>{@link #describe()} "probes" each factory with an empty {@link ConfigMap} and
 * records which attributes it reads — this powers validation, the shorthand
 * {@code - delay: 20} (first attribute = primary) and the generated schema/docs.
 */
public class ComponentRegistry<T> {

    /** One registered component: its names and the attributes it reads. */
    public record Description(String name, List<String> aliases, List<ConfigMap.Attribute> attributes) {
        public @Nullable String primary() {
            return attributes.isEmpty() ? null : attributes.getFirst().name();
        }
    }

    private static final Logger LOG = Logger.getLogger("CelestiRift");

    private final String kind;
    private final Map<String, Function<ConfigMap, T>> factories = new TreeMap<>();
    private final Map<Function<ConfigMap, T>, List<String>> namesByFactory = new IdentityHashMap<>();
    private Map<String, Description> described; // lazily built, reset on register

    public ComponentRegistry(String kind) {
        this.kind = kind;
    }

    /** Register under one or more (case-insensitive) names; the first is the display name. */
    public void register(Function<ConfigMap, T> factory, String... names) {
        for (String name : names) {
            factories.put(name.toLowerCase(Locale.ROOT), factory);
        }
        namesByFactory.computeIfAbsent(factory, f -> new ArrayList<>());
        for (String name : names) {
            namesByFactory.get(factory).add(name.toLowerCase(Locale.ROOT));
        }
        described = null;
    }

    public @Nullable T create(String name, ConfigMap config) {
        Function<ConfigMap, T> f = factories.get(name.toLowerCase(Locale.ROOT));
        return f == null ? null : f.apply(config);
    }

    public boolean has(String name) {
        return factories.containsKey(name.toLowerCase(Locale.ROOT));
    }

    /** All registered names including aliases. */
    public Set<String> names() {
        return factories.keySet();
    }

    /** One description per component (aliases grouped), keyed by display name, sorted. */
    public synchronized Map<String, Description> describe() {
        if (described == null) {
            Map<String, Description> out = new TreeMap<>();
            for (var e : namesByFactory.entrySet()) {
                List<String> names = e.getValue();
                List<ConfigMap.Attribute> attrs = List.of();
                ConfigMap probe = ConfigMap.parse("");
                try {
                    e.getKey().apply(probe);
                    attrs = probe.accessedAttributes();
                } catch (RuntimeException ex) {
                    LOG.fine("Could not probe " + kind + " '" + names.getFirst() + "': " + ex);
                    attrs = probe.accessedAttributes();
                }
                out.put(names.getFirst(), new Description(names.getFirst(),
                        List.copyOf(names.subList(1, names.size())), attrs));
            }
            described = out;
        }
        return described;
    }

    /** Description for any name or alias. */
    public @Nullable Description describe(String name) {
        Function<ConfigMap, T> f = factories.get(name.toLowerCase(Locale.ROOT));
        if (f == null) {
            return null;
        }
        List<String> names = namesByFactory.get(f);
        return names == null ? null : describe().get(names.getFirst());
    }

    /** Primary (shorthand) attribute of a component, or null. */
    public @Nullable String primary(String name) {
        Description d = describe(name);
        return d == null ? null : d.primary();
    }

    /** Display names only (no aliases), for suggestions. */
    public Map<String, Description> byDisplayName() {
        return new LinkedHashMap<>(describe());
    }
}

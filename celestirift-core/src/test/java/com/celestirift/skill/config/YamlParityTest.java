package com.celestirift.skill.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The shipped example.yml was rewritten in the structured format. Every skill must
 * compile to exactly the same lines as the legacy one-line version it replaced.
 */
class YamlParityTest {

    private static final TestPrimaries PRIMARIES = new TestPrimaries();

    private static YamlConfiguration load(String resource) {
        var in = Objects.requireNonNull(YamlParityTest.class.getClassLoader().getResourceAsStream(resource),
                resource);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    private static Object get(ConfigurationSection s, String key) {
        for (String k : s.getKeys(false)) {
            if (k.equalsIgnoreCase(key)) {
                return s.get(k);
            }
        }
        return null;
    }

    @Test
    void structuredExampleMatchesLegacy() {
        YamlConfiguration legacy = load("legacy-example.yml");
        YamlConfiguration v2 = load("structured-example.yml");
        assertEquals(legacy.getKeys(false), v2.getKeys(false), "same skills");

        for (String name : legacy.getKeys(false)) {
            ConfigurationSection a = legacy.getConfigurationSection(name);
            ConfigurationSection b = v2.getConfigurationSection(name);
            assertNotNull(a);
            assertNotNull(b);

            assertEquals(String.valueOf(get(a, "cooldown")), String.valueOf(get(b, "cooldown")), name + " cooldown");
            assertEquals(String.valueOf(get(a, "mana")), String.valueOf(get(b, "mana")), name + " mana");

            List<?> oldLines = (List<?>) get(a, "skills");
            List<?> newLines = (List<?>) get(b, "skills");
            assertEquals(oldLines.size(), newLines.size(), name + " line count");
            for (int i = 0; i < oldLines.size(); i++) {
                SkillLine expected = SkillLine.parse(String.valueOf(oldLines.get(i)));
                SkillLine actual = StructuredLine.parse((Map<?, ?>) newLines.get(i), PRIMARIES);
                assertSameLine(name + " line " + (i + 1), expected, actual);
            }

            List<?> oldConds = (List<?>) get(a, "conditions");
            Object newConds = get(b, "conditions");
            if (oldConds != null) {
                var expected = SkillLine.parseCondition(String.valueOf(oldConds.getFirst()));
                var actual = StructuredLine.parseConditions(newConds, PRIMARIES).getFirst();
                assertEquals(expected.name(), actual.name());
                assertEquals(expected.negated(), actual.negated());
                assertEquals(expected.config().asMap(), actual.config().asMap(), name + " condition");
            }
        }
    }

    /** Attribute aliases used by the legacy file, mapped to their primary names. */
    private static final Map<String, String> ATTR_ALIASES = Map.of("s", "skill");

    private static Map<String, String> canonicalAttrs(Map<String, String> attrs) {
        Map<String, String> out = new java.util.TreeMap<>();
        attrs.forEach((k, v) -> out.put(ATTR_ALIASES.getOrDefault(k, k), v));
        return out;
    }

    private static void assertSameLine(String where, SkillLine expected, SkillLine actual) {
        assertEquals(expected.mechanicName().toLowerCase(Locale.ROOT),
                actual.mechanicName().toLowerCase(Locale.ROOT), where + " mechanic");
        assertEquals(canonicalAttrs(expected.mechanicConfig().asMap()), canonicalAttrs(actual.mechanicConfig().asMap()),
                where + " attributes");
        assertEquals(TestPrimaries.canonicalTargeter(expected.targeterName()), TestPrimaries.canonicalTargeter(actual.targeterName()),
                where + " targeter");
        assertEquals(expected.targeterConfig().asMap(), actual.targeterConfig().asMap(), where + " targeter attrs");
        assertEquals(expected.conditions().size(), actual.conditions().size(), where + " conditions");
    }
}

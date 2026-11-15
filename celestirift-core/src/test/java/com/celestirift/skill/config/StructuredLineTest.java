package com.celestirift.skill.config;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StructuredLineTest {

    private static final TestPrimaries P = new TestPrimaries();

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    @Test
    void shorthandFillsPrimaryAttribute() {
        SkillLine line = StructuredLine.parse(map("delay", 20), P);
        assertEquals("delay", line.mechanicName());
        assertEquals(Map.of("ticks", "20"), line.mechanicConfig().asMap());
        assertNull(line.targeterName());
    }

    @Test
    void attributesTargetConditionsAndChance() {
        SkillLine line = StructuredLine.parse(map(
                "damage", map("amount", 6, "ignorearmor", true),
                "target", map("nearby", map("radius", 4)),
                "if", List.of(map("health", ">10"), "!permission{perm=vip}"),
                "chance", 0.5), P);
        assertEquals(Map.of("amount", "6", "ignorearmor", "true", "chance", "0.5"), line.mechanicConfig().asMap());
        assertEquals("nearby", line.targeterName());
        assertEquals(Map.of("radius", "4"), line.targeterConfig().asMap());
        assertEquals(2, line.conditions().size());
        assertEquals(Map.of("value", ">10"), line.conditions().get(0).config().asMap());
        assertFalse(line.conditions().get(0).negated());
        assertTrue(line.conditions().get(1).negated());
        assertEquals("permission", line.conditions().get(1).name());
    }

    @Test
    void targetAsInlineString() {
        SkillLine line = StructuredLine.parse(map("damage", 2, "target", "@target{range=10}"), P);
        assertEquals("target", line.targeterName());
        assertEquals(Map.of("range", "10"), line.targeterConfig().asMap());
    }

    @Test
    void notWrapperNegates() {
        var refs = StructuredLine.parseConditions(List.of(map("not", map("health", "<5"))), P);
        assertTrue(refs.getFirst().negated());
        assertEquals("health", refs.getFirst().name());
    }

    @Test
    void rejectsTwoMechanicsInOneLine() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> StructuredLine.parse(map("damage", 2, "heal", 3), P));
        assertTrue(ex.getMessage().contains("more than one mechanic"));
    }

    @Test
    void shorthandWithoutPrimaryIsMarkedForTheValidator() {
        SkillLine line = StructuredLine.parse(map("teleport", 5), P);
        assertEquals(Map.of(StructuredLine.SHORTHAND_KEY, "5"), line.mechanicConfig().asMap());
    }

    @Test
    void nullValueMeansNoAttributes() {
        SkillLine line = StructuredLine.parse(map("teleport", null, "target", "targetLocation"), P);
        assertTrue(line.mechanicConfig().asMap().isEmpty());
        assertEquals("targetLocation", line.targeterName());
    }
}

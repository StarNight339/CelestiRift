package com.celestirift.skill.config;

import com.celestirift.api.skill.ConfigMap;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConfigMapTest {

    @Test
    void parsesQuotedValuesAndFlags() {
        ConfigMap m = ConfigMap.parse("msg=\"a; b\";amount=5;silent");
        assertEquals("a; b", m.getString("", "msg"));
        assertEquals(5, m.getInt(0, "amount"));
        assertTrue(m.getBoolean(false, "silent"));
    }

    @Test
    void recordsAttributesInReadOrderWithAliases() {
        ConfigMap m = ConfigMap.parse("");
        m.getInt(20, "ticks", "t");
        m.getDouble(1.0, "speed", "s");
        var attrs = m.accessedAttributes();
        assertEquals("ticks", attrs.get(0).name());
        assertEquals(java.util.List.of("t"), attrs.get(0).aliases());
        assertEquals(ConfigMap.Type.INT, attrs.get(0).type());
        assertEquals(20, attrs.get(0).defaultValue());
        assertEquals("speed", attrs.get(1).name());
    }

    @Test
    void reportsUnusedKeysAndBadValues() {
        ConfigMap m = ConfigMap.parse("radus=3;amount=abc;t=4;chance=0.5");
        m.getDouble(1, "radius", "r");
        m.getInt(1, "amount");
        m.getInt(20, "ticks", "t");
        assertEquals(Set.of("radus"), m.unusedKeys("chance"));
        assertEquals(1, m.problems().size());
        assertEquals("amount", m.problems().getFirst().key());
        assertEquals(4, m.getInt(0, "ticks", "t"), "alias works");
    }

    @Test
    void acceptsWholeDecimalsForInts() {
        assertEquals(20, ConfigMap.parse("ticks=20.0").getInt(0, "ticks"));
    }

    @Test
    void booleanUnknownFallsBackToDefault() {
        ConfigMap m = ConfigMap.parse("pull=maybe");
        assertTrue(m.getBoolean(true, "pull"));
        assertEquals(1, m.problems().size());
    }
}

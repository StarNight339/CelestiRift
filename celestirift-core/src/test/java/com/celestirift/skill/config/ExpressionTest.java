package com.celestirift.skill.config;

import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.Expression;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionTest {

    private static double eval(String s) {
        return Expression.compile(s).eval(name -> 0);
    }

    @Test
    void precedenceAndAssociativity() {
        assertEquals(14, eval("2 + 3 * 4"));
        assertEquals(20, eval("(2 + 3) * 4"));
        assertEquals(512, eval("2 ^ 3 ^ 2"));   // right-associative
        assertEquals(-4, eval("-2 * 2"));
        assertEquals(1, eval("7 % 3"));
        assertEquals(0, eval("5 / 0"));         // no Infinity in skills
    }

    @Test
    void functions() {
        assertEquals(3, eval("max(1, 3)"));
        assertEquals(10, eval("clamp(42, 0, 10)"));
        assertEquals(2, eval("floor(2.9)"));
        assertEquals(4, eval("sqrt(16)"));
        double r = eval("random(5, 6)");
        assertTrue(r >= 5 && r < 6);
    }

    @Test
    void variablesResolvePerEvaluation() {
        Expression e = Expression.compile("6 + <caster.level> * 0.5");
        assertEquals(Set.of("caster.level"), e.variables());
        assertFalse(e.isConstant());
        assertEquals(11, e.eval(name -> name.equals("caster.level") ? 10 : 0));
        assertEquals(16, e.eval(name -> 20));
    }

    @Test
    void badInputGivesReadableErrors() {
        assertThrows(Expression.ParseException.class, () -> Expression.compile("2 +"));
        assertThrows(Expression.ParseException.class, () -> Expression.compile("(2"));
        var ex = assertThrows(Expression.ParseException.class, () -> Expression.compile("level * 2"));
        assertTrue(ex.getMessage().contains("<caster.level>"));
        assertThrows(Expression.ParseException.class, () -> Expression.compile("pow(2, 3)"));
        assertThrows(Expression.ParseException.class, () -> Expression.compile("min(1)"));
    }

    @Test
    void configMapNumbers() {
        ConfigMap m = ConfigMap.of(Map.of("amount", "4 + <caster.level>", "fixed", "2 * 3", "bad", "4 +"));
        var amount = m.getNumber(1, "amount");
        assertFalse(amount.isConstant());
        assertEquals(6, m.getDouble(0, "fixed"));               // constant arithmetic is fine
        assertTrue(m.getNumber(9, "bad").isConstant());         // falls back to default
        assertEquals(1, m.problems().size());
        assertNotNull(m.problems().get(0).detail());
        assertEquals(ConfigMap.Type.NUMBER, m.accessedAttributes().get(0).type());
    }

    @Test
    void doubleRejectsPerCastExpressions() {
        ConfigMap m = ConfigMap.of(Map.of("radius", "<caster.level>"));
        assertEquals(5, m.getDouble(5, "radius"));
        assertEquals(1, m.problems().size());
    }

    @Test
    void choices() {
        ConfigMap m = ConfigMap.of(Map.of("filter", "Allies", "sort", "closest"));
        assertEquals("allies", m.getChoice("enemies", List.of("enemies", "allies"), "filter"));
        assertEquals("none", m.getChoice("none", List.of("none", "nearest"), "sort"));
        assertEquals(1, m.problems().size());
    }
}

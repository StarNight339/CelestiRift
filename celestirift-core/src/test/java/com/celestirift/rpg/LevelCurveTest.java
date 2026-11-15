package com.celestirift.rpg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LevelCurveTest {

    private final LevelCurve curve = new LevelCurve(100, 1.5, 50);

    @Test
    void xpPerLevelFollowsTheCurve() {
        assertEquals(100, curve.xpToNext(1));
        assertEquals(283, curve.xpToNext(2));      // 100 * 2^1.5 = 282.8
        assertEquals(3162, curve.xpToNext(10));
        assertTrue(Double.isInfinite(curve.xpToNext(50)));
    }

    @Test
    void addCarriesOverMultipleLevels() {
        LevelCurve.Result r = curve.add(1, 50, 400);  // 450 total: L1 needs 100, L2 needs 283
        assertEquals(3, r.level());
        assertEquals(2, r.levelsGained());
        assertEquals(67, r.xp(), 0.001);
    }

    @Test
    void stopsAtMaxLevel() {
        LevelCurve.Result r = new LevelCurve(10, 0, 3).add(1, 0, 1_000);
        assertEquals(3, r.level());
        assertEquals(0, r.xp());
    }

    @Test
    void noLevelUpBelowThreshold() {
        LevelCurve.Result r = curve.add(5, 10, 5);
        assertEquals(5, r.level());
        assertEquals(15, r.xp(), 0.001);
        assertEquals(0, r.levelsGained());
    }
}

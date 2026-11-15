package com.celestirift.rpg;

/**
 * XP needed per level: {@code base × level^exponent} (rounded, at least 1), capped at
 * {@code maxLevel}. Pure math so it can be unit-tested.
 *
 * <p>Default (base 100, exponent 1.5): level 1→2 needs 100, 10→11 needs 3162, 49→50 needs 34300.
 */
public record LevelCurve(double base, double exponent, int maxLevel) {

    /** Result of adding XP. */
    public record Result(int level, double xp, int levelsGained) {}

    public LevelCurve {
        base = Math.max(1, base);
        exponent = Math.max(0, exponent);
        maxLevel = Math.max(1, maxLevel);
    }

    /** XP needed to go from {@code level} to {@code level + 1}; infinite at max level. */
    public double xpToNext(int level) {
        if (level >= maxLevel) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.max(1, Math.round(base * Math.pow(Math.max(1, level), exponent)));
    }

    /** Add XP, carrying over into as many level-ups as it pays for. XP is 0 at max level. */
    public Result add(int level, double xp, double amount) {
        int lvl = Math.max(1, Math.min(maxLevel, level));
        double cur = Math.max(0, xp) + Math.max(0, amount);
        int gained = 0;
        while (lvl < maxLevel && cur >= xpToNext(lvl)) {
            cur -= xpToNext(lvl);
            lvl++;
            gained++;
        }
        if (lvl >= maxLevel) {
            cur = 0;
        }
        return new Result(lvl, cur, gained);
    }
}

package com.celestirift.rpg;

import org.jetbrains.annotations.Nullable;

/** Persistent RPG state of a player: level, XP towards the next level, and class. */
public final class PlayerProfile {

    private int level;
    private double xp;
    private @Nullable String classId;

    public PlayerProfile(int level, double xp, @Nullable String classId) {
        this.level = Math.max(1, level);
        this.xp = Math.max(0, xp);
        this.classId = classId == null || classId.isBlank() ? null : classId;
    }

    public static PlayerProfile fresh() {
        return new PlayerProfile(1, 0, null);
    }

    public int level() { return level; }
    public double xp() { return xp; }
    public @Nullable String classId() { return classId; }

    void level(int level) { this.level = Math.max(1, level); }
    void xp(double xp) { this.xp = Math.max(0, xp); }
    void classId(@Nullable String classId) { this.classId = classId; }
}

package com.celestirift.rpg.boss;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A boss from bosses.yml. */
public record BossDef(String id, EntityType type, String name, double health, @Nullable Double damage,
                      @Nullable Bar bar, boolean glowing,
                      List<String> onSpawn, List<Timer> timers, List<Chance> onDamaged,
                      List<Phase> phases, List<String> onDeath) {

    /** Boss bar shown to players within {@code radius} blocks. */
    public record Bar(BossBar.Color color, BossBar.Overlay overlay, double radius) {}

    /** Cast {@code skill} every {@code every} ticks while the boss is loaded. */
    public record Timer(int every, String skill) {}

    /** Cast {@code skill} with {@code chance} when the boss is hit (@trigger = attacker). */
    public record Chance(String skill, double chance) {}

    /** Cast {@code skill} once when health first drops below {@code below} (0-1 of max). */
    public record Phase(double below, String skill) {}
}

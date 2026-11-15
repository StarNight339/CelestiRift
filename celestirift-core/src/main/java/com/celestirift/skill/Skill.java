package com.celestirift.skill;

import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.SkillTargets;
import com.celestirift.api.skill.targeter.Targeter;

import com.celestirift.api.skill.condition.Condition;
import com.celestirift.api.skill.mechanic.Mechanic;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A compiled, ready-to-run skill: a name, some metadata (cooldown/mana), and an
 * ordered list of lines. Compilation happens once at load time in SkillManager.
 */
public final class Skill {

    /** One compiled line: mechanic + targeter + conditions + line-level chance. */
    public record Line(Mechanic mechanic,
                       @Nullable Targeter targeter,
                       List<CompiledCondition> conditions,
                       double chance,
                       int delayTicks) {}

    public record CompiledCondition(Condition condition, boolean negated) {}

    /**
     * Per-skill override of the skill-item gradient colours. Any null field
     * falls back to the global {@code items:} config in SkillItems.
     */
    public record ItemStyle(@Nullable String nameFrom, @Nullable String nameTo,
                            @Nullable String manaFrom, @Nullable String manaTo,
                            @Nullable String model) {
        public static final ItemStyle EMPTY = new ItemStyle(null, null, null, null, null);
    }

    /**
     * Cast time: the player casts for {@code ticks} before the skill fires.
     *
     * @param ticks           0 = instant
     * @param interruptMove   moving cancels the cast
     * @param interruptDamage taking damage cancels the cast
     */
    public record Channel(int ticks, boolean interruptMove, boolean interruptDamage) {
        public static final Channel NONE = new Channel(0, false, false);
    }

    private Channel channel = Channel.NONE;

    /** Set by the loader right after construction. */
    public Skill channel(Channel channel) {
        this.channel = channel;
        return this;
    }

    public Channel channel() { return channel; }

    private final String name;
    private final double cooldownSeconds;
    private final double manaCost;
    private final List<Line> lines;
    private final ItemStyle itemStyle;
    private final List<CompiledCondition> conditions;
    private final String description;

    public Skill(String name, double cooldownSeconds, double manaCost,
                 List<Line> lines, ItemStyle itemStyle, List<CompiledCondition> conditions) {
        this(name, cooldownSeconds, manaCost, lines, itemStyle, conditions, "");
    }

    public Skill(String name, double cooldownSeconds, double manaCost,
                 List<Line> lines, ItemStyle itemStyle, List<CompiledCondition> conditions,
                 String description) {
        this.description = description == null ? "" : description;
        this.name = name;
        this.cooldownSeconds = cooldownSeconds;
        this.manaCost = manaCost;
        this.lines = lines;
        this.itemStyle = itemStyle;
        this.conditions = conditions;
    }

    public String name() { return name; }
    public double cooldownSeconds() { return cooldownSeconds; }
    public double manaCost() { return manaCost; }
    public List<Line> lines() { return lines; }
    public ItemStyle itemStyle() { return itemStyle; }

    /** Skill-level gates evaluated before the skill casts at all. */
    public List<CompiledCondition> conditions() { return conditions; }

    /** MiniMessage description (may be empty). */
    public String description() { return description; }
}

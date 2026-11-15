package com.celestirift.skill;

import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.SkillTargets;
import com.celestirift.api.skill.targeter.Targeter;

import com.celestirift.CelestiRift;
import com.celestirift.api.skill.mechanic.NoTargetMechanic;
import com.celestirift.api.skill.mechanic.TargetedEntityMechanic;
import com.celestirift.api.skill.mechanic.TargetedLocationMechanic;
import com.celestirift.skill.targeter.SelfTargeter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Runs a compiled {@link Skill}. Handles per-line targeter resolution,
 * conditions, chance rolls, and {@code delay} lines (which pause the remaining
 * lines using the Bukkit scheduler, so timelines work like MythicMobs).
 */
public final class SkillExecutor {

    private static final Targeter DEFAULT_TARGETER = new SelfTargeter();

    private final CelestiRift plugin;

    public SkillExecutor(CelestiRift plugin) {
        this.plugin = plugin;
    }

    public void execute(Skill skill, SkillMetadata meta) {
        runFrom(skill, 0, meta);
    }

    private void runFrom(Skill skill, int startIndex, SkillMetadata meta) {
        List<Skill.Line> lines = skill.lines();
        for (int i = startIndex; i < lines.size(); i++) {
            Skill.Line line = lines.get(i);

            if (line.delayTicks() > 0) {
                final int next = i + 1;
                plugin.getServer().getScheduler().runTaskLater(plugin,
                        () -> runFrom(skill, next, meta), line.delayTicks());
                return; // remaining lines resume after the delay
            }

            try {
                runLine(line, meta);
            } catch (RuntimeException e) {
                // One broken line (often an add-on mechanic) must not stop the rest of the skill.
                reportFailure(skill, i, line, e);
            }
        }
    }

    /** Last time a failure was logged per skill line, so a looping skill can't flood the console. */
    private final java.util.Map<String, Long> lastReport = new java.util.HashMap<>();

    private int failures;

    /** Skill lines that threw since startup (used by the smoke test). */
    public int failures() {
        return failures;
    }

    private void reportFailure(Skill skill, int index, Skill.Line line, RuntimeException e) {
        failures++;
        String key = skill.name() + "#" + index;
        long now = System.currentTimeMillis();
        Long last = lastReport.get(key);
        if (last != null && now - last < 30_000) {
            return;
        }
        lastReport.put(key, now);
        String what = "Skill '" + skill.name() + "' line " + (index + 1) + " ("
                + line.mechanic().getClass().getSimpleName() + ") failed: " + e;
        if (plugin.debug()) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, what, e);
        } else {
            plugin.getLogger().warning(what + " - set settings.debug: true for the stack trace.");
        }
    }

    private void runLine(Skill.Line line, SkillMetadata meta) {
        if (line.chance() < 1.0 && ThreadLocalRandom.current().nextDouble() > line.chance()) {
            return;
        }

        Targeter targeter = line.targeter() != null ? line.targeter() : DEFAULT_TARGETER;
        SkillTargets targets = targeter.resolve(meta);

        if (line.mechanic() instanceof NoTargetMechanic noTarget) {
            if (conditionsPass(line, meta, null)) {
                noTarget.cast(meta);
            }
            return;
        }

        if (line.mechanic() instanceof TargetedEntityMechanic entityMech) {
            for (Entity target : targets.entities()) {
                if (conditionsPass(line, meta, target)) {
                    entityMech.cast(meta, target);
                }
            }
            return;
        }

        if (line.mechanic() instanceof TargetedLocationMechanic locMech) {
            for (Location target : targets.locations()) {
                if (conditionsPass(line, meta, null)) {
                    locMech.cast(meta, target);
                }
            }
        }
    }

    private boolean conditionsPass(Skill.Line line, SkillMetadata meta, Entity target) {
        for (Skill.CompiledCondition cc : line.conditions()) {
            boolean result = cc.condition().test(meta, target);
            if (cc.negated()) {
                result = !result;
            }
            if (!result) {
                return false;
            }
        }
        return true;
    }
}

package com.celestirift.skill.condition.impl;

import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.condition.Condition;
import com.celestirift.api.skill.ConfigMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * {@code ?health{value=>10}} — compares the health of the current entity
 * (target if present, else caster). Operators: {@code > < >= <= ==}.
 */
public final class HealthCondition implements Condition {

    private final String op;
    private final double threshold;

    public HealthCondition(ConfigMap cfg) {
        String raw = cfg.getString(">0", "value", "v", "amount", "a").trim();
        String operator = ">";
        int idx = 0;
        if (raw.startsWith(">=") || raw.startsWith("<=") || raw.startsWith("==")) {
            operator = raw.substring(0, 2);
            idx = 2;
        } else if (raw.startsWith(">") || raw.startsWith("<") || raw.startsWith("=")) {
            operator = raw.startsWith("=") ? "==" : raw.substring(0, 1);
            idx = 1;
        }
        this.op = operator;
        double parsed;
        try {
            parsed = Double.parseDouble(raw.substring(idx).trim());
        } catch (NumberFormatException e) {
            parsed = 0;
        }
        this.threshold = parsed;
    }

    @Override
    public boolean test(SkillMetadata meta, @Nullable Entity target) {
        Entity subject = target != null ? target : meta.caster();
        if (!(subject instanceof LivingEntity le)) {
            return false;
        }
        double hp = le.getHealth();
        return switch (op) {
            case "<" -> hp < threshold;
            case "<=" -> hp <= threshold;
            case ">=" -> hp >= threshold;
            case "==" -> hp == threshold;
            default -> hp > threshold;
        };
    }
}

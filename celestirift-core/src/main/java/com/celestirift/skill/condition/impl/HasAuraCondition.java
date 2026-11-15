package com.celestirift.skill.condition.impl;

import com.celestirift.CelestiRift;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.condition.Condition;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * {@code ?hasaura{id=Mark}} - the target (or caster) has the aura.
 * {@code ?hasaura{id=Mark;stacks=>=3}} compares stacks ({@code > < >= <= ==}).
 */
public final class HasAuraCondition implements Condition {

    private final CelestiRift plugin;
    private final String id;
    private final String op;
    private final int amount;

    public HasAuraCondition(CelestiRift plugin, ConfigMap cfg) {
        this.plugin = plugin;
        this.id = cfg.getString("", "id", "name");
        String raw = cfg.getString(">=1", "stacks", "s").replace(" ", "");
        String operator = ">=";
        String num = raw;
        for (String o : new String[]{">=", "<=", "==", ">", "<", "="}) {
            if (raw.startsWith(o)) {
                operator = o.equals("=") ? "==" : o;
                num = raw.substring(o.length());
                break;
            }
        }
        int parsed;
        try {
            parsed = Integer.parseInt(num);
        } catch (NumberFormatException e) {
            parsed = 1;
        }
        this.op = operator;
        this.amount = parsed;
    }

    @Override
    public boolean test(SkillMetadata meta, @Nullable Entity target) {
        Entity subject = target != null ? target : meta.caster();
        int s = plugin.auras().stacks(subject, id);
        return switch (op) {
            case ">" -> s > amount;
            case "<" -> s < amount;
            case "<=" -> s <= amount;
            case "==" -> s == amount;
            default -> s >= amount;
        };
    }
}

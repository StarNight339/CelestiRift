package com.celestirift.skill.condition.impl;

import com.celestirift.CelestiRift;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.condition.Condition;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@code class{is=Mage,Warlock}} — the player has one of these classes ({@code none} =
 * no class). Checks the target when it's a player, else the caster.
 * Shorthand: {@code - class: Mage}.
 */
public final class ClassCondition implements Condition {

    private final CelestiRift plugin;
    private final Set<String> classes;

    public ClassCondition(CelestiRift plugin, ConfigMap cfg) {
        this.plugin = plugin;
        this.classes = Arrays.stream(cfg.getString("", "is", "class", "classes").split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean test(SkillMetadata meta, @Nullable Entity target) {
        Entity subject = target instanceof Player ? target : meta.caster();
        if (!(subject instanceof Player p)) {
            return false;
        }
        String id = plugin.profiles().get(p).classId();
        return classes.contains(id == null ? "none" : id.toLowerCase(Locale.ROOT));
    }
}

package com.celestirift.skill.condition.impl;

import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.condition.Condition;
import com.celestirift.api.skill.ConfigMap;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

/** {@code ?permission{perm=celestirift.vip}} — true if the caster has the node. */
public final class PermissionCondition implements Condition {

    private final String node;

    public PermissionCondition(ConfigMap cfg) {
        this.node = cfg.getString("", "perm", "permission", "p");
    }

    @Override
    public boolean test(SkillMetadata meta, @Nullable Entity target) {
        return !node.isBlank() && meta.caster().hasPermission(node);
    }
}

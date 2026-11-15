package com.celestirift.skill.mechanic.impl;

import com.celestirift.CelestiRift;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.mechanic.TargetedEntityMechanic;
import org.bukkit.entity.Entity;

import java.util.List;

/**
 * {@code removeaura{id=Mark}} - remove an aura (or {@code stacks=n} of it).
 * {@code removeaura{type=debuff}} cleanses every debuff; {@code id=all} removes everything.
 * {@code runend=true} also casts the aura's onend skill.
 */
public final class RemoveAuraMechanic implements TargetedEntityMechanic {

    private final CelestiRift plugin;
    private final String id;
    private final String type;
    private final int stacks;
    private final boolean runEnd;

    public RemoveAuraMechanic(CelestiRift plugin, ConfigMap cfg) {
        this.plugin = plugin;
        this.id = cfg.getString("all", "id", "name");
        this.type = cfg.getChoice("any", List.of("any", "buff", "debuff"), "type");
        this.stacks = Math.max(0, cfg.getInt(0, "stacks"));
        this.runEnd = cfg.getBoolean(false, "runend");
    }

    @Override
    public void cast(SkillMetadata meta, Entity target) {
        plugin.auras().remove(target, id.equalsIgnoreCase("all") ? null : id,
                type.equals("any") ? null : type, stacks, runEnd);
    }
}

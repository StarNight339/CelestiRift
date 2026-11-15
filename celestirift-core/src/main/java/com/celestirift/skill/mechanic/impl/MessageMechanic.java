package com.celestirift.skill.mechanic.impl;

import com.celestirift.skill.Placeholders;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.TargetedEntityMechanic;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * {@code message{msg="..."}} — sends a MiniMessage-formatted message to each
 * targeted player. Supports {@code <caster.name>} etc. placeholders.
 */
public final class MessageMechanic implements TargetedEntityMechanic {

    private final String message;

    public MessageMechanic(ConfigMap cfg) {
        this.message = cfg.getString("", "msg", "m", "message");
    }

    @Override
    public void cast(SkillMetadata meta, Entity target) {
        if (target instanceof Player player) {
            String parsed = Placeholders.apply(message, meta, target);
            player.sendMessage(MiniMessage.miniMessage().deserialize(parsed, com.celestirift.integration.Orion.tags()));
        }
    }
}

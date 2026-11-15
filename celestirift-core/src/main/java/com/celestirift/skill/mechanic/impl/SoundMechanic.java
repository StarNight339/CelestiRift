package com.celestirift.skill.mechanic.impl;

import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.TargetedLocationMechanic;
import org.bukkit.Location;

/**
 * {@code sound{sound=entity.blaze.shoot;volume=1;pitch=1}} — plays a sound at
 * each targeted location. {@code sound} is a Minecraft sound key.
 */
public final class SoundMechanic implements TargetedLocationMechanic {

    private final String sound;
    private final float volume;
    private final float pitch;

    public SoundMechanic(ConfigMap cfg) {
        this.sound = cfg.getString("entity.experience_orb.pickup", "sound", "s").toLowerCase(java.util.Locale.ROOT);
        this.volume = cfg.getFloat(1.0f, "volume", "vol", "v");
        this.pitch = cfg.getFloat(1.0f, "pitch", "p");
    }

    @Override
    public void cast(SkillMetadata meta, Location target) {
        if (target.getWorld() != null) {
            target.getWorld().playSound(target, sound, volume, pitch);
        }
    }
}

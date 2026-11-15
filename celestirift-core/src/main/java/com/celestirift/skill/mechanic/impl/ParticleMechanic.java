package com.celestirift.skill.mechanic.impl;

import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.TargetedLocationMechanic;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.logging.Logger;

/**
 * {@code particle{type=FLAME;amount=20;spread=0.5;speed=0.05}} — spawns a
 * particle cloud at each targeted location. {@code type} is a Bukkit
 * {@link Particle} name.
 */
public final class ParticleMechanic implements TargetedLocationMechanic {

    private static final Logger LOG = Logger.getLogger("CelestiRift");

    private final Particle particle;
    private final int amount;
    private final double spread;
    private final double speed;

    public ParticleMechanic(ConfigMap cfg) {
        this.particle = resolve(cfg.getString("FLAME", "type", "particle", "p"));
        this.amount = cfg.getInt(15, "amount", "count", "a");
        this.spread = cfg.getDouble(0.4, "spread", "offset", "o");
        this.speed = cfg.getDouble(0.02, "speed", "s");
    }

    private static Particle resolve(String name) {
        return com.celestirift.util.Registries.particle(name).orElseGet(() -> {
            LOG.warning("[CelestiRift] Unknown particle '" + name + "', defaulting to FLAME");
            return Particle.FLAME;
        });
    }

    @Override
    public void cast(SkillMetadata meta, Location target) {
        if (target.getWorld() != null) {
            target.getWorld().spawnParticle(particle, target, amount, spread, spread, spread, speed);
        }
    }
}

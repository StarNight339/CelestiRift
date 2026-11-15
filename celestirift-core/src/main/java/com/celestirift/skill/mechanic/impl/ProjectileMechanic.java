package com.celestirift.skill.mechanic.impl;

import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.mechanic.NoTargetMechanic;
import org.bukkit.entity.*;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.Locale;

/**
 * {@code projectile{type=FIREBALL;velocity=1.5}} — the caster launches a vanilla
 * projectile in its facing direction. Supported types: FIREBALL, SMALL_FIREBALL,
 * SNOWBALL, ARROW, WITHER_SKULL, EGG, ENDER_PEARL, TRIDENT.
 */
public final class ProjectileMechanic implements NoTargetMechanic {

    private final Class<? extends Projectile> type;
    private final double velocity;

    public ProjectileMechanic(ConfigMap cfg) {
        this.type = resolve(cfg.getString("SNOWBALL", "type", "t"));
        this.velocity = cfg.getDouble(1.5, "velocity", "v", "speed");
    }

    private static Class<? extends Projectile> resolve(String name) {
        return switch (name.toUpperCase(Locale.ROOT)) {
            case "FIREBALL", "LARGE_FIREBALL" -> Fireball.class;
            case "SMALL_FIREBALL" -> SmallFireball.class;
            case "ARROW" -> Arrow.class;
            case "WITHER_SKULL" -> WitherSkull.class;
            case "EGG" -> Egg.class;
            case "ENDER_PEARL" -> EnderPearl.class;
            case "TRIDENT" -> Trident.class;
            default -> Snowball.class;
        };
    }

    @Override
    public void cast(SkillMetadata meta) {
        if (!(meta.caster() instanceof ProjectileSource source)) {
            return;
        }
        Vector dir = meta.caster().getLocation().getDirection().normalize().multiply(velocity);
        Projectile projectile = source.launchProjectile(type, dir);
        projectile.setShooter(source);
    }
}

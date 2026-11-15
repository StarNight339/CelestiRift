package com.celestirift.skill.mechanic.impl;

import com.celestirift.CelestiRift;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.mechanic.TargetedLocationMechanic;
import com.celestirift.util.Registries;
import com.celestirift.vfx.DisplayFx;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Locale;

/**
 * {@code meteor{block=MAGMA_BLOCK;height=18;duration=20;onland=MeteorImpact}} - a real
 * spinning block falls from the sky onto the target location, leaving a trail, then
 * casts {@code onland} with the landing spot as origin. Pair it with a
 * {@code telegraph} of the same duration to warn players.
 */
public final class MeteorMechanic implements TargetedLocationMechanic {

    private final CelestiRift plugin;
    private final Material block;
    private final double height;
    private final double drift;
    private final int duration;
    private final float scale;
    private final String trail;
    private final String onLand;

    public MeteorMechanic(CelestiRift plugin, ConfigMap cfg) {
        this.plugin = plugin;
        this.onLand = cfg.getString("", "onland", "skill", "s");
        Material m = Material.matchMaterial(cfg.getString("MAGMA_BLOCK", "block", "material", "m")
                .toUpperCase(Locale.ROOT));
        this.block = m != null && m.isBlock() ? m : Material.MAGMA_BLOCK;
        this.height = cfg.getDouble(18, "height", "h");
        this.drift = cfg.getDouble(6, "drift", "angle");
        this.duration = Math.max(2, cfg.getInt(20, "duration", "d", "ticks"));
        this.scale = (float) cfg.getDouble(1.6, "scale", "size");
        this.trail = cfg.getString("FLAME", "trail", "particle");
    }

    @Override
    public void cast(SkillMetadata meta, Location target) {
        Location land = target.clone();
        // Come in at an angle, from behind the caster.
        Vector back = meta.caster().getLocation().getDirection().setY(0);
        if (back.lengthSquared() < 1e-4) {
            back = new Vector(1, 0, 0);
        }
        back.normalize().multiply(-drift);
        Location start = land.clone().add(back).add(0, height, 0);
        Vector step = land.toVector().subtract(start.toVector()).multiply(1.0 / duration);
        Particle trailParticle = Registries.particle(trail).orElse(Particle.FLAME);

        BlockDisplay d = DisplayFx.spawn(start, BlockDisplay.class, e -> {
            e.setBlock(block.createBlockData());
            e.setTeleportDuration(1);
            e.setTransformation(DisplayFx.transform(scale, 0, 0, 0, true));
        });

        new BukkitRunnable() {
            int t;
            final Location pos = start.clone();

            @Override
            public void run() {
                if (!d.isValid()) {
                    cancel();
                    return;
                }
                t++;
                pos.add(step);
                d.teleport(pos);
                DisplayFx.animate(d, DisplayFx.transform(scale, t * 25f, t * 17f, 0, true), 1);
                if (trailParticle.getDataType() == Void.class) {
                    pos.getWorld().spawnParticle(trailParticle, pos.clone().add(0, scale / 2, 0), 6, 0.3, 0.3, 0.3, 0.02);
                }
                pos.getWorld().spawnParticle(Particle.LARGE_SMOKE, pos, 2, 0.2, 0.2, 0.2, 0.01);
                if (t >= duration) {
                    cancel();
                    DisplayFx.remove(d);
                    land.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, land, 1);
                    land.getWorld().playSound(land, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.8f);
                    if (!onLand.isBlank() && meta.caster().isValid()) {
                        plugin.skills().cast(onLand, new SkillMetadata(meta.caster(), meta.trigger(), land,
                                meta.power()).rangeMultiplier(meta.rangeMultiplier()));
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}

package com.celestirift.vfx;

import com.celestirift.api.skill.ConfigMap;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

import java.util.Locale;
import java.util.logging.Logger;

/**
 * Parsed particle appearance shared by all VFX shape mechanics. Handles the
 * particle type plus the special data some particles need:
 * <ul>
 *   <li>{@code DUST} → colour + size via {@link Particle.DustOptions}</li>
 *   <li>{@code DUST_COLOR_TRANSITION} → {@code color} → {@code tocolor}</li>
 * </ul>
 * Common attributes: {@code particle}, {@code count}, {@code offset},
 * {@code speed}, {@code color} (#RRGGBB), {@code size}, {@code tocolor}.
 */
public final class ParticleSpec {

    private static final Logger LOG = Logger.getLogger("CelestiRift");

    private final Particle particle;
    private final int count;
    private final double offset;
    private final double speed;
    private final Object data; // DustOptions / DustTransition / null

    private ParticleSpec(Particle particle, int count, double offset, double speed, Object data) {
        this.particle = particle;
        this.count = count;
        this.offset = offset;
        this.speed = speed;
        this.data = data;
    }

    public static ParticleSpec parse(ConfigMap cfg) {
        Particle particle = resolve(cfg.getString("FLAME", "particle", "type", "p"));
        int count = Math.max(1, cfg.getInt(1, "count", "amount", "a"));
        double offset = cfg.getDouble(0.0, "offset", "spread", "o");
        double speed = cfg.getDouble(0.0, "speed", "s");

        // Read every attribute up front so it is documented and validated even when
        // the chosen particle ignores it.
        float size = cfg.getFloat(1.0f, "size");
        Color color = parseColor(cfg.getString("#ffffff", "color", "c", "fromcolor"));
        Color to = parseColor(cfg.getString("#ffffff", "tocolor", "to"));

        Object data = null;
        if (particle == Particle.DUST) {
            data = new Particle.DustOptions(color, size);
        } else if (particle == Particle.DUST_COLOR_TRANSITION) {
            data = new Particle.DustTransition(color, to, size);
        }
        return new ParticleSpec(particle, count, offset, speed, data);
    }

    /** Spawn this particle once at a point. */
    public void spawn(Location loc) {
        World w = loc.getWorld();
        if (w == null) {
            return;
        }
        if (data != null) {
            w.spawnParticle(particle, loc, count, offset, offset, offset, speed, data);
        } else {
            w.spawnParticle(particle, loc, count, offset, offset, offset, speed);
        }
    }

    private static Particle resolve(String name) {
        return com.celestirift.util.Registries.particle(name).orElseGet(() -> {
            LOG.warning("[CelestiRift] Unknown particle '" + name + "', defaulting to FLAME");
            return Particle.FLAME;
        });
    }

    private static Color parseColor(String hex) {
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            return Color.fromRGB(Integer.parseInt(h, 16));
        } catch (RuntimeException e) {
            return Color.WHITE;
        }
    }
}

package com.celestirift.vfx;

import com.celestirift.CelestiRift;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Spawns short-lived display entities for skill effects. They are never saved to disk
 * ({@code persistent=false}), are tracked, and are all removed on reload/shutdown, so
 * a crash or /reload can't leave floating swords behind.
 */
public final class DisplayFx {

    private static final Set<UUID> LIVE = new HashSet<>();
    private static CelestiRift plugin;

    private DisplayFx() {}

    public static void install(CelestiRift p) {
        plugin = p;
    }

    public static <T extends Display> T spawn(Location at, Class<T> type, Consumer<T> setup) {
        T d = at.getWorld().spawn(at, type, e -> {
            e.setPersistent(false);
            e.setViewRange(1.5f);
            setup.accept(e);
        });
        LIVE.add(d.getUniqueId());
        return d;
    }

    public static void remove(Entity e) {
        LIVE.remove(e.getUniqueId());
        e.remove();
    }

    /** Remove every effect entity still alive. */
    public static void clearAll() {
        if (plugin == null) {
            return;
        }
        for (UUID id : Set.copyOf(LIVE)) {
            Entity e = plugin.getServer().getEntity(id);
            if (e != null) {
                e.remove();
            }
        }
        LIVE.clear();
    }

    public static int live() {
        LIVE.removeIf(id -> plugin == null || plugin.getServer().getEntity(id) == null);
        return LIVE.size();
    }

    /**
     * A transformation: uniform scale, rotation around Y (yaw) then X (tilt), lifted by {@code y}.
     * {@code centered} shifts block displays so the block is centred on the location.
     */
    public static Transformation transform(float scale, float yawDeg, float tiltDeg, float y, boolean centered) {
        AxisAngle4f yaw = new AxisAngle4f((float) Math.toRadians(-yawDeg), 0, 1, 0);
        AxisAngle4f tilt = new AxisAngle4f((float) Math.toRadians(tiltDeg), 1, 0, 0);
        Vector3f translation = new Vector3f(0, y, 0);
        if (centered) {
            // Rotate the half-block offset the same way, so the block spins around its centre.
            Vector3f half = new Vector3f(-scale / 2, -scale / 2, -scale / 2);
            new org.joml.Quaternionf(yaw).mul(new org.joml.Quaternionf(tilt)).transform(half);
            translation.add(half).add(0, scale / 2, 0);
        }
        return new Transformation(translation, new org.joml.Quaternionf(yaw).mul(new org.joml.Quaternionf(tilt)),
                new Vector3f(scale, scale, scale), new org.joml.Quaternionf());
    }

    /** Animate to {@code to} over {@code ticks} (client-side interpolation). */
    public static void animate(Display d, Transformation to, int ticks) {
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(Math.max(1, ticks));
        d.setTransformation(to);
    }
}

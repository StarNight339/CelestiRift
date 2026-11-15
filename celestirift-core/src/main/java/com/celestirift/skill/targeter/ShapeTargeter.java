package com.celestirift.skill.targeter;

import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.SkillTargets;
import com.celestirift.api.skill.targeter.Targeter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Area targeters shaped for melee/skill-shots. All accept the {@link TargetFilter}
 * options ({@code filter} default enemies, {@code limit}, {@code sort}):
 * <ul>
 *   <li>{@code cone{angle=90;range=5}}: in front of the caster, within the angle</li>
 *   <li>{@code line{length=12;width=1}}: a straight strip from the caster's eyes</li>
 *   <li>{@code ring{inner=3;outer=6}}: between two radii around the origin (dodge by
 *       standing close)</li>
 * </ul>
 * Entities count by their closest point (hitbox), so big mobs are easy to hit.
 */
public final class ShapeTargeter implements Targeter {

    public enum Shape { CONE, LINE, RING }

    private final Shape shape;
    private final double a; // cone: range, line: length, ring: inner
    private final double b; // cone: half-angle cosine, line: width, ring: outer
    private final TargetFilter filter;

    public ShapeTargeter(Shape shape, ConfigMap cfg) {
        this.shape = shape;
        switch (shape) {
            case CONE -> {
                this.a = cfg.getDouble(5, "range", "r", "length");
                double angle = Math.max(1, Math.min(360, cfg.getDouble(90, "angle", "a")));
                this.b = Math.cos(Math.toRadians(angle / 2));
            }
            case LINE -> {
                this.a = cfg.getDouble(12, "length", "l", "range");
                this.b = cfg.getDouble(1, "width", "w");
            }
            default -> {
                this.a = cfg.getDouble(3, "inner", "min");
                this.b = cfg.getDouble(6, "outer", "max", "radius", "r");
            }
        }
        this.filter = TargetFilter.read(cfg, TargetFilter.Mode.ENEMIES);
    }

    @Override
    public SkillTargets resolve(SkillMetadata meta) {
        Entity caster = meta.caster();
        double scale = meta.rangeMultiplier();
        Location from = shape == Shape.RING ? meta.origin()
                : caster instanceof LivingEntity le ? le.getEyeLocation() : caster.getLocation();
        Vector dir = from.getDirection().normalize();
        double reach = (shape == Shape.RING ? b : a) * scale + 2;

        List<Entity> candidates = from.getWorld().getNearbyEntities(from, reach, reach, reach).stream()
                .filter(e -> inside(e.getBoundingBox(), from, dir, scale))
                .toList();
        SkillTargets targets = new SkillTargets();
        filter.apply(caster, from, candidates).forEach(targets::addEntity);
        return targets;
    }

    private boolean inside(BoundingBox box, Location from, Vector dir, double scale) {
        Vector origin = from.toVector();
        Vector center = box.getCenter();
        switch (shape) {
            case CONE -> {
                Vector closest = closestPoint(box, origin);
                Vector to = closest.clone().subtract(origin);
                double dist = to.length();
                if (dist > a * scale) return false;
                if (dist < 0.75) return true; // touching the caster
                // Test the closest point and the centre, so wide mobs at the edge still count.
                Vector toCenter = center.clone().subtract(origin);
                return to.normalize().dot(dir) >= b
                        || (toCenter.lengthSquared() > 0 && toCenter.normalize().dot(dir) >= b);
            }
            case LINE -> {
                double along = center.clone().subtract(origin).dot(dir);
                if (along < 0 || along > a * scale) return false;
                Vector point = origin.clone().add(dir.clone().multiply(along));
                double half = Math.max(box.getWidthX(), box.getWidthZ()) / 2;
                return closestPoint(box, point).distance(point) <= b * scale / 2 + 0.01
                        || center.distance(point) <= b * scale / 2 + half;
            }
            default -> {
                double d = closestPoint(box, origin).distance(origin);
                double far = center.distance(origin);
                return far >= a * scale && d <= b * scale;
            }
        }
    }

    private static Vector closestPoint(BoundingBox box, Vector p) {
        return new Vector(
                Math.max(box.getMinX(), Math.min(p.getX(), box.getMaxX())),
                Math.max(box.getMinY(), Math.min(p.getY(), box.getMaxY())),
                Math.max(box.getMinZ(), Math.min(p.getZ(), box.getMaxZ())));
    }
}

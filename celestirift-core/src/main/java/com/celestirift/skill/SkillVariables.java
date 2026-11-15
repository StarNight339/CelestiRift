package com.celestirift.skill;

import com.celestirift.CelestiRift;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.Variables;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Numeric variables for expressions: {@code <caster.level>}, {@code <target.hp>}, ...
 * Prefixes: caster, target, trigger. A value that doesn't apply (a mob's level) is 0.
 */
public final class SkillVariables implements Variables.Resolver {

    public static final List<String> PREFIXES = List.of("caster", "target", "trigger");
    public static final List<String> FIELDS = List.of("hp", "mhp", "maxhp", "hppct", "level", "mana", "maxmana",
            "armor", "distance", "l.x", "l.y", "l.z");
    private static final Set<String> GLOBAL = Set.of("power", "range");

    private final CelestiRift plugin;

    public SkillVariables(CelestiRift plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isKnown(String name) {
        if (GLOBAL.contains(name)) {
            return true;
        }
        int dot = name.indexOf('.');
        if (dot < 0) {
            return false;
        }
        String prefix = name.substring(0, dot);
        String field = name.substring(dot + 1);
        return PREFIXES.contains(prefix) && (FIELDS.contains(field) || field.startsWith("aura."));
    }

    @Override
    public double resolve(String name, SkillMetadata meta, @Nullable Entity target) {
        switch (name) {
            case "power": return meta.power();
            case "range": return meta.rangeMultiplier();
            default: break;
        }
        int dot = name.indexOf('.');
        if (dot < 0) {
            return 0;
        }
        Entity e = switch (name.substring(0, dot)) {
            case "caster" -> meta.caster();
            case "target" -> target;
            case "trigger" -> meta.trigger();
            default -> null;
        };
        if (e == null) {
            return 0;
        }
        String field = name.substring(dot + 1);
        if (field.startsWith("aura.")) {
            return plugin.auras().variable(e, field.substring(5)); // <target.aura.mark[.stacks|.time|.shield]>
        }
        return switch (field) {
            case "hp" -> e instanceof LivingEntity le ? le.getHealth() : 0;
            case "mhp", "maxhp" -> maxHealth(e);
            case "hppct" -> {
                double max = maxHealth(e);
                yield max <= 0 ? 0 : (e instanceof LivingEntity le ? le.getHealth() / max * 100 : 0);
            }
            case "level" -> e instanceof Player p ? plugin.profiles().level(p) : 0;
            case "mana" -> e instanceof Player p ? plugin.mana().get(p.getUniqueId()) : 0;
            case "maxmana" -> e instanceof Player p ? plugin.mana().max(p.getUniqueId()) : 0;
            case "armor" -> attribute(e, Attribute.ARMOR);
            case "distance" -> meta.caster().getWorld().equals(e.getWorld())
                    ? meta.caster().getLocation().distance(e.getLocation()) : 0;
            case "l.x" -> e.getLocation().getX();
            case "l.y" -> e.getLocation().getY();
            case "l.z" -> e.getLocation().getZ();
            default -> 0;
        };
    }

    private static double maxHealth(Entity e) {
        return attribute(e, Attribute.MAX_HEALTH);
    }

    private static double attribute(Entity e, Attribute attribute) {
        if (e instanceof LivingEntity le) {
            var a = le.getAttribute(attribute);
            return a != null ? a.getValue() : 0;
        }
        return 0;
    }
}

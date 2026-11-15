package com.celestirift.skill;

import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.SkillTargets;
import com.celestirift.api.skill.targeter.Targeter;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves {@code <...>} placeholders inside mechanic attributes/messages.
 * Supported: {@code <caster.name>}, {@code <caster.hp>}, {@code <caster.uuid>},
 * {@code <caster.l.x|y|z|world>}, and the same for {@code <trigger...>} and
 * {@code <target...>}. Unknown placeholders are left untouched.
 */
public final class Placeholders {

    private Placeholders() {}

    /** Supplies {@code <x.level>} and {@code <x.class>} for players (set by the plugin). */
    public interface PlayerInfo {
        int level(org.bukkit.entity.Player player);

        String className(org.bukkit.entity.Player player);
    }

    private static @Nullable PlayerInfo playerInfo;

    public static void playerInfo(@Nullable PlayerInfo info) {
        playerInfo = info;
    }

    public static String apply(String input, SkillMetadata meta, @Nullable Entity target) {
        if (input == null || input.indexOf('<') < 0) {
            return input;
        }
        String out = input;
        out = replaceEntity(out, "caster", meta.caster());
        out = replaceEntity(out, "trigger", meta.trigger());
        out = replaceEntity(out, "target", target);
        return out;
    }

    private static String replaceEntity(String input, String prefix, @Nullable Entity e) {
        if (e == null) {
            return input;
        }
        String p = "<" + prefix + ".";
        if (!input.contains(p)) {
            return input;
        }
        Location l = e.getLocation();
        String out = input;
        out = out.replace(p + "name>", plainName(e));
        out = out.replace(p + "uuid>", e.getUniqueId().toString());
        out = out.replace(p + "type>", e.getType().name());
        out = out.replace(p + "l.x>", fmt(l.getX()));
        out = out.replace(p + "l.y>", fmt(l.getY()));
        out = out.replace(p + "l.z>", fmt(l.getZ()));
        out = out.replace(p + "l.world>", l.getWorld() != null ? l.getWorld().getName() : "");
        if (e instanceof org.bukkit.entity.Player player && playerInfo != null) {
            out = out.replace(p + "level>", String.valueOf(playerInfo.level(player)));
            out = out.replace(p + "class>", playerInfo.className(player));
        }
        if (e instanceof LivingEntity le) {
            out = out.replace(p + "hp>", fmt(le.getHealth()));
            out = out.replace(p + "mhp>",
                    fmt(le.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                            ? le.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()
                            : le.getHealth()));
        }
        return out;
    }

    private static String plainName(Entity e) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(e.name());
    }

    private static String fmt(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return String.valueOf((long) d);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", d);
    }
}

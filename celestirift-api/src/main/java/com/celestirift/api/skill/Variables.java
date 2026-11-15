package com.celestirift.api.skill;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Numeric variables usable in expressions ({@code <caster.level>}, {@code <target.hp>}, ...).
 * CelestiRift installs the resolver; add-ons normally only read values through
 * {@link NumberValue}.
 */
public final class Variables {

    /** Resolves variable names such as {@code caster.level} (lowercase, no brackets). */
    public interface Resolver {
        /**
         * @param name   variable name
         * @param meta   the running cast
         * @param target current target, if any
         * @return the value, or 0 when it does not apply (e.g. level of a mob)
         */
        double resolve(String name, SkillMetadata meta, @Nullable Entity target);

        /**
         * @param name variable name
         * @return true if the name is a known variable (used to flag typos)
         */
        boolean isKnown(String name);
    }

    private static @Nullable Resolver resolver;

    private Variables() {}

    /**
     * @param r the resolver, or null to remove it
     */
    public static void resolver(@Nullable Resolver r) {
        resolver = r;
    }

    /**
     * @param name   variable name
     * @param meta   the running cast
     * @param target current target, if any
     * @return the value (0 when no resolver is installed)
     */
    public static double resolve(String name, SkillMetadata meta, @Nullable Entity target) {
        Resolver r = resolver;
        return r == null ? 0 : r.resolve(name, meta, target);
    }

    /**
     * @param name variable name
     * @return true if known (always true when no resolver is installed)
     */
    public static boolean isKnown(String name) {
        Resolver r = resolver;
        return r == null || r.isKnown(name);
    }
}

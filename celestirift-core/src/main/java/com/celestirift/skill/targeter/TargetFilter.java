package com.celestirift.skill.targeter;

import com.celestirift.api.skill.ConfigMap;
import com.celestirift.skill.CombatRules;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Shared {@code filter / limit / sort} options for entity targeters:
 * <pre>
 * target: { nearby: { radius: 6, filter: enemies, limit: 3, sort: nearest } }
 * </pre>
 * filter: enemies (default for area targeters), allies, players, mobs, all.
 * Armor stands, NPCs, creative players and invulnerable entities are never targeted.
 */
public final class TargetFilter {

    public enum Mode { ENEMIES, ALLIES, PLAYERS, MOBS, ALL }

    public enum Sort { NONE, NEAREST, FURTHEST, RANDOM, LOWESTHP }

    public static final List<String> MODES = List.of("enemies", "allies", "players", "mobs", "all");
    public static final List<String> SORTS = List.of("none", "nearest", "furthest", "random", "lowesthp");

    private final Mode mode;
    private final int limit;
    private final Sort sort;
    private final boolean ignoreCaster;

    TargetFilter(Mode mode, int limit, Sort sort, boolean ignoreCaster) {
        this.mode = mode;
        this.limit = limit;
        this.sort = sort;
        this.ignoreCaster = ignoreCaster;
    }

    /** Read filter options; unknown values fall back to the default (and are reported). */
    public static TargetFilter read(ConfigMap cfg, Mode defaultMode) {
        String f = cfg.getChoice(defaultMode.name().toLowerCase(Locale.ROOT), MODES, "filter", "f");
        int limit = Math.max(0, cfg.getInt(0, "limit", "max"));
        String s = cfg.getChoice("none", SORTS, "sort");
        boolean ignoreCaster = cfg.getBoolean(true, "ignorecaster", "ic");
        // Old option: players=true meant "only players".
        boolean playersOnly = cfg.getBoolean(false, "players", "playersonly");
        Mode mode = parse(f, Mode.class, defaultMode);
        if (playersOnly) {
            mode = Mode.PLAYERS;
        }
        return new TargetFilter(mode, limit, parse(s, Sort.class, Sort.NONE), ignoreCaster);
    }

    private static <E extends Enum<E>> E parse(String v, Class<E> type, E def) {
        try {
            return Enum.valueOf(type, v.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return def;
        }
    }

    public Mode mode() {
        return mode;
    }

    /** Does {@code e} pass the filter for this caster? */
    public boolean test(Entity caster, Entity e) {
        if (e.equals(caster)) {
            return !ignoreCaster && mode != Mode.ENEMIES;
        }
        if (!(e instanceof LivingEntity) || CombatRules.isProtected(e)) {
            return false;
        }
        return switch (mode) {
            case ENEMIES -> CombatRules.isEnemy(caster, e);
            case ALLIES -> CombatRules.isAlly(caster, e);
            case PLAYERS -> e instanceof Player;
            case MOBS -> e instanceof Mob;
            case ALL -> true;
        };
    }

    /** Filter, sort (by distance to {@code origin}) and limit. */
    public List<Entity> apply(Entity caster, Location origin, Collection<? extends Entity> candidates) {
        List<Entity> out = new ArrayList<>();
        for (Entity e : candidates) {
            if (test(caster, e)) {
                out.add(e);
            }
        }
        switch (sort) {
            case NEAREST -> out.sort(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(origin)));
            case FURTHEST -> out.sort(Comparator.comparingDouble((Entity e) -> e.getLocation().distanceSquared(origin))
                    .reversed());
            case RANDOM -> Collections.shuffle(out, ThreadLocalRandom.current());
            case LOWESTHP -> out.sort(Comparator.comparingDouble(e -> ((LivingEntity) e).getHealth()));
            case NONE -> { }
        }
        if (limit > 0 && out.size() > limit) {
            return new ArrayList<>(out.subList(0, limit));
        }
        return out;
    }
}

package com.celestirift.skill;

import com.celestirift.CelestiRift;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Who a skill may hurt. Used by targeter filters ({@code filter: enemies}) and, as a
 * last safety net, by harmful mechanics (damage, ignite, projectile hits), so a
 * public server's AoE never hits pets, armor stands, creative players or - with
 * {@code combat.pvp: false} - other players.
 *
 * <p>Actual damage is dealt through normal damage events, so region plugins
 * (WorldGuard {@code pvp deny}, claims) still cancel it.
 */
public final class CombatRules {

    private static @Nullable CelestiRift plugin;

    private CombatRules() {}

    public static void install(@Nullable CelestiRift p) {
        plugin = p;
    }

    public static boolean pvp() {
        return plugin == null || plugin.getConfig().getBoolean("combat.pvp", true);
    }

    public static double pvpMultiplier() {
        return plugin == null ? 1.0 : plugin.getConfig().getDouble("combat.pvp-multiplier", 1.0);
    }

    /** Never a target of anything: armor stands, invulnerable, creative/spectator players, NPCs, dead. */
    public static boolean isProtected(Entity e) {
        if (!(e instanceof LivingEntity le) || le.isDead() || e instanceof ArmorStand || e.isMarkedInvulnerable()) {
            return true;
        }
        if (e.hasMetadata("NPC")) { // Citizens and similar NPC plugins
            return true;
        }
        if (e instanceof org.bukkit.entity.Mannequin && dummy(e) == null) {
            return true; // decorative mannequins; training dummies have a role
        }
        return e instanceof Player p && (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR);
    }

    private static com.celestirift.devtools.TrainingDummies.@Nullable Role dummy(Entity e) {
        return plugin != null && plugin.dummies() != null ? plugin.dummies().role(e) : null;
    }

    /** UUID of the player/entity that owns this pet or summon, if any. */
    public static @Nullable UUID owner(Entity e) {
        if (e instanceof Tameable t && t.isTamed() && t.getOwnerUniqueId() != null) {
            return t.getOwnerUniqueId();
        }
        return plugin != null ? plugin.summons().owner(e) : null;
    }

    /** The entity whose side {@code caster} fights on: a summon fights for its owner. */
    private static Entity side(Entity caster) {
        UUID owner = owner(caster);
        if (owner != null) {
            Entity o = Bukkit.getEntity(owner);
            if (o != null) {
                return o;
            }
        }
        return caster;
    }

    /** Is {@code e} on the caster's team (itself, its pets and summons, and owner's)? */
    public static boolean sameTeam(Entity caster, Entity e) {
        Entity side = side(caster);
        if (e.equals(caster) || e.equals(side)) {
            return true;
        }
        UUID owner = owner(e);
        return owner != null && (owner.equals(side.getUniqueId()) || owner.equals(caster.getUniqueId()));
    }

    /** Should an AoE/`filter: enemies` skill from {@code caster} hit {@code e}? */
    public static boolean isEnemy(Entity caster, Entity e) {
        if (isProtected(e) || sameTeam(caster, e)) {
            return false;
        }
        Entity side = side(caster);
        var dummy = dummy(e);
        if (dummy != null) {
            return switch (dummy) {
                case ENEMY -> true;
                case ALLY -> false;
                case PLAYER -> !(side instanceof Player) || pvp();
            };
        }
        if (side instanceof Player) {
            if (e instanceof Player) {
                return pvp();
            }
            if (owner(e) != null) {
                return pvp(); // someone else's pet or summon
            }
            return e instanceof Enemy || (e instanceof Mob m && side.equals(m.getTarget()));
        }
        // A mob or boss: fights players, their pets, and anything attacking it.
        if (e instanceof Player) {
            return true;
        }
        UUID owner = owner(e);
        if (owner != null && Bukkit.getEntity(owner) instanceof Player) {
            return true;
        }
        return e instanceof Mob m && (side.equals(m.getTarget()) || caster.equals(m.getTarget()));
    }

    /** Should a supportive skill (`filter: allies`) from {@code caster} affect {@code e}? */
    public static boolean isAlly(Entity caster, Entity e) {
        if (isProtected(e)) {
            return false;
        }
        if (sameTeam(caster, e)) {
            return true;
        }
        Entity side = side(caster);
        var dummy = dummy(e);
        if (dummy != null) {
            return side instanceof Player && (dummy == com.celestirift.devtools.TrainingDummies.Role.ALLY
                    || (dummy == com.celestirift.devtools.TrainingDummies.Role.PLAYER && !pvp()));
        }
        if (side instanceof Player) {
            return e instanceof Player && !pvp(); // with PvP on, other players are fair game
        }
        return !(e instanceof Player) && !isEnemy(caster, e) && e instanceof Enemy;
    }

    /**
     * Last check before a harmful mechanic touches {@code victim}: own team, protected
     * entities and (with PvP off) players and their pets are never hurt. Explicitly
     * aimed skills on neutral mobs are allowed.
     */
    public static boolean canHarm(@Nullable Entity caster, Entity victim) {
        if (isProtected(victim)) {
            return false;
        }
        if (caster == null) {
            return true;
        }
        if (sameTeam(caster, victim)) {
            return false;
        }
        var dummy = dummy(victim);
        if (dummy != null && side(caster) instanceof Player) {
            return dummy == com.celestirift.devtools.TrainingDummies.Role.ENEMY
                    || (dummy == com.celestirift.devtools.TrainingDummies.Role.PLAYER && pvp());
        }
        if (side(caster) instanceof Player && !pvp()) {
            return !(victim instanceof Player) && owner(victim) == null;
        }
        return true;
    }

    /** Damage multiplier for this pair (PvP scaling). */
    public static double damageMultiplier(@Nullable Entity caster, Entity victim) {
        if (caster != null && side(caster) instanceof Player && (victim instanceof Player
                || dummy(victim) == com.celestirift.devtools.TrainingDummies.Role.PLAYER)) {
            return pvpMultiplier();
        }
        return 1.0;
    }
}

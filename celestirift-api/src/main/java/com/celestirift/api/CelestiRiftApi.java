package com.celestirift.api;

import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.condition.Condition;
import com.celestirift.api.skill.mechanic.Mechanic;
import com.celestirift.api.skill.targeter.Targeter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Entry point for other plugins. Obtain it with {@link #get()} (or through Bukkit's
 * {@code ServicesManager}) after CelestiRift has enabled — declare
 * {@code depend: [CelestiRift]} or {@code softdepend} in your plugin.yml.
 *
 * <h2>Adding a mechanic</h2>
 * <pre>{@code
 * public final class FreezeMechanic implements TargetedEntityMechanic {
 *     private final int ticks;
 *     public FreezeMechanic(ConfigMap cfg) { this.ticks = cfg.getInt(60, "ticks", "t"); }
 *     public void cast(SkillMetadata meta, Entity target) { target.setFreezeTicks(ticks); }
 * }
 *
 * // in onEnable():
 * CelestiRiftApi.get().registerMechanic(FreezeMechanic::new, "freeze");
 * }</pre>
 * Skills are first compiled one tick after the server finishes enabling plugins, so
 * anything registered in your {@code onEnable} is available to YAML skills. Registering
 * later requires {@link #reloadSkills()}.
 */
public interface CelestiRiftApi {

    /**
     * @return the running API instance
     * @throws IllegalStateException if CelestiRift is not enabled
     */
    static CelestiRiftApi get() {
        CelestiRiftApi api = Bukkit.getServicesManager().load(CelestiRiftApi.class);
        if (api == null) {
            throw new IllegalStateException("CelestiRift is not enabled");
        }
        return api;
    }

    // ------------------------------------------------------------------ registries

    /**
     * Register a mechanic usable in YAML as {@code name{...}}.
     *
     * @param factory creates a mechanic from its parsed {@code {...}} attributes
     * @param names   name plus optional aliases (case-insensitive)
     */
    void registerMechanic(Function<ConfigMap, Mechanic> factory, String... names);

    /**
     * Register a targeter usable in YAML as {@code @name{...}}.
     *
     * @param factory creates a targeter from its parsed attributes
     * @param names   name plus optional aliases (case-insensitive)
     */
    void registerTargeter(Function<ConfigMap, Targeter> factory, String... names);

    /**
     * Register a condition usable in YAML as {@code ?name{...}}.
     *
     * @param factory creates a condition from its parsed attributes
     * @param names   name plus optional aliases (case-insensitive)
     */
    void registerCondition(Function<ConfigMap, Condition> factory, String... names);

    // ------------------------------------------------------------------ skills

    /** @return names of all loaded skills */
    Set<String> skillNames();

    /**
     * @param name skill name (case-insensitive)
     * @return true if a skill with that name is loaded
     */
    boolean hasSkill(String name);

    /**
     * Cast a skill as a player, enforcing conditions, cooldown and mana, and firing
     * {@link com.celestirift.api.event.SkillPreCastEvent} / {@link com.celestirift.api.event.SkillCastEvent}.
     *
     * @param player  the caster
     * @param skill   skill name
     * @param trigger entity exposed to {@code @trigger}, may be null
     * @return the outcome
     */
    CastResult cast(Player player, String skill, @Nullable Entity trigger);

    /**
     * Cast a skill for any entity with no cooldown/mana (mobs, bosses, scripted casts).
     * Skill-level conditions still apply.
     *
     * @param skill skill name
     * @param meta  caster, trigger, origin and power
     * @return true if the skill ran
     */
    boolean castRaw(String skill, SkillMetadata meta);

    /** Recompile all skills from disk (use after registering mechanics late). */
    void reloadSkills();

    // ------------------------------------------------------------------ mana & cooldowns

    /**
     * @param player player id
     * @return current mana
     */
    double getMana(UUID player);

    /**
     * Set a player's mana (clamped to 0..max). Fires {@link com.celestirift.api.event.ManaChangeEvent}.
     *
     * @param player player id
     * @param amount new mana
     */
    void setMana(UUID player, double amount);

    /**
     * @param player player id
     * @return the player's maximum mana
     */
    double getMaxMana(UUID player);

    /**
     * @param player player id
     * @param skill  skill name
     * @return remaining cooldown in seconds (0 when ready)
     */
    double getCooldown(UUID player, String skill);

    // ------------------------------------------------------------------ RPG (level / class / bosses)

    /**
     * @param player an online player
     * @return the player's CelestiRift level (1+)
     */
    int getLevel(Player player);

    /**
     * @param player an online player
     * @return XP towards the next level
     */
    double getXp(Player player);

    /**
     * Give XP (fires {@link com.celestirift.api.event.PlayerXpGainEvent} with source API and
     * {@link com.celestirift.api.event.PlayerLevelUpEvent} per level gained).
     *
     * @param player an online player
     * @param amount XP to add
     */
    void giveXp(Player player, double amount);

    /**
     * @param player an online player
     * @return the player's class id, or null if they have none
     */
    @Nullable String getPlayerClass(Player player);

    /**
     * Set or clear a player's class (fires {@link com.celestirift.api.event.PlayerClassChangeEvent}).
     *
     * @param player  an online player
     * @param classId a classes.yml id, or null to remove the class
     * @return true if the class changed
     */
    boolean setPlayerClass(Player player, @Nullable String classId);

    /** @return ids of all classes in classes.yml */
    Set<String> classIds();

    /**
     * Spawn a boss from bosses.yml.
     *
     * @param bossId   bosses.yml id
     * @param location where to spawn
     * @return the boss entity, or null if the id is unknown
     */
    org.bukkit.entity.@Nullable LivingEntity spawnBoss(String bossId, org.bukkit.Location location);

    // ------------------------------------------------------------------ auras (API 2.2)

    /**
     * @param entity the holder
     * @param auraId aura id (case-insensitive), as in {@code aura{id=Mark}}
     * @return current stacks, 0 if the entity doesn't have it
     */
    int auraStacks(org.bukkit.entity.Entity entity, String auraId);

    /**
     * Remove an aura without running its onend skill.
     *
     * @param entity the holder
     * @param auraId aura id (case-insensitive)
     * @return true if it was removed
     */
    boolean removeAura(org.bukkit.entity.Entity entity, String auraId);
}

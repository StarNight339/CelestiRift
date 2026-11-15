package com.celestirift;

import com.celestirift.api.CastResult;
import com.celestirift.api.CelestiRiftApi;
import com.celestirift.api.skill.ConfigMap;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.condition.Condition;
import com.celestirift.api.skill.mechanic.Mechanic;
import com.celestirift.api.skill.targeter.Targeter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/** Core implementation of the public API, registered with Bukkit's ServicesManager. */
final class ApiImpl implements CelestiRiftApi {

    private final CelestiRift plugin;

    ApiImpl(CelestiRift plugin) {
        this.plugin = plugin;
    }

    @Override
    public void registerMechanic(Function<ConfigMap, Mechanic> factory, String... names) {
        plugin.skills().mechanics().register(factory, names);
    }

    @Override
    public void registerTargeter(Function<ConfigMap, Targeter> factory, String... names) {
        plugin.skills().targeters().register(factory, names);
    }

    @Override
    public void registerCondition(Function<ConfigMap, Condition> factory, String... names) {
        plugin.skills().conditions().register(factory, names);
    }

    @Override
    public Set<String> skillNames() {
        return plugin.skills().skillNames();
    }

    @Override
    public boolean hasSkill(String name) {
        return plugin.skills().getSkill(name).isPresent();
    }

    @Override
    public CastResult cast(Player player, String skill, @Nullable Entity trigger) {
        return plugin.casts().cast(player, skill, trigger);
    }

    @Override
    public boolean castRaw(String skill, SkillMetadata meta) {
        return plugin.skills().cast(skill, meta);
    }

    @Override
    public void reloadSkills() {
        plugin.skills().reload();
    }

    @Override
    public double getMana(UUID player) {
        return plugin.mana().get(player);
    }

    @Override
    public void setMana(UUID player, double amount) {
        plugin.mana().set(player, amount);
    }

    @Override
    public double getMaxMana(UUID player) {
        return plugin.mana().max(player);
    }

    @Override
    public double getCooldown(UUID player, String skill) {
        return plugin.cooldowns().remaining(player, skill);
    }

    @Override
    public int getLevel(Player player) {
        return plugin.profiles().level(player);
    }

    @Override
    public double getXp(Player player) {
        return plugin.profiles().get(player).xp();
    }

    @Override
    public void giveXp(Player player, double amount) {
        plugin.profiles().addXp(player, amount, com.celestirift.api.event.PlayerXpGainEvent.Source.API);
    }

    @Override
    public @Nullable String getPlayerClass(Player player) {
        return plugin.profiles().get(player).classId();
    }

    @Override
    public boolean setPlayerClass(Player player, @Nullable String classId) {
        return plugin.profiles().setClass(player, classId, false);
    }

    @Override
    public Set<String> classIds() {
        return plugin.classes().ids();
    }

    @Override
    public org.bukkit.entity.@Nullable LivingEntity spawnBoss(String bossId, org.bukkit.Location location) {
        return plugin.bosses().spawn(bossId, location);
    }

    @Override
    public int auraStacks(org.bukkit.entity.Entity entity, String auraId) {
        return plugin.auras().stacks(entity, auraId);
    }

    @Override
    public boolean removeAura(org.bukkit.entity.Entity entity, String auraId) {
        return plugin.auras().remove(entity, auraId, null, 0, false) > 0;
    }
}

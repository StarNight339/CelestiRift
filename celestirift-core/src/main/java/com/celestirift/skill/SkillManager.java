package com.celestirift.skill;

import com.celestirift.CelestiRift;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.api.skill.condition.Condition;
import com.celestirift.api.skill.mechanic.Mechanic;
import com.celestirift.api.skill.targeter.Targeter;
import com.celestirift.skill.condition.impl.DistanceCondition;
import com.celestirift.skill.condition.impl.HealthCondition;
import com.celestirift.skill.condition.impl.PermissionCondition;
import com.celestirift.skill.config.StructuredLine;
import com.celestirift.skill.load.SkillLoader;
import com.celestirift.skill.mechanic.impl.*;
import com.celestirift.skill.registry.ComponentRegistry;
import com.celestirift.skill.targeter.*;
import com.celestirift.validate.Diagnostics;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Owns the mechanic/targeter/condition registries and the compiled skills, and
 * runs casts. Loading/validation of {@code skills/**.yml} lives in {@link SkillLoader}.
 */
public final class SkillManager implements com.celestirift.util.Restorable {

    private final CelestiRift plugin;
    private final ComponentRegistry<Mechanic> mechanics = new ComponentRegistry<>("mechanic");
    private final ComponentRegistry<Targeter> targeters = new ComponentRegistry<>("targeter");
    private final ComponentRegistry<Condition> conditions = new ComponentRegistry<>("condition");
    private final SkillExecutor executor;
    private Map<String, Skill> skills = new HashMap<>();

    /** Shorthand-attribute lookup used by the structured YAML form. */
    private final StructuredLine.Primaries primaries = new StructuredLine.Primaries() {
        @Override public @Nullable String mechanic(String name) { return mechanics.primary(name); }
        @Override public @Nullable String targeter(String name) { return targeters.primary(name); }
        @Override public @Nullable String condition(String name) { return conditions.primary(name); }
    };

    public SkillManager(CelestiRift plugin) {
        this.plugin = plugin;
        this.executor = new SkillExecutor(plugin);
        registerDefaults();
    }

    // ------------------------------------------------------------------ registries

    public ComponentRegistry<Mechanic> mechanics() { return mechanics; }
    public ComponentRegistry<Targeter> targeters() { return targeters; }
    public ComponentRegistry<Condition> conditions() { return conditions; }
    public StructuredLine.Primaries primaries() { return primaries; }

    private void registerDefaults() {
        // Mechanics
        mechanics.register(MessageMechanic::new, "message", "msg");
        mechanics.register(DamageMechanic::new, "damage", "dmg");
        mechanics.register(HealMechanic::new, "heal");
        mechanics.register(PotionMechanic::new, "potion", "effect");
        mechanics.register(IgniteMechanic::new, "ignite", "burn");
        mechanics.register(PushMechanic::new, "push", "knockback", "velocity");
        mechanics.register(LeapMechanic::new, "leap", "jump");
        mechanics.register(SoundMechanic::new, "sound");
        mechanics.register(ParticleMechanic::new, "particle", "particles");
        mechanics.register(LightningMechanic::new, "lightning", "strike");
        mechanics.register(TeleportMechanic::new, "teleport", "tp");
        mechanics.register(ProjectileMechanic::new, "projectile", "shoot");
        mechanics.register(DelayMechanic::new, "delay", "wait");
        mechanics.register(cfg -> new MetaSkillMechanic(plugin, cfg), "skill", "cast", "meta");
        mechanics.register(cfg -> new FlyMechanic(plugin, cfg), "fly", "flight");
        mechanics.register(cfg -> new SummonMechanic(plugin, cfg), "summon", "spawn", "minion");
        mechanics.register(StealMechanic::new, "stealitem", "steal", "disarm");
        mechanics.register(cfg -> new AuraMechanic(plugin, cfg), "aura", "buff", "status");
        mechanics.register(cfg -> new RemoveAuraMechanic(plugin, cfg), "removeaura", "cleanse", "dispel");
        mechanics.register(cfg -> new TelegraphMechanic(plugin, cfg), "telegraph", "warning");
        mechanics.register(cfg -> new DisplayMechanic(plugin, cfg), "display", "model", "hologram");
        mechanics.register(cfg -> new MeteorMechanic(plugin, cfg), "meteor");
        mechanics.register(cfg -> new GiveItemMechanic(plugin, cfg), "giveitem", "orionitem");
        mechanics.register(cfg -> new ChainMechanic(plugin, cfg), "chain", "chainlightning");

        // VFX shape mechanics
        mechanics.register(CircleMechanic::new, "circle", "ring");
        mechanics.register(SphereMechanic::new, "sphere");
        mechanics.register(HelixMechanic::new, "helix");
        mechanics.register(LineMechanic::new, "line", "beam");
        mechanics.register(cfg -> new SlashMechanic(plugin, cfg), "slash", "airslash");
        mechanics.register(cfg -> new BlackholeMechanic(plugin, cfg), "blackhole", "vortex");
        mechanics.register(cfg -> new TornadoMechanic(plugin, cfg), "tornado", "twister");
        mechanics.register(cfg -> new ShockwaveMechanic(plugin, cfg), "shockwave", "nova");
        mechanics.register(StarMechanic::new, "star");
        mechanics.register(cfg -> new ProjectileTrailMechanic(plugin, cfg), "projectiletrail", "trail");

        // Targeters
        targeters.register(cfg -> new SelfTargeter(), "self", "caster");
        targeters.register(cfg -> new TriggerTargeter(), "trigger");
        targeters.register(TargetTargeter::new, "target", "t");
        targeters.register(NearbyEntitiesTargeter::new, "nearbyentities", "nearby", "eir", "entitiesinradius");
        targeters.register(cfg -> new CasterLocationTargeter(), "casterlocation", "cl");
        targeters.register(cfg -> new OriginTargeter(), "origin", "o");
        targeters.register(TargetLocationTargeter::new, "targetlocation", "tl");
        targeters.register(cfg -> new ShapeTargeter(ShapeTargeter.Shape.CONE, cfg), "cone");
        targeters.register(cfg -> new ShapeTargeter(ShapeTargeter.Shape.LINE, cfg), "line", "ray", "strip");
        targeters.register(cfg -> new ShapeTargeter(ShapeTargeter.Shape.RING, cfg), "ring", "donut");

        // Conditions
        conditions.register(HealthCondition::new, "health", "hp");
        conditions.register(PermissionCondition::new, "permission", "perm");
        conditions.register(DistanceCondition::new, "distance", "dist");
        conditions.register(cfg -> new com.celestirift.skill.condition.impl.LevelCondition(plugin, cfg), "level", "lvl");
        conditions.register(cfg -> new com.celestirift.skill.condition.impl.ClassCondition(plugin, cfg), "class", "playerclass");
        conditions.register(cfg -> new com.celestirift.skill.condition.impl.HasAuraCondition(plugin, cfg), "hasaura", "aura");
    }

    // ------------------------------------------------------------------ loading

    @Override
    public Runnable snapshot() {
        Map<String, Skill> old = skills;
        return () -> skills = old;
    }

    /** (Re)load all skills from disk, reporting problems. Returns number of skills loaded. */
    public int reload(Diagnostics diagnostics) {
        Map<String, Skill> loaded = new SkillLoader(plugin, this, diagnostics).load();
        this.skills = loaded; // swap only once fully compiled
        return loaded.size();
    }

    /** Reload with problems only logged to the console. */
    public int reload() {
        Diagnostics d = new Diagnostics();
        int n = reload(d);
        d.log(plugin.getLogger());
        return n;
    }

    // ------------------------------------------------------------------ casting

    public Optional<Skill> getSkill(String name) {
        return Optional.ofNullable(skills.get(name.toLowerCase(Locale.ROOT)));
    }

    public Set<String> skillNames() {
        Set<String> out = new TreeSet<>();
        for (Skill s : skills.values()) {
            out.add(s.name());
        }
        return out;
    }

    /**
     * Evaluate a skill's skill-level {@code Conditions:} against the caster.
     * Returns true if the skill is allowed to cast (or has no conditions).
     */
    public boolean canCast(Skill skill, SkillMetadata meta) {
        for (Skill.CompiledCondition cc : skill.conditions()) {
            boolean result = cc.condition().test(meta, meta.trigger());
            if (cc.negated()) {
                result = !result;
            }
            if (!result) {
                return false;
            }
        }
        return true;
    }

    /**
     * Cast a skill by name (no cooldown/mana check, but skill-level conditions
     * are enforced). Returns false if the skill is unknown or its conditions fail.
     * A depth guard prevents self-referencing meta skills ({@code skill{s=Self}})
     * from recursing into a StackOverflow.
     */
    public boolean cast(String name, SkillMetadata meta) {
        Skill skill = skills.get(name.toLowerCase(Locale.ROOT));
        if (skill == null || !canCast(skill, meta)) {
            return false;
        }
        if (castDepth >= MAX_CAST_DEPTH) {
            plugin.getLogger().warning("Skill '" + skill.name() + "' aborted: cast depth "
                    + MAX_CAST_DEPTH + " reached (self-referencing meta skill?).");
            return false;
        }
        castDepth++;
        try {
            executor.execute(skill, meta);
        } finally {
            castDepth--;
        }
        return true;
    }

    /** Skill lines that threw an exception since startup. */
    public int lineFailures() {
        return executor.failures();
    }

    /** Run a skill ignoring its skill-level conditions (class/level gates). For tests and admin tools. */
    public boolean forceCast(String name, SkillMetadata meta) {
        Skill skill = skills.get(name.toLowerCase(Locale.ROOT));
        if (skill == null) {
            return false;
        }
        executor.execute(skill, meta);
        return true;
    }

    private static final int MAX_CAST_DEPTH = 64;
    private int castDepth = 0; // main-thread only
}

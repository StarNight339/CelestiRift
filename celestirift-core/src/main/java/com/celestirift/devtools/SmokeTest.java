package com.celestirift.devtools;

import com.celestirift.CelestiRift;
import com.celestirift.api.CelestiRiftApi;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.validate.Diagnostics;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * In-server self test, run by {@code ./gradlew smokeTest} (JVM flag
 * {@code -Dcelestirift.smokeTest=true}). On a throwaway Paper server it checks the
 * bundled content loads cleanly, casts every skill, spawns every boss, tests safe
 * reload, then writes {@code smoke-result.txt} and stops the server. Gradle fails the
 * build when the result contains a FAIL line.
 */
public final class SmokeTest {

    public static final String PROPERTY = "celestirift.smokeTest";
    public static final String RESULT_FILE = "smoke-result.txt";

    /** Ticks to let delayed skill lines, timers and boss phases run before judging. */
    private static final long SETTLE_TICKS = 200;

    private final CelestiRift plugin;
    private final List<String> lines = new ArrayList<>();
    private final List<String> severe = new ArrayList<>();
    private boolean failed;
    private Handler logHandler;
    private boolean paused; // the safe-reload check logs SEVERE on purpose

    public SmokeTest(CelestiRift plugin) {
        this.plugin = plugin;
    }

    public static boolean enabled() {
        return Boolean.getBoolean(PROPERTY);
    }

    /** Start watching the log now; the test itself runs once content is loaded. */
    public void schedule() {
        plugin.getConfig().set("settings.debug", true); // log dummy hits etc.
        logHandler = new Handler() {
            @Override
            public void publish(LogRecord r) {
                if (paused || r.getLevel().intValue() < Level.SEVERE.intValue()) {
                    return;
                }
                String src = String.valueOf(r.getLoggerName()) + " " + r.getMessage();
                boolean ours = src.contains("CelestiRift");
                for (Throwable t = r.getThrown(); t != null && !ours; t = t.getCause()) {
                    for (StackTraceElement el : t.getStackTrace()) {
                        if (el.getClassName().startsWith("com.celestirift")) {
                            ours = true;
                            break;
                        }
                    }
                }
                if (ours) {
                    severe.add(r.getMessage() + (r.getThrown() != null ? " (" + r.getThrown() + ")" : ""));
                }
            }

            @Override public void flush() {}
            @Override public void close() {}
        };
        Logger.getLogger("").addHandler(logHandler);
        // Content loads on tick 1; give the world a moment, then run.
        Bukkit.getScheduler().runTaskLater(plugin, this::runChecks, 40);
    }

    private void check(String name, boolean ok, String detail) {
        lines.add((ok ? "PASS " : "FAIL ") + name + (detail.isEmpty() ? "" : " - " + detail));
        failed |= !ok;
    }

    private void runChecks() {
        Diagnostics diag = plugin.diagnostics();
        long errors = diag.count(Diagnostics.Severity.ERROR);
        check("bundled content has no errors", errors == 0,
                errors == 0 ? diag.count(Diagnostics.Severity.WARNING) + " warning(s)"
                        : diag.entries().stream().filter(e -> e.severity() == Diagnostics.Severity.ERROR)
                        .map(Object::toString).toList().toString());
        check("skills loaded", !plugin.skills().skillNames().isEmpty(), plugin.skills().skillNames().size() + "");
        check("gems loaded", !plugin.gemDefs().ids().isEmpty(), plugin.gemDefs().ids().size() + "");
        check("classes loaded", !plugin.classes().ids().isEmpty(), plugin.classes().ids().size() + "");
        check("api service registered",
                Bukkit.getServicesManager().load(CelestiRiftApi.class) != null, "");
        check("resource pack extracted", Files.exists(plugin.resourcePack().bundledFile()),
                String.valueOf(plugin.resourcePack().bundledHash()));

        World world = Bukkit.getWorlds().getFirst();
        Location at = world.getSpawnLocation().toHighestLocation().add(0.5, 1, 0.5);
        // 26.x has no spawn chunks and nobody is online: keep the test area loaded and ticking.
        List<Chunk> chunks = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Chunk c = world.getChunkAt(at.getChunk().getX() + dx, at.getChunk().getZ() + dz);
                c.addPluginChunkTicket(plugin);
                chunks.add(c);
            }
        }

        // Every gem can be built into an item.
        int gemErrors = 0;
        for (String id : plugin.gemDefs().ids()) {
            try {
                plugin.gemDefs().get(id).map(plugin.gems()::create).orElseThrow();
            } catch (RuntimeException e) {
                gemErrors++;
                lines.add("     gem " + id + ": " + e);
            }
        }
        check("every gem builds an item", gemErrors == 0, gemErrors + " failed");

        // Cast every skill from a dummy mob caster.
        LivingEntity caster = (LivingEntity) world.spawnEntity(at, EntityType.ZOMBIE);
        if (caster instanceof Mob mob) {
            mob.setAware(false);
        }
        checkTargeting(world, at, caster);
        checkVariables(caster);
        checkDummies(world, at, caster);
        checkShapes(at, caster);
        checkAuras(at, caster);
        checkTelegraph(world, at);
        checkVfx(world, at, caster);
        caster.setInvulnerable(true); // after the targeting check: mobs won't target invulnerable entities

        int casts = 0;
        int castErrors = 0;
        for (String skill : plugin.skills().skillNames()) {
            try {
                plugin.skills().forceCast(skill, SkillMetadata.of(caster)); // ignore class/level gates
                casts++;
            } catch (RuntimeException e) {
                castErrors++;
                lines.add("     skill " + skill + ": " + e);
            }
        }
        check("cast every skill", castErrors == 0, casts + " cast, " + castErrors + " threw");

        // Spawn every boss.
        int spawned = 0;
        for (String id : plugin.bosses().ids()) {
            if (plugin.bosses().spawn(id, at.clone().add(4, 0, 0)) != null) {
                spawned++;
            } else {
                lines.add("     boss " + id + " did not spawn");
            }
        }
        check("spawn every boss", spawned == plugin.bosses().ids().size(),
                spawned + "/" + plugin.bosses().ids().size());

        Bukkit.getScheduler().runTaskLater(plugin, () -> finish(caster, chunks), SETTLE_TICKS);
    }

    /** AoE (filter: enemies) must skip bystanders, armor stands and the caster's own summons. */
    private void checkTargeting(World world, Location at, LivingEntity caster) {
        Location near = at.clone().add(2, 0, 0);
        var villager = world.spawnEntity(near, EntityType.VILLAGER);
        var stand = world.spawnEntity(near, EntityType.ARMOR_STAND);
        var minion = world.spawnEntity(near, EntityType.ZOMBIE);
        plugin.summons().setOwner(minion, caster.getUniqueId());
        var golem = (Mob) world.spawnEntity(near, EntityType.IRON_GOLEM);
        golem.setTarget(caster); // fighting the caster -> an enemy
        try {
            var targeter = new com.celestirift.skill.targeter.NearbyEntitiesTargeter(
                    com.celestirift.api.skill.ConfigMap.of(java.util.Map.of("radius", 6)));
            var hit = targeter.resolve(SkillMetadata.of(caster)).entities();
            boolean ok = hit.contains(golem) && !hit.contains(villager) && !hit.contains(stand)
                    && !hit.contains(minion) && !hit.contains(caster);
            check("AoE hits enemies only", ok, hit.stream().map(e -> e.getType().name()).toList()
                    + " golemTarget=" + golem.getTarget() + " valid=" + golem.isValid());

            var allies = new com.celestirift.skill.targeter.NearbyEntitiesTargeter(
                    com.celestirift.api.skill.ConfigMap.of(java.util.Map.of("radius", 6, "filter", "allies")))
                    .resolve(SkillMetadata.of(caster)).entities();
            check("filter: allies finds own summon", allies.contains(minion) && !allies.contains(golem),
                    allies.stream().map(e -> e.getType().name()).toList().toString());
        } finally {
            villager.remove();
            stand.remove();
            minion.remove();
            golem.remove();
        }
    }

    /** Training dummies: roles drive targeting, lethal damage leaves them alive, plain mannequins are ignored. */
    private void checkDummies(World world, Location at, LivingEntity caster) {
        Location near = at.clone().add(-2, 0, 0);
        var enemy = plugin.dummies().spawn(near, TrainingDummies.Role.ENEMY, 50, null);
        var ally = plugin.dummies().spawn(near, TrainingDummies.Role.ALLY, 50, null);
        var plain = world.spawnEntity(near, EntityType.MANNEQUIN);
        try {
            var hit = new com.celestirift.skill.targeter.NearbyEntitiesTargeter(
                    com.celestirift.api.skill.ConfigMap.of(java.util.Map.of("radius", 6)))
                    .resolve(SkillMetadata.of(caster)).entities();
            check("dummies: enemy targeted, ally and decor mannequin not",
                    hit.contains(enemy) && !hit.contains(ally) && !hit.contains(plain),
                    hit.stream().map(e -> e.getType().name()).toList().toString());
            enemy.damage(500, caster);
            check("dummies never die", !enemy.isDead() && enemy.getHealth() >= 1, "hp " + enemy.getHealth());
        } finally {
            plugin.dummies().clear();
            plain.remove();
        }
    }

    /** cone / line / ring pick the right dummies around a caster facing +Z. */
    private void checkShapes(Location at, LivingEntity caster) {
        caster.setRotation(0, 0); // yaw 0 = facing +Z (south)
        Location base = caster.getLocation();
        java.util.function.BiFunction<Double, Double, org.bukkit.entity.Mannequin> put = (dx, dz) ->
                plugin.dummies().spawn(base.clone().add(dx, 0, dz), TrainingDummies.Role.ENEMY, 50, null);
        var front = put.apply(0.0, 3.0);
        var behind = put.apply(0.0, -3.0);
        var far = put.apply(0.0, 9.0);
        var side = put.apply(5.0, 5.0);
        try {
            var meta = SkillMetadata.of(caster);
            java.util.function.Function<java.util.Map<String, Object>, java.util.Collection<org.bukkit.entity.Entity>>
                    shape = m -> new com.celestirift.skill.targeter.ShapeTargeter(
                            com.celestirift.skill.targeter.ShapeTargeter.Shape.valueOf((String) m.get("shape")),
                            com.celestirift.api.skill.ConfigMap.of(m)).resolve(meta).entities();
            var cone = shape.apply(java.util.Map.of("shape", "CONE", "angle", 90, "range", 5));
            check("cone: front only", cone.contains(front) && !cone.contains(behind) && !cone.contains(far)
                    && !cone.contains(side), names(cone));
            var line = shape.apply(java.util.Map.of("shape", "LINE", "length", 12, "width", 1.5));
            check("line: front and far, not side/behind", line.contains(front) && line.contains(far)
                    && !line.contains(side) && !line.contains(behind), names(line));
            var ring = shape.apply(java.util.Map.of("shape", "RING", "inner", 5, "outer", 8));
            check("ring: only between 5 and 8 blocks", ring.contains(side) && !ring.contains(front)
                    && !ring.contains(behind) && !ring.contains(far), names(ring));
        } finally {
            plugin.dummies().clear();
        }
        var warrior = plugin.classes().get("Warrior");
        check("class passives load", warrior.isPresent() && warrior.get().passives().size() == 3,
                warrior.map(c -> c.passives().toString()).orElse("no Warrior"));
    }

    private static String names(java.util.Collection<org.bukkit.entity.Entity> list) {
        return list.size() + " hit";
    }

    /** Telegraph: nothing happens while the circle fills, then the skill hits from the circle. */
    private void checkTelegraph(World world, Location at) {
        Location spot = at.clone().add(-10, 0, -12);
        LivingEntity boss = (LivingEntity) world.spawnEntity(spot, EntityType.ZOMBIE);
        if (boss instanceof Mob m) {
            m.setAware(false);
        }
        var dummy = plugin.dummies().spawn(spot.clone().add(2, 0, 0), TrainingDummies.Role.ENEMY, 100, null);
        plugin.skills().cast("FrostLord_Nova", SkillMetadata.of(boss)); // 30-tick telegraph, then 6 damage
        Bukkit.getScheduler().runTaskLater(plugin, () -> check("telegraph waits before hitting",
                dummy.getHealth() == 100, "hp " + dummy.getHealth()), 20);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            check("telegraph then hits inside the circle", Math.abs(dummy.getHealth() - 94) < 0.01,
                    "hp " + dummy.getHealth());
            dummy.remove();
            boss.remove();
        }, 40);

        var pyro = plugin.skills().getSkill("Pyroblast");
        check("cast-time loads", pyro.isPresent() && pyro.get().channel().ticks() == 30
                && pyro.get().channel().interruptMove(), pyro.map(s -> s.channel().toString()).orElse("missing"));
    }

    /** chain decays per jump; meteor lands and runs onland; displays appear. */
    private void checkVfx(World world, Location at, LivingEntity caster) {
        var meta = SkillMetadata.of(caster);
        var d1 = plugin.dummies().spawn(at.clone().add(8, 0, 12), TrainingDummies.Role.ENEMY, 100, null);
        var d2 = plugin.dummies().spawn(at.clone().add(10, 0, 12), TrainingDummies.Role.ENEMY, 100, null);
        var d3 = plugin.dummies().spawn(at.clone().add(12, 0, 12), TrainingDummies.Role.ENEMY, 100, null);
        new com.celestirift.skill.mechanic.impl.ChainMechanic(plugin, com.celestirift.api.skill.ConfigMap.of(
                java.util.Map.of("damage", 10, "jumps", 3, "decay", 0.5, "delay", 0, "radius", 3))).cast(meta, d1);
        double t1 = 100 - d1.getHealth(), t2 = 100 - d2.getHealth(), t3 = 100 - d3.getHealth();
        check("chain jumps with decay (10 / 5 / 2.5)", Math.abs(t1 - 10) < 0.01 && Math.abs(t2 - 5) < 0.01
                && Math.abs(t3 - 2.5) < 0.01, t1 + " / " + t2 + " / " + t3);

        int before = com.celestirift.vfx.DisplayFx.live();
        var land = plugin.dummies().spawn(at.clone().add(-12, 0, 12), TrainingDummies.Role.ENEMY, 100, null);
        new com.celestirift.skill.mechanic.impl.MeteorMechanic(plugin, com.celestirift.api.skill.ConfigMap.of(
                java.util.Map.of("duration", 10, "onland", "MeteorStrike_Impact"))).cast(meta, land.getLocation());
        check("meteor spawns a display", com.celestirift.vfx.DisplayFx.live() > before, "");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            check("meteor lands and runs onland", land.getHealth() < 100, "hp " + land.getHealth());
            d1.remove();
            d2.remove();
            d3.remove();
            land.remove();
        }, 20);
    }

    private com.celestirift.aura.AuraSpec spec(java.util.Map<String, Object> values) {
        return com.celestirift.aura.AuraSpec.read(com.celestirift.api.skill.ConfigMap.of(values));
    }

    /** Auras on enemy dummies: stacking, damage-taken, shields, DoT ticks and expiry. */
    private void checkAuras(Location at, LivingEntity caster) {
        var meta = SkillMetadata.of(caster);
        var a = plugin.dummies().spawn(at.clone().add(0, 0, 3), TrainingDummies.Role.ENEMY, 100, null);
        var b = plugin.dummies().spawn(at.clone().add(0, 0, -3), TrainingDummies.Role.ENEMY, 100, null);
        var c = plugin.dummies().spawn(at.clone().add(-14, 0, 0), TrainingDummies.Role.ENEMY, 100, null);

        var mark = spec(java.util.Map.of("id", "Mark", "maxstacks", 2, "damage-taken", 0.25, "duration", 400));
        for (int i = 0; i < 3; i++) {
            plugin.auras().apply(mark, meta, a);
        }
        check("aura stacks up to maxstacks", plugin.auras().stacks(a, "mark") == 2,
                plugin.auras().stacks(a, "mark") + " stacks");
        a.damage(10, caster);
        check("damage-taken per stack", Math.abs((100 - a.getHealth()) - 15) < 0.01, "took " + (100 - a.getHealth()));

        plugin.auras().apply(spec(java.util.Map.of("id", "Shield", "shield", 5, "duration", 400)), meta, b);
        b.damage(8, caster);
        check("shield absorbs then breaks", Math.abs((100 - b.getHealth()) - 3) < 0.01
                && plugin.auras().stacks(b, "shield") == 0, "took " + (100 - b.getHealth()));

        plugin.auras().apply(spec(java.util.Map.of("id", "Burning", "duration", 30, "interval", 10,
                "ontick", "Immolate_Tick")), meta, c);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            check("DoT ticks (3 x 1.5) then the aura expires", Math.abs((100 - c.getHealth()) - 4.5) < 0.01 && plugin.auras().stacks(c, "burning") == 0,
                    "took " + (100 - c.getHealth()));
            a.remove();
            b.remove();
            c.remove();
        }, 45);
    }

    /** Expressions read live values: <caster.mhp> * 2 on a zombie = 40. */
    private void checkVariables(LivingEntity caster) {
        var value = com.celestirift.api.skill.ConfigMap.of(java.util.Map.of("amount", "<caster.mhp> * 2 + <power>"))
                .getNumber(0, "amount");
        double v = value.get(SkillMetadata.of(caster), null);
        check("expressions use live variables", v == 41, "got " + v);
    }

    private void finish(LivingEntity caster, List<Chunk> chunks) {
        check("no skill line threw (incl. delayed lines)", plugin.skills().lineFailures() == 0,
                plugin.skills().lineFailures() + " failure(s), see log");
        check("bosses still tracked", plugin.bosses().activeCount() == plugin.bosses().ids().size(),
                plugin.bosses().activeCount() + " active");
        plugin.bosses().killAll();
        caster.remove();

        paused = true;
        testSafeReload();
        paused = false;

        chunks.forEach(c -> c.removePluginChunkTicket(plugin));
        check("effects cleaned up (no leftover displays)", com.celestirift.vfx.DisplayFx.live() == 0,
                com.celestirift.vfx.DisplayFx.live() + " left");
        check("no SEVERE log from CelestiRift", severe.isEmpty(), String.join(" | ", severe));
        write();
        Logger.getLogger("").removeHandler(logHandler);
        Bukkit.shutdown();
    }

    /** A broken YAML file must not wipe the loaded skills; force must still load the rest. */
    private void testSafeReload() {
        int before = plugin.skills().skillNames().size();
        Path broken = plugin.getDataFolder().toPath().resolve("skills/zz_smoke_broken.yml");
        try {
            Files.writeString(broken, "Broken:\n  skills: [\n  - nope: {\n", StandardCharsets.UTF_8);
            var result = plugin.reloadAll(false);
            check("safe reload keeps old content on a broken file",
                    !result.applied() && plugin.skills().skillNames().size() == before,
                    "applied=" + result.applied() + ", skills " + before + " -> " + plugin.skills().skillNames().size());
            var forced = plugin.reloadAll(true);
            check("reload force loads the rest", forced.applied() && plugin.skills().skillNames().size() == before,
                    plugin.skills().skillNames().size() + " skills");
        } catch (IOException e) {
            check("safe reload", false, e.toString());
        } finally {
            try {
                Files.deleteIfExists(broken);
            } catch (IOException ignored) {
                // throwaway server folder
            }
        }
        var clean = plugin.reloadAll(false);
        check("clean reload", clean.applied() && plugin.skills().skillNames().size() == before, "");
    }

    private void write() {
        lines.add(failed ? "RESULT FAIL" : "RESULT PASS");
        lines.forEach(l -> plugin.getLogger().info("[smoke] " + l));
        try {
            Files.write(plugin.getDataFolder().toPath().resolve(RESULT_FILE), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not write " + RESULT_FILE + ": " + e);
        }
    }
}

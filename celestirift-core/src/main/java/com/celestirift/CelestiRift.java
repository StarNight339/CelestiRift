package com.celestirift;

import com.celestirift.api.CelestiRiftApi;
import com.celestirift.cast.CastService;
import com.celestirift.cast.TriggerDispatcher;
import com.celestirift.command.CelestiRiftCommand;
import com.celestirift.data.PdcPlayerDataStore;
import com.celestirift.gem.ArmorPassiveTask;
import com.celestirift.gem.FusionManager;
import com.celestirift.gem.GemItems;
import com.celestirift.gem.GemManager;
import com.celestirift.gem.SocketData;
import com.celestirift.gui.GemLibraryGui;
import com.celestirift.gui.SocketGui;
import com.celestirift.gui.TriggerGui;
import com.celestirift.listener.GemLibraryListener;
import com.celestirift.listener.PlayerSessionListener;
import com.celestirift.listener.SkillTriggerListener;
import com.celestirift.listener.SocketGuiListener;
import com.celestirift.listener.SummonListener;
import com.celestirift.listener.TriggerGuiListener;
import com.celestirift.mana.ManaManager;
import com.celestirift.mana.ManaRegenTask;
import com.celestirift.skill.SkillManager;
import com.celestirift.summon.Summons;
import com.celestirift.trigger.TriggerBindings;
import com.celestirift.util.CooldownManager;
import com.celestirift.util.Messages;
import com.celestirift.util.SkillItems;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * CelestiRift — a MythicMobs-style custom skill framework for Paper.
 *
 * <p>This class only wires services together. Casting lives in {@link CastService},
 * trigger handling in {@link TriggerDispatcher}, and other plugins use
 * {@link CelestiRiftApi}.
 */
public final class CelestiRift extends JavaPlugin {

    private Messages messages;
    private SkillManager skillManager;
    private CooldownManager cooldowns;
    private SkillItems skillItems;
    private ManaManager manaManager;
    private TriggerBindings triggerBindings;
    private GemItems gemItems;
    private GemManager gemManager;
    private FusionManager fusionManager;
    private SocketData socketData;
    private Summons summons;
    private CastService castService;
    private final com.celestirift.validate.Diagnostics diagnostics = new com.celestirift.validate.Diagnostics();
    private TriggerDispatcher dispatcher;
    private com.celestirift.pack.ResourcePackService resourcePack;
    private com.celestirift.rpg.ProfileManager profiles;
    private com.celestirift.rpg.ClassManager classes;
    private com.celestirift.rpg.DropManager drops;
    private com.celestirift.rpg.boss.BossManager bosses;
    private com.celestirift.devtools.TrainingDummies dummies;
    private com.celestirift.aura.AuraManager auras;

    /** Oldest Paper 26.3 build this plugin is compiled against (keep in sync with gradle.properties). */
    private static final int MIN_PAPER_BUILD = 252;

    @Override
    public void onEnable() {
        checkServerBuild();
        saveDefaultConfig();
        installDefaultSkills();
        installDefaultGems();

        this.messages = new Messages(this);
        this.skillManager = new SkillManager(this);
        this.cooldowns = new CooldownManager();
        this.skillItems = new SkillItems(this);
        var dataStore = new PdcPlayerDataStore(this);
        this.manaManager = new ManaManager(this, dataStore);
        this.profiles = new com.celestirift.rpg.ProfileManager(this, dataStore);
        profiles.loadConfig();
        this.classes = new com.celestirift.rpg.ClassManager(this);
        this.drops = new com.celestirift.rpg.DropManager(this);
        this.bosses = new com.celestirift.rpg.boss.BossManager(this);
        com.celestirift.skill.Placeholders.playerInfo(new com.celestirift.skill.Placeholders.PlayerInfo() {
            @Override public int level(Player p) { return profiles.level(p); }
            @Override public String className(Player p) {
                return profiles.rpgClass(p).map(com.celestirift.rpg.RpgClass::id).orElse("none");
            }
        });
        com.celestirift.api.skill.Variables.resolver(new com.celestirift.skill.SkillVariables(this));
        this.triggerBindings = new TriggerBindings(this);
        this.gemItems = new GemItems(this);
        this.gemManager = new GemManager(this);
        this.fusionManager = new FusionManager(this);
        this.socketData = new SocketData(this);
        this.summons = new Summons(this);
        this.dummies = new com.celestirift.devtools.TrainingDummies(this);
        this.auras = new com.celestirift.aura.AuraManager(this);
        com.celestirift.vfx.DisplayFx.install(this);
        var damageNumbers = new com.celestirift.vfx.DamageNumbers(this);
        com.celestirift.skill.mechanic.impl.HealMechanic.damageNumbers = damageNumbers;
        com.celestirift.skill.CombatRules.install(this);
        this.castService = new CastService(this);
        com.celestirift.integration.Orion.install(this);
        this.dispatcher = new TriggerDispatcher(this);
        this.resourcePack = new com.celestirift.pack.ResourcePackService(this);
        resourcePack.reload();

        getServer().getServicesManager().register(CelestiRiftApi.class, new ApiImpl(this), this,
                ServicePriority.Normal);

        CelestiRiftCommand.register(this);
        com.celestirift.command.PlayerCommands.register(this);
        registerListeners(
                new SkillTriggerListener(this),
                new SocketGuiListener(this),
                new TriggerGuiListener(this),
                new GemLibraryListener(this),
                new SummonListener(this),
                new PlayerSessionListener(this),
                resourcePack,
                drops,
                bosses,
                dummies,
                auras,
                damageNumbers,
                new com.celestirift.rpg.RpgListener(this));
        new ManaRegenTask(this).start();
        new ArmorPassiveTask(this).start();
        bosses.start();
        dummies.start();
        auras.start();
        dispatcher.startTimer();

        // Load content one tick after startup, once every plugin has enabled, so
        // add-ons can register mechanics/targeters/conditions in their onEnable.
        messages.reload();
        getServer().getScheduler().runTask(this, this::loadContent);
        if (com.celestirift.devtools.SmokeTest.enabled()) {
            new com.celestirift.devtools.SmokeTest(this).schedule();
        }

        getLogger().info("CelestiRift enabled.");
    }

    @Override
    public void onDisable() {
        // Hand weapons back from open socket menus (close events don't reach a disabled plugin).
        for (Player p : getServer().getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof SocketGui gui) {
                gui.close();
                p.closeInventory();
            }
        }
        if (manaManager != null) {
            manaManager.saveAll();
        }
        if (profiles != null) {
            profiles.saveAll();
        }
        if (auras != null) {
            auras.shutdown();
        }
        com.celestirift.vfx.DisplayFx.clearAll();
        com.celestirift.skill.mechanic.impl.HealMechanic.damageNumbers = null;
        if (dummies != null) {
            dummies.stop();
        }
        if (bosses != null) {
            bosses.shutdown();
        }
        com.celestirift.skill.Placeholders.playerInfo(null);
        com.celestirift.api.skill.Variables.resolver(null);
        com.celestirift.skill.CombatRules.install(null);
        getServer().getServicesManager().unregisterAll(this);
        getLogger().info("CelestiRift disabled.");
    }

    /** Warn clearly (instead of failing later with NoSuchMethodError) on too-old Paper builds. */
    private void checkServerBuild() {
        var info = io.papermc.paper.ServerBuildInfo.buildInfo();
        int build = info.buildNumber().orElse(Integer.MAX_VALUE); // dev builds have no number
        if (info.minecraftVersionId().equals("26.3") && build < MIN_PAPER_BUILD) {
            getLogger().severe("This server runs Paper 26.3 build " + build + ", but CelestiRift needs build "
                    + MIN_PAPER_BUILD + " or newer. Update paper.jar - some features (e.g. bosses) will fail.");
        }
    }

    /** Bundled default content (skills/ layout described in skills/_guide.yml). Keep in sync with resources. */
    public static final java.util.List<String> DEFAULT_SKILL_FILES = java.util.List.of(
            "skills/_guide.yml",
            "skills/shared/effects.yml",
            "skills/gems/fireball.yml", "skills/gems/frost_nova.yml", "skills/gems/heal.yml",
            "skills/gems/smite.yml", "skills/gems/blackfire.yml", "skills/gems/shadow_bolt.yml",
            "skills/gems/chain_lightning.yml", "skills/gems/sword_rain.yml", "skills/gems/starfall.yml",
            "skills/gems/blizzard.yml", "skills/gems/radiant_blade.yml",
            "skills/classes/mage.yml", "skills/classes/warrior.yml", "skills/classes/cleric.yml",
            "skills/bosses/frost_lord.yml",
            "skills/showcase/basics.yml", "skills/showcase/templates.yml", "skills/showcase/targeting.yml",
            "skills/showcase/auras.yml", "skills/showcase/casting.yml", "skills/showcase/particles.yml",
            "skills/showcase/displays.yml");

    /** Bundled default gems, one theme per file (gems.yml itself is the field reference). */
    public static final java.util.List<String> DEFAULT_GEM_FILES = java.util.List.of(
            "gems/fire.yml", "gems/frost.yml", "gems/holy.yml", "gems/shadow.yml",
            "gems/storm.yml", "gems/steel.yml", "gems/runes.yml", "gems/fusion.yml");

    /** Bundled fusion recipes, one per file (fusions.yml itself is the field reference). */
    public static final java.util.List<String> DEFAULT_FUSION_FILES = java.util.List.of(
            "fusions/blackfire.yml", "fusions/blizzard.yml", "fusions/radiant.yml");

    /** Default gems only on a fresh install (no gems.yml and no gems/ folder yet). */
    private void installDefaultGems() {
        if (new java.io.File(getDataFolder(), "gems.yml").exists() || new java.io.File(getDataFolder(), "gems").exists()) {
            return;
        }
        saveIfMissing("gems.yml");
        DEFAULT_GEM_FILES.forEach(this::saveIfMissing);
        if (!new java.io.File(getDataFolder(), "fusions.yml").exists()
                && !new java.io.File(getDataFolder(), "fusions").exists()) {
            saveIfMissing("fusions.yml");
            DEFAULT_FUSION_FILES.forEach(this::saveIfMissing);
        }
    }

    /**
     * Write the default skills only on a fresh install (no skills/ folder content yet).
     * Existing servers keep their own files: copying the new set next to an older one
     * would define the same skills twice.
     */
    private void installDefaultSkills() {
        java.nio.file.Path skills = getDataFolder().toPath().resolve("skills");
        boolean fresh;
        try (var files = java.nio.file.Files.exists(skills) ? java.nio.file.Files.walk(skills) : java.util.stream.Stream.<java.nio.file.Path>empty()) {
            fresh = files.noneMatch(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"));
        } catch (java.io.IOException e) {
            fresh = false;
        }
        if (!fresh) {
            return;
        }
        DEFAULT_SKILL_FILES.forEach(this::saveIfMissing);
        saveIfMissing("bosses/frost_lord.yml"); // the boss that uses skills/bosses/frost_lord.yml
    }

    /** Copy a bundled default file into the data folder unless the user already has one. */
    private void saveIfMissing(String path) {
        if (!new java.io.File(getDataFolder(), path).exists()) {
            saveResource(path, false);
        }
    }

    private void registerListeners(Listener... listeners) {
        for (Listener l : listeners) {
            getServer().getPluginManager().registerEvents(l, this);
        }
    }

    /** Compile skills and load gem/fusion/trigger definitions. */
    private void loadContent() {
        diagnostics.clear();
        skillManager.reload(diagnostics);
        triggerBindings.reload();
        gemManager.reload(diagnostics);
        fusionManager.reload(diagnostics);
        classes.reload(diagnostics);
        bosses.reload(diagnostics);
        drops.reload(diagnostics);
        diagnostics.log(getLogger());
        if (!diagnostics.isEmpty()) {
            getLogger().warning(diagnostics.count(com.celestirift.validate.Diagnostics.Severity.ERROR)
                    + " error(s), " + diagnostics.count(com.celestirift.validate.Diagnostics.Severity.WARNING)
                    + " warning(s) in content - run /cr validate in-game for details.");
        }
        new com.celestirift.validate.ReferenceGenerator(this).writeAll();
    }

    /** Everything that holds loaded content, for rollback. Order does not matter. */
    private java.util.List<com.celestirift.util.Restorable> contentHolders() {
        return java.util.List.of(skillManager, triggerBindings, gemManager, fusionManager, classes, bosses, drops);
    }

    /**
     * Re-apply resource-pack looks to every CelestiRift item in a player's inventory,
     * so enabling/disabling the pack also updates gems players already own.
     */
    public void refreshItemLooks(Player player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            var item = inv.getItem(i);
            if (item == null || item.isEmpty()) {
                continue;
            }
            if (gemItems.refreshLook(item) || skillItems.refreshLook(item)) {
                inv.setItem(i, item);
            }
        }
    }

    /** settings.debug in config.yml: extra console output (cast results, stack traces). */
    public boolean debug() {
        return getConfig().getBoolean("settings.debug", false);
    }

    /** Log only when debug is on. The message is built lazily. */
    public void debug(java.util.function.Supplier<String> message) {
        if (debug()) {
            getLogger().info("[debug] " + message.get());
        }
    }

    /** Problems found by the last content load. */
    public com.celestirift.validate.Diagnostics diagnostics() { return diagnostics; }

    /** Outcome of {@link #reloadAll(boolean)}. */
    public record ReloadResult(boolean applied, java.util.List<com.celestirift.validate.Diagnostics.Entry> brokenFiles) {}

    /**
     * Safe reload: config/messages are syntax-checked first, then content is loaded. If any
     * file fails to parse, the previous content is kept (nothing disappears mid-game) unless
     * {@code force} is set. Diagnostics from the attempt are always kept for /cr validate.
     */
    public ReloadResult reloadAll(boolean force) {
        var broken = new java.util.ArrayList<com.celestirift.validate.Diagnostics.Entry>();
        for (String f : java.util.List.of("config.yml", "messages.yml")) {
            var file = new java.io.File(getDataFolder(), f).toPath();
            if (java.nio.file.Files.exists(file)) {
                var d = new com.celestirift.validate.Diagnostics();
                com.celestirift.skill.load.SkillLoader.readYaml(file, f, d);
                broken.addAll(d.brokenFiles());
            }
        }
        if (!broken.isEmpty() && !force) {
            diagnostics.clear();
            broken.forEach(e -> diagnostics.error(e.file(), e.line(), e.context(), e.message()));
            diagnostics.log(getLogger());
            getLogger().severe("Reload cancelled - fix the file(s) above. Everything still runs on the old settings.");
            return new ReloadResult(false, broken);
        }

        com.celestirift.vfx.DisplayFx.clearAll();
        var restore = contentHolders().stream().map(com.celestirift.util.Restorable::snapshot).toList();
        reloadConfig();
        messages.reload();
        manaManager.loadConfig();
        profiles.loadConfig();
        resourcePack.reload();
        loadContent();
        var brokenContent = diagnostics.brokenFiles();
        if (!brokenContent.isEmpty() && !force) {
            restore.forEach(Runnable::run);
            getLogger().severe(brokenContent.size() + " file(s) could not be read - kept the previous skills/gems/"
                    + "bosses. Fix them and /cr reload again (or /cr reload force to load without them).");
            return new ReloadResult(false, brokenContent);
        }
        getServer().getOnlinePlayers().forEach(this::refreshItemLooks);
        return new ReloadResult(true, brokenContent);
    }

    /** Safe reload (keeps old content if a file is broken). */
    public ReloadResult reloadAll() {
        return reloadAll(false);
    }

    // ------------------------------------------------------------------ services

    public Messages messages() { return messages; }
    public com.celestirift.rpg.ProfileManager profiles() { return profiles; }
    public com.celestirift.rpg.ClassManager classes() { return classes; }
    public com.celestirift.rpg.DropManager drops() { return drops; }
    public com.celestirift.rpg.boss.BossManager bosses() { return bosses; }
    public com.celestirift.devtools.TrainingDummies dummies() { return dummies; }
    public com.celestirift.aura.AuraManager auras() { return auras; }
    public SkillManager skills() { return skillManager; }
    public CooldownManager cooldowns() { return cooldowns; }
    public SkillItems items() { return skillItems; }
    public ManaManager mana() { return manaManager; }
    public TriggerBindings triggers() { return triggerBindings; }
    public GemItems gems() { return gemItems; }
    public GemManager gemDefs() { return gemManager; }
    public FusionManager fusions() { return fusionManager; }
    public SocketData sockets() { return socketData; }
    public Summons summons() { return summons; }
    public CastService casts() { return castService; }
    public TriggerDispatcher dispatcher() { return dispatcher; }
    public com.celestirift.pack.ResourcePackService resourcePack() { return resourcePack; }

    // ------------------------------------------------------------------ menus

    /** Open the socketing menu for a player. */
    public void openSocketMenu(Player player) {
        new SocketGui(this, player).open();
    }

    /** Open the trigger-editing menu for the player's held gem/weapon. */
    public void openTriggerMenu(Player player) {
        new TriggerGui(this, player).open();
    }

    /** Open the gem library (browse & get every defined gem). */
    public void openGemLibrary(Player player) {
        new GemLibraryGui(this).open(player);
    }
}

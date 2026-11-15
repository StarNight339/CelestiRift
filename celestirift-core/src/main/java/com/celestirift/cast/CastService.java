package com.celestirift.cast;

import com.celestirift.CelestiRift;
import com.celestirift.api.CastResult;
import com.celestirift.api.event.SkillCastEvent;
import com.celestirift.api.event.SkillPreCastEvent;
import com.celestirift.api.skill.SkillMetadata;
import com.celestirift.gem.Augment;
import com.celestirift.skill.Skill;
import com.celestirift.util.Messages;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * The player cast pipeline: lookup → skill conditions → cooldown → mana →
 * {@link SkillPreCastEvent} → run → apply cooldown/mana → {@link SkillCastEvent}.
 * Nothing is consumed unless the skill actually runs.
 */
public final class CastService {

    private final CelestiRift plugin;

    public CastService(CelestiRift plugin) {
        this.plugin = plugin;
    }

    public CastResult cast(Player player, String skillName, @Nullable Entity trigger) {
        return cast(player, skillName, trigger, false, Augment.NONE);
    }

    /**
     * @param silent  suppress deny feedback (used for item/trigger casts so failures are quiet)
     * @param augment passive multipliers from the weapon's runes
     */
    public CastResult cast(Player player, String skillName, @Nullable Entity trigger,
                           boolean silent, Augment augment) {
        CastResult result = doCast(player, skillName, trigger, silent, augment);
        plugin.debug(() -> "cast " + skillName + " by " + player.getName()
                + (trigger != null ? " (trigger " + trigger.getType() + ")" : "") + " -> " + result);
        return result;
    }

    private CastResult doCast(Player player, String skillName, @Nullable Entity trigger,
                              boolean silent, Augment augment) {
        var opt = plugin.skills().getSkill(skillName);
        if (opt.isEmpty()) {
            if (!silent) {
                plugin.messages().send(player, "cast.unknown-skill", Messages.p("skill", skillName));
            }
            return CastResult.UNKNOWN_SKILL;
        }
        Skill skill = opt.get();

        // Passive runes socketed into worn armor boost every skill the player casts.
        Augment worn = plugin.sockets().wornAugment(player);
        if (!worn.isIdentity()) {
            augment = augment.timesMultipliers(worn);
        }
        SkillMetadata meta = new SkillMetadata(player, trigger, player.getLocation(), (float) augment.power())
                .rangeMultiplier(augment.range());

        // Skill-level conditions gate the whole cast: no cooldown/mana is spent.
        if (!plugin.skills().canCast(skill, meta)) {
            if (!silent) {
                plugin.messages().actionBar(player, "cast.conditions-failed", Messages.p("skill", skill.name()));
            }
            return CastResult.CONDITIONS_FAILED;
        }

        double remaining = plugin.cooldowns().remaining(player.getUniqueId(), skill.name());
        if (remaining > 0) {
            if (!silent) {
                plugin.messages().actionBar(player, "cast.cooldown",
                        Messages.p("skill", skill.name()), Messages.p("remaining", Messages.fmt1(remaining)));
            }
            return CastResult.ON_COOLDOWN;
        }

        var mana = plugin.mana();
        if (!mana.has(player.getUniqueId(), skill.manaCost())) {
            if (!silent) {
                plugin.messages().actionBar(player, "cast.no-mana",
                        Messages.p("mana", Messages.fmt(mana.get(player.getUniqueId()))),
                        Messages.p("cost", Messages.fmt(skill.manaCost())));
            }
            return CastResult.NOT_ENOUGH_MANA;
        }

        SkillPreCastEvent pre = new SkillPreCastEvent(player, skill.name(), meta,
                skill.manaCost(), skill.cooldownSeconds() * augment.cooldown());
        if (!pre.callEvent()) {
            return CastResult.CANCELLED;
        }
        // A listener may have raised the cost above what the player has.
        if (!mana.has(player.getUniqueId(), pre.getManaCost())) {
            return CastResult.NOT_ENOUGH_MANA;
        }

        if (skill.channel().ticks() > 0) {
            if (channels.containsKey(player.getUniqueId())) {
                plugin.messages().actionBar(player, "cast.busy");
                return CastResult.CANCELLED;
            }
            startChannel(player, skill, meta, pre);
            return CastResult.CASTING;
        }
        return complete(player, skill, meta, pre);
    }

    /** Run the skill and pay for it (instantly, or when a cast-time finishes). */
    private CastResult complete(Player player, Skill skill, SkillMetadata meta, SkillPreCastEvent pre) {
        var mana = plugin.mana();
        if (!mana.has(player.getUniqueId(), pre.getManaCost())) {
            return CastResult.NOT_ENOUGH_MANA;
        }
        if (!plugin.skills().cast(skill.name(), meta)) {
            return CastResult.CONDITIONS_FAILED; // conditions re-checked inside; nothing consumed
        }
        plugin.cooldowns().set(player.getUniqueId(), skill.name(), pre.getCooldownSeconds());
        mana.consume(player.getUniqueId(), pre.getManaCost());
        // Hotbar sweep on skill items of this skill (they share a cooldown group).
        int ticks = (int) Math.round(pre.getCooldownSeconds() * 20);
        if (ticks > 0) {
            player.setCooldown(com.celestirift.util.SkillItems.cooldownGroup(skill.name()), ticks);
        }

        if (mana.enabled() && mana.showActionBar()) {
            mana.showBar(player);
        }
        new SkillCastEvent(player, skill.name(), meta, pre.getManaCost()).callEvent();
        return CastResult.SUCCESS;
    }

    // ------------------------------------------------------------------ feedback

    /**
     * Tell a player why a key-press cast did nothing (cooldown / mana). Trigger casts
     * are silent so passives don't spam; deliberate key presses should not feel dead.
     */
    public void explain(Player player, String skillName, CastResult result) {
        var skill = plugin.skills().getSkill(skillName).orElse(null);
        if (skill == null) {
            return;
        }
        switch (result) {
            case ON_COOLDOWN -> plugin.messages().actionBar(player, "cast.cooldown",
                    Messages.p("skill", skill.name()), Messages.p("remaining",
                            Messages.fmt1(plugin.cooldowns().remaining(player.getUniqueId(), skill.name()))));
            case NOT_ENOUGH_MANA -> plugin.messages().actionBar(player, "cast.no-mana",
                    Messages.p("mana", Messages.fmt(plugin.mana().get(player.getUniqueId()))),
                    Messages.p("cost", Messages.fmt(skill.manaCost())));
            default -> { }
        }
    }

    // ------------------------------------------------------------------ cast time

    private final class Channel {
        final Player player;
        final Skill skill;
        final SkillMetadata meta;
        final SkillPreCastEvent pre;
        final org.bukkit.Location start;
        int elapsed;
        org.bukkit.scheduler.BukkitTask task;

        Channel(Player player, Skill skill, SkillMetadata meta, SkillPreCastEvent pre) {
            this.player = player;
            this.skill = skill;
            this.meta = meta;
            this.pre = pre;
            this.start = player.getLocation();
        }
    }

    private final java.util.Map<java.util.UUID, Channel> channels = new java.util.HashMap<>();

    /** Is the player currently casting a cast-time skill? */
    public boolean isCasting(Player player) {
        return channels.containsKey(player.getUniqueId());
    }

    private void startChannel(Player player, Skill skill, SkillMetadata meta, SkillPreCastEvent pre) {
        Channel ch = new Channel(player, skill, meta, pre);
        channels.put(player.getUniqueId(), ch);
        int total = skill.channel().ticks();
        ch.task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || player.isDead()) {
                stop(ch);
                return;
            }
            if (skill.channel().interruptMove() && moved(ch.start, player.getLocation())) {
                interrupt(player);
                return;
            }
            ch.elapsed++;
            if (ch.elapsed % 4 == 0) {
                player.getWorld().spawnParticle(org.bukkit.Particle.ENCHANT,
                        player.getLocation().add(0, 1, 0), 6, 0.4, 0.6, 0.4, 0.5);
            }
            if (ch.elapsed >= total) {
                stop(ch);
                // Fire from where the player is now (they may have turned to aim).
                SkillMetadata now = new SkillMetadata(player, meta.trigger(), player.getLocation(), meta.power())
                        .rangeMultiplier(meta.rangeMultiplier());
                CastResult r = complete(player, skill, now, pre);
                if (!r.success()) {
                    explain(player, skill.name(), r);
                }
                return;
            }
            if (ch.elapsed % 2 == 0) {
                plugin.messages().actionBar(player, "cast.casting", Messages.p("skill", skill.name()),
                        Messages.p("bar", bar(ch.elapsed, total)),
                        Messages.p("remaining", Messages.fmt1((total - ch.elapsed) / 20.0)));
            }
        }, 1L, 1L);
    }

    /** Cancel the player's cast (moved, took damage, swapped worlds...). */
    public void interrupt(Player player) {
        Channel ch = channels.get(player.getUniqueId());
        if (ch != null) {
            stop(ch);
            plugin.messages().actionBar(player, "cast.interrupted", Messages.p("skill", ch.skill.name()));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_FIRE_EXTINGUISH, 0.6f, 1.4f);
        }
    }

    /** Interrupt on damage if the casting skill says so. */
    public void onDamaged(Player player) {
        Channel ch = channels.get(player.getUniqueId());
        if (ch != null && ch.skill.channel().interruptDamage()) {
            interrupt(player);
        }
    }

    private void stop(Channel ch) {
        channels.remove(ch.player.getUniqueId());
        if (ch.task != null) {
            ch.task.cancel();
        }
    }

    private static boolean moved(org.bukkit.Location a, org.bukkit.Location b) {
        if (a.getWorld() != b.getWorld()) return true;
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz > 0.25 * 0.25 || Math.abs(a.getY() - b.getY()) > 1.2;
    }

    private static String bar(int done, int total) {
        int filled = (int) Math.round(10.0 * done / total);
        return "<aqua>" + "■".repeat(filled) + "<dark_gray>" + "■".repeat(10 - filled);
    }
}

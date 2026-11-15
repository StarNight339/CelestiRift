package com.celestirift.command.sub;

import com.celestirift.CelestiRift;
import com.celestirift.command.Cmd;
import com.celestirift.api.skill.SkillMetadata;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** {@code reload, list, info, cast, give}. */
public final class SkillSubcommands {

    private final CelestiRift plugin;

    public SkillSubcommands(CelestiRift plugin) {
        this.plugin = plugin;
    }

    public List<LiteralArgumentBuilder<CommandSourceStack>> build() {
        return List.of(reload(), validate(), list(), info(), cast(), give());
    }

    private LiteralArgumentBuilder<CommandSourceStack> reload() {
        return Commands.literal("reload")
                .executes(ctx -> doReload(Cmd.sender(ctx), false))
                .then(Commands.literal("force").executes(ctx -> doReload(Cmd.sender(ctx), true)));
    }

    private int doReload(CommandSender sender, boolean force) {
        var result = plugin.reloadAll(force);
        if (!result.applied()) {
            sender.sendMessage(Component.text("✘ Reload cancelled - these file(s) could not be read, so the "
                    + "previous content is still active:", NamedTextColor.RED));
            for (var e : result.brokenFiles()) {
                sender.sendMessage(Component.text("  " + e, NamedTextColor.GRAY));
            }
            sender.sendMessage(Component.text("Fix them and /cr reload again, or /cr reload force to load "
                    + "everything else without them.", NamedTextColor.GRAY));
            return Cmd.OK;
        }
        Cmd.ok(sender, "Reloaded: " + plugin.skills().skillNames().size() + " skills, "
                + plugin.gemDefs().ids().size() + " gems, " + plugin.fusions().recipes().size() + " fusions, "
                + plugin.bosses().ids().size() + " bosses.");
        sendSummary(sender);
        return Cmd.OK;
    }

    /** {@code /cr validate} — list every problem found by the last load (file:line + hint). */
    private LiteralArgumentBuilder<CommandSourceStack> validate() {
        return Commands.literal("validate").executes(ctx -> {
            CommandSender sender = Cmd.sender(ctx);
            var diag = plugin.diagnostics();
            if (diag.isEmpty()) {
                return Cmd.ok(sender, "✔ No problems found in skills, gems or fusions.");
            }
            int shown = 0;
            for (var e : diag.entries()) {
                if (shown++ >= 40) {
                    sender.sendMessage(Component.text("… " + (diag.entries().size() - 40)
                            + " more in the server console.", NamedTextColor.GRAY));
                    break;
                }
                boolean error = e.severity() == com.celestirift.validate.Diagnostics.Severity.ERROR;
                sender.sendMessage(Component.text(error ? "✖ " : "⚠ ", error ? NamedTextColor.RED : NamedTextColor.GOLD)
                        .append(Component.text(e.location() + " ", NamedTextColor.GRAY))
                        .append(Component.text(e.context() + ": ", NamedTextColor.WHITE))
                        .append(Component.text(e.message(), error ? NamedTextColor.RED : NamedTextColor.YELLOW)));
            }
            sendSummary(sender);
            return Cmd.OK;
        });
    }

    private void sendSummary(CommandSender sender) {
        var diag = plugin.diagnostics();
        if (diag.isEmpty()) {
            return;
        }
        long errors = diag.count(com.celestirift.validate.Diagnostics.Severity.ERROR);
        long warnings = diag.count(com.celestirift.validate.Diagnostics.Severity.WARNING);
        sender.sendMessage(Component.text(errors + " error(s), " + warnings + " warning(s)",
                        errors > 0 ? NamedTextColor.RED : NamedTextColor.GOLD)
                .append(Component.text(" — /cr validate for details", NamedTextColor.GRAY)));
    }

    private LiteralArgumentBuilder<CommandSourceStack> list() {
        return Commands.literal("list").executes(ctx -> {
            var names = plugin.skills().skillNames();
            Cmd.sender(ctx).sendMessage(Component.text("Skills (" + names.size() + "): ", NamedTextColor.AQUA)
                    .append(Component.text(String.join(", ", names), NamedTextColor.WHITE)));
            return Cmd.OK;
        });
    }

    private LiteralArgumentBuilder<CommandSourceStack> info() {
        return Commands.literal("info")
                .then(Commands.argument("skill", StringArgumentType.word())
                        .suggests(Cmd.skills(plugin))
                        .executes(ctx -> {
                            CommandSender sender = Cmd.sender(ctx);
                            String name = StringArgumentType.getString(ctx, "skill");
                            var opt = plugin.skills().getSkill(name);
                            if (opt.isEmpty()) {
                                return Cmd.fail(sender, "Unknown skill: " + name);
                            }
                            var skill = opt.get();
                            sender.sendMessage(Component.text(skill.name(), NamedTextColor.LIGHT_PURPLE));
                            if (!skill.description().isBlank()) {
                                sender.sendMessage(Component.text("  ").append(
                                        net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                                                .deserialize(skill.description(), com.celestirift.integration.Orion.tags())));
                            }
                            sender.sendMessage(Component.text("  Cooldown: " + skill.cooldownSeconds() + "s", NamedTextColor.GRAY));
                            sender.sendMessage(Component.text("  Mana: " + skill.manaCost(), NamedTextColor.GRAY));
                            sender.sendMessage(Component.text("  Lines: " + skill.lines().size(), NamedTextColor.GRAY));
                            return Cmd.OK;
                        }));
    }

    private LiteralArgumentBuilder<CommandSourceStack> cast() {
        return Commands.literal("cast")
                .then(Commands.argument("skill", StringArgumentType.word())
                        .suggests(Cmd.skills(plugin))
                        .executes(ctx -> cast(ctx, null))
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(ctx -> cast(ctx, Cmd.player(ctx, "player")))));
    }

    private int cast(CommandContext<CommandSourceStack> ctx, @Nullable Player caster) {
        CommandSender sender = Cmd.sender(ctx);
        if (caster == null && (caster = Cmd.self(ctx, "Console must specify a player.")) == null) {
            return Cmd.FAIL;
        }
        String skillName = StringArgumentType.getString(ctx, "skill");
        if (plugin.skills().cast(skillName, SkillMetadata.of(caster))) {
            return Cmd.ok(sender, "Cast " + skillName + " as " + caster.getName() + ".");
        }
        return Cmd.fail(sender, "Unknown skill: " + skillName);
    }

    private LiteralArgumentBuilder<CommandSourceStack> give() {
        return Commands.literal("give")
                .then(Commands.argument("skill", StringArgumentType.word())
                        .suggests(Cmd.skills(plugin))
                        .executes(ctx -> give(ctx, null, Material.BLAZE_ROD))
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(ctx -> give(ctx, Cmd.player(ctx, "player"), Material.BLAZE_ROD))
                                .then(Commands.argument("material", ArgumentTypes.itemStack())
                                        .executes(ctx -> give(ctx, Cmd.player(ctx, "player"),
                                                ctx.getArgument("material", ItemStack.class).getType())))));
    }

    private int give(CommandContext<CommandSourceStack> ctx, @Nullable Player target, Material material) {
        CommandSender sender = Cmd.sender(ctx);
        String skillName = StringArgumentType.getString(ctx, "skill");
        if (plugin.skills().getSkill(skillName).isEmpty()) {
            return Cmd.fail(sender, "Unknown skill: " + skillName);
        }
        if (target == null && (target = Cmd.self(ctx, "Console must specify a player.")) == null) {
            return Cmd.FAIL;
        }
        target.getInventory().addItem(plugin.items().create(skillName,
                material.isItem() ? material : Material.BLAZE_ROD));
        return Cmd.ok(sender, "Gave " + skillName + " skill item to " + target.getName() + ".");
    }
}

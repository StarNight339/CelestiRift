package com.celestirift.command;

import com.celestirift.CelestiRift;
import com.celestirift.rpg.PlayerProfile;
import com.celestirift.rpg.RpgClass;
import com.celestirift.util.Messages;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Player commands (no admin needed):
 * <ul>
 *   <li>{@code /class} — your class and the list; {@code /class choose <id>}; {@code /class info <id>}</li>
 *   <li>{@code /profile [player]} — level, XP, class, mana</li>
 * </ul>
 */
public final class PlayerCommands {

    private PlayerCommands() {}

    public static void register(CelestiRift plugin) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(classCommand(plugin), "Choose and view RPG classes.", List.of("classes"));
            event.registrar().register(profileCommand(plugin), "Your level, XP, class and mana.", List.of("stats"));
        });
    }

    // ------------------------------------------------------------------ /class

    private static com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> classCommand(CelestiRift plugin) {
        var classArg = Commands.argument("class", StringArgumentType.word())
                .suggests(Cmd.suggest(ctx -> plugin.classes().ids()));
        return Commands.literal("class")
                .requires(src -> src.getSender().hasPermission("celestirift.class"))
                .executes(ctx -> overview(plugin, ctx))
                .then(Commands.literal("choose").then(classArg.executes(ctx -> choose(plugin, ctx))))
                .then(Commands.literal("info").then(Commands.argument("class", StringArgumentType.word())
                        .suggests(Cmd.suggest(ctx -> plugin.classes().ids()))
                        .executes(ctx -> info(plugin, ctx))))
                .build();
    }

    private static int overview(CelestiRift plugin, CommandContext<CommandSourceStack> ctx) {
        var sender = Cmd.sender(ctx);
        if (sender instanceof Player p) {
            var current = plugin.profiles().rpgClass(p);
            if (current.isPresent()) {
                plugin.messages().send(p, "class.current", Messages.mm("display", current.get().display()));
            } else {
                plugin.messages().send(p, "class.none");
            }
        }
        plugin.messages().send(sender, "class.list-header");
        for (RpgClass c : plugin.classes().all()) {
            plugin.messages().send(sender, "class.list-entry", Messages.p("id", c.id()),
                    Messages.mm("display", c.display()), Messages.mm("description", c.description()));
        }
        return Cmd.OK;
    }

    private static int info(CelestiRift plugin, CommandContext<CommandSourceStack> ctx) {
        var sender = Cmd.sender(ctx);
        String id = StringArgumentType.getString(ctx, "class");
        RpgClass c = plugin.classes().get(id).orElse(null);
        if (c == null) {
            plugin.messages().send(sender, "class.unknown", Messages.p("class", id));
            return Cmd.FAIL;
        }
        plugin.messages().send(sender, "class.info", Messages.mm("display", c.display()),
                Messages.mm("description", c.description()),
                Messages.p("mana", Messages.fmt(c.manaBase())), Messages.p("mana_per_level", Messages.fmt1(c.manaPerLevel())),
                Messages.p("regen", Messages.fmt1(c.regenBase())), Messages.p("sockets", c.socketBonus()),
                Messages.p("gems", c.startingGems().isEmpty() ? "-" : String.join(", ", c.startingGems())));
        return Cmd.OK;
    }

    private static int choose(CelestiRift plugin, CommandContext<CommandSourceStack> ctx) {
        Player p = Cmd.self(ctx, "Only players can choose a class.");
        if (p == null) {
            return Cmd.FAIL;
        }
        String id = StringArgumentType.getString(ctx, "class");
        RpgClass c = plugin.classes().get(id).orElse(null);
        if (c == null) {
            plugin.messages().send(p, "class.unknown", Messages.p("class", id));
            return Cmd.FAIL;
        }
        if (c.permission() != null && !p.hasPermission(c.permission())) {
            plugin.messages().send(p, "class.no-permission", Messages.mm("display", c.display()));
            return Cmd.FAIL;
        }
        if (plugin.profiles().get(p).classId() != null && !plugin.profiles().allowClassChange()) {
            plugin.messages().send(p, "class.already");
            return Cmd.FAIL;
        }
        if (!plugin.profiles().setClass(p, c.id(), true)) {
            return Cmd.FAIL;
        }
        plugin.messages().send(p, "class.chosen", Messages.mm("display", c.display()));
        return Cmd.OK;
    }

    // ------------------------------------------------------------------ /profile

    private static com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack> profileCommand(CelestiRift plugin) {
        return Commands.literal("profile")
                .requires(src -> src.getSender().hasPermission("celestirift.profile"))
                .executes(ctx -> profile(plugin, ctx, null))
                .then(Commands.argument("player", ArgumentTypes.player())
                        .requires(src -> src.getSender().hasPermission("celestirift.profile.others"))
                        .executes(ctx -> profile(plugin, ctx, Cmd.player(ctx, "player"))))
                .build();
    }

    private static int profile(CelestiRift plugin, CommandContext<CommandSourceStack> ctx, @Nullable Player target) {
        if (target == null && (target = Cmd.self(ctx, "Usage: /profile <player>")) == null) {
            return Cmd.FAIL;
        }
        var sender = Cmd.sender(ctx);
        PlayerProfile prof = plugin.profiles().get(target);
        double needed = plugin.profiles().curve().xpToNext(prof.level());
        String display = plugin.profiles().rpgClass(target).map(RpgClass::display).orElse("<gray>none");
        plugin.messages().send(sender, "profile.header", Messages.p("player", target.getName()));
        plugin.messages().send(sender, "profile.level", Messages.p("level", prof.level()),
                Messages.p("xp", Messages.fmt(prof.xp())),
                Messages.p("needed", Double.isInfinite(needed) ? "MAX" : Messages.fmt(needed)));
        plugin.messages().send(sender, "profile.class", Messages.mm("class", display));
        plugin.messages().send(sender, "profile.mana",
                Messages.p("mana", Messages.fmt(plugin.mana().get(target.getUniqueId()))),
                Messages.p("max", Messages.fmt(plugin.mana().max(target.getUniqueId()))));
        return Cmd.OK;
    }
}

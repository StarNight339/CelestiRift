package com.celestirift.command.sub;

import com.celestirift.CelestiRift;
import com.celestirift.api.event.PlayerXpGainEvent;
import com.celestirift.command.Cmd;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin RPG commands: {@code level set}, {@code xp add}, {@code class set},
 * {@code boss spawn|list|killall}.
 */
public final class RpgSubcommands {

    private final CelestiRift plugin;

    public RpgSubcommands(CelestiRift plugin) {
        this.plugin = plugin;
    }

    public List<LiteralArgumentBuilder<CommandSourceStack>> build() {
        return List.of(level(), xp(), clazz(), boss());
    }

    private LiteralArgumentBuilder<CommandSourceStack> level() {
        return Commands.literal("level").then(Commands.literal("set")
                .then(Commands.argument("player", ArgumentTypes.player())
                        .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    Player p = Cmd.player(ctx, "player");
                                    plugin.profiles().setLevel(p, IntegerArgumentType.getInteger(ctx, "level"));
                                    return Cmd.ok(Cmd.sender(ctx), p.getName() + " is now level "
                                            + plugin.profiles().level(p) + ".");
                                }))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> xp() {
        return Commands.literal("xp").then(Commands.literal("add")
                .then(Commands.argument("player", ArgumentTypes.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                .executes(ctx -> {
                                    Player p = Cmd.player(ctx, "player");
                                    plugin.profiles().addXp(p, DoubleArgumentType.getDouble(ctx, "amount"),
                                            PlayerXpGainEvent.Source.COMMAND);
                                    var prof = plugin.profiles().get(p);
                                    return Cmd.ok(Cmd.sender(ctx), p.getName() + ": level " + prof.level()
                                            + ", " + Math.round(prof.xp()) + " XP.");
                                }))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> clazz() {
        return Commands.literal("class").then(Commands.literal("set")
                .then(Commands.argument("player", ArgumentTypes.player())
                        .then(Commands.argument("class", StringArgumentType.word())
                                .suggests(Cmd.suggest(ctx -> {
                                    var ids = new ArrayList<>(plugin.classes().ids());
                                    ids.add("none");
                                    return ids;
                                }))
                                .executes(ctx -> {
                                    Player p = Cmd.player(ctx, "player");
                                    String id = StringArgumentType.getString(ctx, "class");
                                    boolean none = id.equalsIgnoreCase("none");
                                    if (!none && plugin.classes().get(id).isEmpty()) {
                                        return Cmd.fail(Cmd.sender(ctx), "Unknown class: " + id);
                                    }
                                    if (!plugin.profiles().setClass(p, none ? null : id, false)) {
                                        return Cmd.fail(Cmd.sender(ctx), "Class unchanged.");
                                    }
                                    return Cmd.ok(Cmd.sender(ctx), p.getName() + "'s class is now "
                                            + (none ? "none" : id) + ".");
                                }))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> boss() {
        return Commands.literal("boss")
                .then(Commands.literal("list").executes(ctx -> {
                    Cmd.sender(ctx).sendMessage(Component.text("Bosses (" + plugin.bosses().ids().size() + ", "
                                    + plugin.bosses().activeCount() + " alive): ", NamedTextColor.AQUA)
                            .append(Component.text(String.join(", ", plugin.bosses().ids()), NamedTextColor.WHITE)));
                    return Cmd.OK;
                }))
                .then(Commands.literal("killall").executes(ctx ->
                        Cmd.ok(Cmd.sender(ctx), "Removed " + plugin.bosses().killAll() + " loaded boss(es).")))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("boss", StringArgumentType.word())
                                .suggests(Cmd.suggest(ctx -> plugin.bosses().ids()))
                                .executes(ctx -> spawn(ctx, null))
                                .then(Commands.argument("player", ArgumentTypes.player())
                                        .executes(ctx -> spawn(ctx, Cmd.player(ctx, "player"))))));
    }

    private int spawn(CommandContext<CommandSourceStack> ctx, @Nullable Player at) {
        String id = StringArgumentType.getString(ctx, "boss");
        Location loc;
        if (at != null) {
            loc = at.getLocation();
        } else {
            Entity executor = ctx.getSource().getExecutor();
            loc = executor != null ? executor.getLocation() : ctx.getSource().getLocation();
        }
        if (plugin.bosses().get(id).isEmpty()) {
            return Cmd.fail(Cmd.sender(ctx), "Unknown boss: " + id);
        }
        // Spawn a few blocks in front so it doesn't land on top of the player.
        Location spawnAt = loc.clone().add(loc.getDirection().setY(0).normalize().multiply(3));
        if (plugin.bosses().spawn(id, spawnAt) == null) {
            return Cmd.fail(Cmd.sender(ctx), "Could not spawn " + id + ".");
        }
        return Cmd.ok(Cmd.sender(ctx), "Spawned boss " + id + ".");
    }
}

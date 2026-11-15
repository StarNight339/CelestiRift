package com.celestirift.command;

import com.celestirift.CelestiRift;
import com.celestirift.trigger.TriggerType;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Locale;
import java.util.function.Function;

/** Shared helpers for the Brigadier command tree. */
public final class Cmd {

    private Cmd() {}

    public static final int OK = Command.SINGLE_SUCCESS;
    public static final int FAIL = 0;

    public static int ok(CommandSender sender, String msg) {
        sender.sendMessage(Component.text(msg, NamedTextColor.GREEN));
        return OK;
    }

    public static int fail(CommandSender sender, String msg) {
        sender.sendMessage(Component.text(msg, NamedTextColor.RED));
        return FAIL;
    }

    public static CommandSender sender(CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getSender();
    }

    /** The command sender as a player, or null (after telling them) if it's the console. */
    public static @Nullable Player self(CommandContext<CommandSourceStack> ctx, String consoleMsg) {
        if (ctx.getSource().getSender() instanceof Player p) {
            return p;
        }
        fail(ctx.getSource().getSender(), consoleMsg);
        return null;
    }

    /** Resolve a {@code ArgumentTypes.player()} argument. */
    public static Player player(CommandContext<CommandSourceStack> ctx, String arg) throws CommandSyntaxException {
        return ctx.getArgument(arg, PlayerSelectorArgumentResolver.class)
                .resolve(ctx.getSource()).getFirst();
    }

    /** Exact online player by name, or null (after telling the sender). */
    public static @Nullable Player playerNamed(CommandSender sender, String name) {
        Player p = Bukkit.getPlayerExact(name);
        if (p == null) {
            fail(sender, "Player not found: " + name);
        }
        return p;
    }

    // ------------------------------------------------------------------ suggestions

    public static SuggestionProvider<CommandSourceStack> suggest(Function<CommandContext<CommandSourceStack>,
            Collection<String>> values) {
        return (ctx, builder) -> {
            String remaining = builder.getRemainingLowerCase();
            for (String v : values.apply(ctx)) {
                if (v.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                    builder.suggest(v);
                }
            }
            return builder.buildFuture();
        };
    }

    public static SuggestionProvider<CommandSourceStack> skills(CelestiRift plugin) {
        return suggest(ctx -> plugin.skills().skillNames());
    }

    public static SuggestionProvider<CommandSourceStack> triggers() {
        return suggest(ctx -> java.util.Arrays.stream(TriggerType.values()).map(Enum::name).toList());
    }

    public static SuggestionProvider<CommandSourceStack> onlinePlayers() {
        return suggest(ctx -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
    }
}

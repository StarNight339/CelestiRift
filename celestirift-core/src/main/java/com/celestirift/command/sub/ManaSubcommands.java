package com.celestirift.command.sub;

import com.celestirift.CelestiRift;
import com.celestirift.command.Cmd;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** {@code mana [player]} and {@code mana set <player> <amount>}. */
public final class ManaSubcommands {

    private final CelestiRift plugin;

    public ManaSubcommands(CelestiRift plugin) {
        this.plugin = plugin;
    }

    public List<LiteralArgumentBuilder<CommandSourceStack>> build() {
        return List.of(Commands.literal("mana")
                .executes(ctx -> show(ctx, null))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0))
                                        .executes(ctx -> {
                                            Player target = Cmd.player(ctx, "player");
                                            plugin.mana().set(target.getUniqueId(),
                                                    DoubleArgumentType.getDouble(ctx, "amount"));
                                            return Cmd.ok(Cmd.sender(ctx), "Set " + target.getName()
                                                    + "'s mana to " + plugin.mana().get(target.getUniqueId()) + ".");
                                        }))))
                .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(ctx -> show(ctx, Cmd.player(ctx, "player")))));
    }

    private int show(CommandContext<CommandSourceStack> ctx, @Nullable Player target) {
        if (target == null && (target = Cmd.self(ctx, "Usage: /cr mana <player>")) == null) {
            return Cmd.FAIL;
        }
        Cmd.sender(ctx).sendMessage(Component.text(target.getName() + " mana: "
                + String.format("%.0f/%.0f", plugin.mana().get(target.getUniqueId()), plugin.mana().max()),
                NamedTextColor.AQUA));
        return Cmd.OK;
    }
}

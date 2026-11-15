package com.celestirift.command.sub;

import com.celestirift.CelestiRift;
import com.celestirift.command.Cmd;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** {@code /cr pack} (status) and {@code /cr pack send [player]}. */
public final class PackSubcommands {

    private final CelestiRift plugin;

    public PackSubcommands(CelestiRift plugin) {
        this.plugin = plugin;
    }

    public List<LiteralArgumentBuilder<CommandSourceStack>> build() {
        return List.of(Commands.literal("pack")
                .executes(this::status)
                .then(Commands.literal("send")
                        .executes(ctx -> send(ctx, null))
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(ctx -> send(ctx, Cmd.player(ctx, "player"))))));
    }

    private int status(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = Cmd.sender(ctx);
        var pack = plugin.resourcePack();
        sender.sendMessage(Component.text("Resource pack", NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(line("Models", pack.enabled() ? "enabled" : "disabled (resource-pack.enabled)",
                pack.enabled() ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        sender.sendMessage(line("Bundled zip", plugin.getDataFolder().getName() + "/"
                + com.celestirift.pack.ResourcePackService.BUNDLED, NamedTextColor.WHITE));
        String hash = pack.bundledHash();
        if (hash != null) {
            sender.sendMessage(Component.text("  SHA-1: ", NamedTextColor.GRAY)
                    .append(Component.text(hash, NamedTextColor.WHITE)
                            .clickEvent(ClickEvent.copyToClipboard(hash))
                            .hoverEvent(Component.text("Click to copy"))));
        }
        if (pack.managedByOrionRP()) {
            sender.sendMessage(line("Sent by", "OrionRP (merged into the server pack) - see /orp", NamedTextColor.AQUA));
            return Cmd.OK;
        }
        sender.sendMessage(line("Send URL", pack.url().isEmpty() ? "(not set - players aren't sent the pack)"
                : pack.url(), pack.url().isEmpty() ? NamedTextColor.GRAY : NamedTextColor.WHITE));
        return Cmd.OK;
    }

    private static Component line(String label, String value, NamedTextColor color) {
        return Component.text("  " + label + ": ", NamedTextColor.GRAY).append(Component.text(value, color));
    }

    private int send(CommandContext<CommandSourceStack> ctx, @Nullable Player target) {
        if (target == null && (target = Cmd.self(ctx, "Console must specify a player.")) == null) {
            return Cmd.FAIL;
        }
        if (plugin.resourcePack().managedByOrionRP()) {
            return Cmd.fail(Cmd.sender(ctx), "The pack is sent by OrionRP - use /orp send " + target.getName() + ".");
        }
        if (!plugin.resourcePack().send(target)) {
            return Cmd.fail(Cmd.sender(ctx), "Set resource-pack.send.url in config.yml first.");
        }
        return Cmd.ok(Cmd.sender(ctx), "Sent the resource pack to " + target.getName() + ".");
    }
}

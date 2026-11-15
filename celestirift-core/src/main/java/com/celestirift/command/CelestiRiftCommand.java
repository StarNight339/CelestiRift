package com.celestirift.command;

import com.celestirift.CelestiRift;
import com.celestirift.command.sub.GemSubcommands;
import com.celestirift.command.sub.ManaSubcommands;
import com.celestirift.command.sub.SkillSubcommands;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

/**
 * The {@code /celestirift} (aliases {@code /cr}, {@code /skill}, {@code /skills}) Brigadier tree.
 * Subcommands live in {@code command/sub}; each returns its literal nodes.
 */
public final class CelestiRiftCommand {

    public static final String ADMIN_PERMISSION = "celestirift.admin";
    private static final List<String> ALIASES = List.of("cr", "skill", "skills");

    private CelestiRiftCommand() {}

    /** Register the command tree; call from {@code onEnable}. */
    public static void register(CelestiRift plugin) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
                event -> event.registrar().register(build(plugin),
                        "CelestiRift skill framework command.", ALIASES));
    }

    static LiteralCommandNode<CommandSourceStack> build(CelestiRift plugin) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("celestirift")
                .requires(src -> src.getSender().hasPermission(ADMIN_PERMISSION))
                .executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(
                            Component.text("CelestiRift ", NamedTextColor.LIGHT_PURPLE)
                                    .append(Component.text("— /cr <reload|validate|list|info|cast|give|mana|gem|fuse|"
                                            + "shardvoid|socket|trigger|item|pack|level|xp|class|boss|dummy>", NamedTextColor.GRAY)));
                    return Cmd.OK;
                });

        new SkillSubcommands(plugin).build().forEach(root::then);
        new ManaSubcommands(plugin).build().forEach(root::then);
        new GemSubcommands(plugin).build().forEach(root::then);
        new com.celestirift.command.sub.PackSubcommands(plugin).build().forEach(root::then);
        new com.celestirift.command.sub.RpgSubcommands(plugin).build().forEach(root::then);
        new com.celestirift.command.sub.DummySubcommands(plugin).build().forEach(root::then);
        return root.build();
    }
}

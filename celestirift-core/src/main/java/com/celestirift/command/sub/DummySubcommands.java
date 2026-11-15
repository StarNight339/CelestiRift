package com.celestirift.command.sub;

import com.celestirift.CelestiRift;
import com.celestirift.command.Cmd;
import com.celestirift.devtools.TrainingDummies;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * {@code /cr dummy spawn [enemy|ally|player] [health]} and {@code /cr dummy clear}:
 * training-dummy Mannequins for testing skills (damage/heal/DPS on the action bar).
 */
public final class DummySubcommands {

    private final CelestiRift plugin;

    public DummySubcommands(CelestiRift plugin) {
        this.plugin = plugin;
    }

    public List<LiteralArgumentBuilder<CommandSourceStack>> build() {
        var spawn = Commands.literal("spawn").executes(ctx -> spawn(ctx, TrainingDummies.Role.ENEMY, 100));
        for (TrainingDummies.Role role : TrainingDummies.Role.values()) {
            String name = role.name().toLowerCase(Locale.ROOT);
            spawn.then(Commands.literal(name)
                    .executes(ctx -> spawn(ctx, role, 100))
                    .then(Commands.argument("health", DoubleArgumentType.doubleArg(1, 1024))
                            .executes(ctx -> spawn(ctx, role, DoubleArgumentType.getDouble(ctx, "health")))));
        }
        var clear = Commands.literal("clear").executes(ctx ->
                Cmd.ok(Cmd.sender(ctx), "Removed " + plugin.dummies().clear() + " training dummy(ies)."));
        return List.of(Commands.literal("dummy").then(spawn).then(clear));
    }

    private int spawn(CommandContext<CommandSourceStack> ctx, TrainingDummies.Role role, double health) {
        Player p = Cmd.self(ctx, "Only players can spawn dummies (they appear in front of you).");
        if (p == null) {
            return Cmd.FAIL;
        }
        // 3 blocks ahead, facing the player.
        Location at = p.getLocation().add(p.getLocation().getDirection().setY(0).normalize().multiply(3));
        at.setDirection(p.getLocation().toVector().subtract(at.toVector()));
        plugin.dummies().spawn(at, role, health, role == TrainingDummies.Role.PLAYER ? p : null);
        return Cmd.ok(p, "Spawned a " + role.name().toLowerCase(Locale.ROOT) + " dummy (" + (int) health
                + " HP). Hit it with skills - results show on your action bar. /cr dummy clear removes all.");
    }
}

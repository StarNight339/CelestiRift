package com.celestirift.command.sub;

import com.celestirift.CelestiRift;
import com.celestirift.command.Cmd;
import com.celestirift.gem.GemDef;
import com.celestirift.trigger.TriggerType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/** {@code gem, fuse, shardvoid, socket, trigger, item}. */
public final class GemSubcommands {

    private final CelestiRift plugin;

    public GemSubcommands(CelestiRift plugin) {
        this.plugin = plugin;
    }

    public List<LiteralArgumentBuilder<CommandSourceStack>> build() {
        return List.of(gem(), fuse(), shardvoid(), socket(), trigger(), item());
    }

    // ------------------------------------------------------------------ gem

    /**
     * {@code /cr gem} opens the library; {@code /cr gem <GemId> [player]} gives a defined gem;
     * {@code /cr gem <skill> [tier] [trigger] [player]} gives an ad-hoc gem.
     */
    private LiteralArgumentBuilder<CommandSourceStack> gem() {
        var name = Commands.argument("name", StringArgumentType.word())
                .suggests(Cmd.suggest(ctx -> {
                    var out = new TreeSet<>(plugin.gemDefs().ids());
                    out.addAll(plugin.skills().skillNames());
                    return out;
                }))
                .executes(ctx -> giveGem(ctx, null, null, null))
                .then(Commands.argument("arg2", StringArgumentType.word())
                        // Defined gem -> player name; otherwise -> tier 1-5.
                        .suggests(Cmd.suggest(ctx -> isDefined(ctx)
                                ? Bukkit.getOnlinePlayers().stream().map(Player::getName).toList()
                                : List.of("1", "2", "3", "4", "5")))
                        .executes(ctx -> giveGem(ctx, StringArgumentType.getString(ctx, "arg2"), null, null))
                        .then(Commands.argument("trigger", StringArgumentType.word())
                                .suggests(Cmd.triggers())
                                .executes(ctx -> giveGem(ctx, StringArgumentType.getString(ctx, "arg2"),
                                        StringArgumentType.getString(ctx, "trigger"), null))
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests(Cmd.onlinePlayers())
                                        .executes(ctx -> giveGem(ctx, StringArgumentType.getString(ctx, "arg2"),
                                                StringArgumentType.getString(ctx, "trigger"),
                                                StringArgumentType.getString(ctx, "player"))))));
        return Commands.literal("gem")
                .executes(ctx -> {
                    Player player = Cmd.self(ctx, "Usage: /cr gem <gem|skill> [tier] [trigger] [player]");
                    if (player == null) {
                        return Cmd.FAIL;
                    }
                    plugin.openGemLibrary(player);
                    return Cmd.OK;
                })
                .then(name);
    }

    private boolean isDefined(CommandContext<CommandSourceStack> ctx) {
        return plugin.gemDefs().get(StringArgumentType.getString(ctx, "name")).isPresent();
    }

    private int giveGem(CommandContext<CommandSourceStack> ctx, @Nullable String arg2,
                        @Nullable String triggerArg, @Nullable String playerArg) {
        CommandSender sender = Cmd.sender(ctx);
        String name = StringArgumentType.getString(ctx, "name");

        // Prefer a named gem definition from gems.yml: /cr gem <GemId> [player]
        var defined = plugin.gemDefs().get(name);
        if (defined.isPresent()) {
            GemDef def = defined.get();
            if (!def.isPassive() && plugin.skills().getSkill(def.skill()).isEmpty()) {
                return Cmd.fail(sender, "Gem '" + def.id() + "' uses unknown skill '" + def.skill() + "'.");
            }
            Player target = targetOrSelf(ctx, arg2);
            if (target == null) {
                return Cmd.FAIL;
            }
            target.getInventory().addItem(plugin.gems().create(def));
            return Cmd.ok(sender, "Gave gem '" + def.id() + "' to " + target.getName() + ".");
        }

        // Ad-hoc gem: /cr gem <skill> [tier] [trigger] [player]
        if (plugin.skills().getSkill(name).isEmpty()) {
            return Cmd.fail(sender, "No gem or skill named '" + name + "'.");
        }
        int tier = 1;
        if (arg2 != null) {
            try {
                tier = Math.max(1, Math.min(5, Integer.parseInt(arg2)));
            } catch (NumberFormatException e) {
                return Cmd.fail(sender, "Tier must be a number 1-5.");
            }
        }
        TriggerType trigger = TriggerType.RIGHT_CLICK;
        if (triggerArg != null) {
            trigger = TriggerType.fromString(triggerArg);
            if (trigger == null) {
                return Cmd.fail(sender, "Unknown trigger: " + triggerArg);
            }
        }
        Player target = targetOrSelf(ctx, playerArg);
        if (target == null) {
            return Cmd.FAIL;
        }
        target.getInventory().addItem(plugin.gems().create(name, tier, trigger));
        return Cmd.ok(sender, "Gave a tier " + tier + " " + name + " gem to " + target.getName() + ".");
    }

    private @Nullable Player targetOrSelf(CommandContext<CommandSourceStack> ctx, @Nullable String name) {
        if (name != null) {
            return Cmd.playerNamed(Cmd.sender(ctx), name);
        }
        return Cmd.self(ctx, "Console must specify a player.");
    }

    // ------------------------------------------------------------------ fuse

    private LiteralArgumentBuilder<CommandSourceStack> fuse() {
        return Commands.literal("fuse").executes(ctx -> {
            Player player = Cmd.self(ctx, "Only players can fuse gems.");
            return player == null ? Cmd.FAIL : fuse(player);
        });
    }

    private int fuse(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        List<String> ids = new ArrayList<>();
        for (ItemStack it : contents) {
            String id = plugin.gems().getId(it);
            if (id != null && !id.isBlank()) {
                ids.add(id);
            }
        }

        var recipe = plugin.fusions().match(ids);
        if (recipe == null) {
            return Cmd.fail(player, "No matching fusion. Carry the ingredient gems (e.g. EmberGem + DarkGem).");
        }
        // Resolve the output: a defined result-gem wins, else an auto gem from the skill.
        GemDef resultDef = recipe.hasResultGem() ? plugin.gemDefs().get(recipe.resultGem()).orElse(null) : null;
        if (recipe.hasResultGem() && resultDef == null) {
            return Cmd.fail(player, "Result gem '" + recipe.resultGem() + "' is not defined.");
        }
        if (resultDef == null && plugin.skills().getSkill(recipe.skill()).isEmpty()) {
            return Cmd.fail(player, "Fusion skill '" + recipe.skill() + "' does not exist.");
        }

        if (!new com.celestirift.api.event.GemFuseEvent(player, recipe.id(), recipe.ingredients(),
                recipe.hasResultGem() ? recipe.resultGem() : recipe.skill()).callEvent()) {
            return Cmd.FAIL; // cancelled by another plugin
        }

        // Consume one of each ingredient from the inventory.
        List<String> needed = new ArrayList<>(recipe.ingredients()); // lowercase ids
        for (int i = 0; i < contents.length && !needed.isEmpty(); i++) {
            ItemStack it = contents[i];
            String id = plugin.gems().getId(it);
            if (id == null || id.isBlank()) {
                continue;
            }
            if (needed.remove(id.toLowerCase(Locale.ROOT))) {
                it.setAmount(it.getAmount() - 1);
                player.getInventory().setItem(i, it.getAmount() > 0 ? it : null);
            }
        }

        ItemStack fused = (resultDef != null)
                ? plugin.gems().create(resultDef)
                : plugin.gems().create(recipe.skill(), recipe.tier(), recipe.trigger());
        for (ItemStack leftover : player.getInventory().addItem(fused).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        player.playSound(player, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.2f);
        player.sendMessage(Component.text("✦ Fused " + recipe.id() + " → a " + recipe.skill() + " gem!",
                NamedTextColor.LIGHT_PURPLE));
        return Cmd.OK;
    }

    // ------------------------------------------------------------------ shardvoid

    private LiteralArgumentBuilder<CommandSourceStack> shardvoid() {
        return Commands.literal("shardvoid")
                .executes(ctx -> shardvoid(ctx, 1, null))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                        .executes(ctx -> shardvoid(ctx, IntegerArgumentType.getInteger(ctx, "amount"), null))
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(ctx -> shardvoid(ctx, IntegerArgumentType.getInteger(ctx, "amount"),
                                        Cmd.player(ctx, "player")))));
    }

    private int shardvoid(CommandContext<CommandSourceStack> ctx, int amount, @Nullable Player target) {
        if (target == null && (target = Cmd.self(ctx, "Console must specify a player.")) == null) {
            return Cmd.FAIL;
        }
        ItemStack item = plugin.gems().createShardVoid();
        item.setAmount(amount);
        target.getInventory().addItem(item);
        return Cmd.ok(Cmd.sender(ctx), "Gave " + amount + " Shardvoid to " + target.getName() + ".");
    }

    // ------------------------------------------------------------------ menus

    private LiteralArgumentBuilder<CommandSourceStack> socket() {
        return Commands.literal("socket").executes(ctx -> {
            Player player = Cmd.self(ctx, "Only players can open the socket menu.");
            if (player == null) {
                return Cmd.FAIL;
            }
            plugin.openSocketMenu(player);
            return Cmd.OK;
        });
    }

    private LiteralArgumentBuilder<CommandSourceStack> trigger() {
        return Commands.literal("trigger").executes(ctx -> {
            Player player = Cmd.self(ctx, "Only players can open the trigger menu.");
            if (player == null) {
                return Cmd.FAIL;
            }
            plugin.openTriggerMenu(player);
            return Cmd.OK;
        });
    }

    // ------------------------------------------------------------------ item

    private LiteralArgumentBuilder<CommandSourceStack> item() {
        return Commands.literal("item")
                .executes(ctx -> {
                    ItemStack held = held(ctx);
                    if (held == null) {
                        return Cmd.FAIL;
                    }
                    CommandSender sender = Cmd.sender(ctx);
                    var sockets = plugin.sockets().get(held);
                    if (sockets.isEmpty()) {
                        sender.sendMessage(Component.text("That item has no sockets.", NamedTextColor.GRAY));
                        return Cmd.OK;
                    }
                    sender.sendMessage(Component.text("Sockets:", NamedTextColor.GOLD));
                    for (var s : sockets) {
                        sender.sendMessage(Component.text("  ◆ " + s.skill() + " (" + s.trigger().name() + ")",
                                NamedTextColor.WHITE));
                    }
                    return Cmd.OK;
                })
                .then(Commands.literal("clear").executes(ctx -> {
                    ItemStack held = held(ctx);
                    if (held == null) {
                        return Cmd.FAIL;
                    }
                    plugin.sockets().clear(held);
                    return Cmd.ok(Cmd.sender(ctx), "Cleared all sockets.");
                }))
                .then(Commands.literal("settrigger")
                        .then(Commands.argument("skill", StringArgumentType.word())
                                .suggests(Cmd.suggest(ctx -> {
                                    if (ctx.getSource().getSender() instanceof Player p) {
                                        return plugin.sockets().get(p.getInventory().getItemInMainHand()).stream()
                                                .filter(s -> s.isActive()).map(s -> s.skill()).toList();
                                    }
                                    return List.of();
                                }))
                                .then(Commands.argument("trigger", StringArgumentType.word())
                                        .suggests(Cmd.triggers())
                                        .executes(ctx -> {
                                            ItemStack held = held(ctx);
                                            if (held == null) {
                                                return Cmd.FAIL;
                                            }
                                            CommandSender sender = Cmd.sender(ctx);
                                            String skill = StringArgumentType.getString(ctx, "skill");
                                            String raw = StringArgumentType.getString(ctx, "trigger");
                                            TriggerType trigger = TriggerType.fromString(raw);
                                            if (trigger == null) {
                                                return Cmd.fail(sender, "Unknown trigger: " + raw);
                                            }
                                            if (plugin.sockets().retrigger(held, skill, trigger)) {
                                                return Cmd.ok(sender, "Re-bound " + skill + " to " + trigger.name() + ".");
                                            }
                                            return Cmd.fail(sender, "No socket for skill '" + skill + "' on that item.");
                                        }))));
    }

    /** The sender's main-hand item, or null (after telling them) if console/empty. */
    private @Nullable ItemStack held(CommandContext<CommandSourceStack> ctx) {
        Player player = Cmd.self(ctx, "Only players can edit held items.");
        if (player == null) {
            return null;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            Cmd.fail(player, "Hold an item in your main hand.");
            return null;
        }
        return held;
    }
}

package com.celestirift.integration;

import com.celestirift.CelestiRift;
import com.celestirift.api.CastResult;
import dev.orionrp.api.OrionRP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The OrionRP side of {@link Orion}. Loaded only when OrionRP is enabled. Also adds the OrionRP
 * action {@code skill <name>}, so OrionRP items, furniture, blocks and GUI buttons can cast
 * CelestiRift skills (with mana, cooldown and conditions, like any player cast).
 */
final class OrionBridge implements Orion.Bridge {

    private final OrionRP api;

    OrionBridge(CelestiRift plugin) {
        this.api = OrionRP.get();
        api.registerAction(plugin, "skill", (player, arg) -> {
            String name = arg.trim();
            if (name.isEmpty()) {
                plugin.getLogger().warning("OrionRP action 'skill' needs a skill name: skill <name>");
                return;
            }
            CastResult result = plugin.casts().cast(player, name, null);
            plugin.debug(() -> "OrionRP action: skill " + name + " by " + player.getName() + " -> " + result);
        });
    }

    @Override
    public TagResolver tags() {
        return api.glyphTags();
    }

    @Override
    public @Nullable Component glyph(String nameOrId) {
        return api.glyph(nameOrId);
    }

    @Override
    public @Nullable ItemStack item(String id, int amount) {
        return api.createItem(id, amount);
    }

    @Override
    public @Nullable String itemId(@Nullable ItemStack item) {
        return api.itemId(item);
    }

    @Override
    public @Nullable Component guiTitle(String background, Component title) {
        return api.guiTitle(background, title);
    }

    @Override
    public void hud(Player player, String layer, @Nullable Component content, int x, int ticks) {
        api.hud(player, layer, content, x, ticks);
    }

    @Override
    public int width(Component text) {
        return api.textWidth(text);
    }
}

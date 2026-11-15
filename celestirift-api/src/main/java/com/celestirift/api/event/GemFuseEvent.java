package com.celestirift.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Fired before a fusion recipe consumes its ingredient gems (socket menu or
 * {@code /cr fuse}). Cancel to block the fusion; nothing is consumed.
 */
public final class GemFuseEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String recipeId;
    private final List<String> ingredients;
    private final String result;
    private boolean cancelled;

    public GemFuseEvent(Player player, String recipeId, List<String> ingredients, String result) {
        super(player);
        this.recipeId = recipeId;
        this.ingredients = List.copyOf(ingredients);
        this.result = result;
    }

    /** @return the fusions.yml recipe id */
    public String getRecipeId() { return recipeId; }

    /** @return ingredient gem ids (lowercase) */
    public List<String> getIngredients() { return ingredients; }

    /** @return the result gem id or skill name */
    public String getResult() { return result; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    /** @return the handler list (Bukkit requirement) */
    public static HandlerList getHandlerList() { return HANDLERS; }
}

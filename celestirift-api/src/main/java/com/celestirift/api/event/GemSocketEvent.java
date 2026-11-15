package com.celestirift.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired once per gem about to be socketed into a weapon or armor piece in the
 * socket menu. Cancel to reject that gem (it stays in the menu).
 */
public final class GemSocketEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ItemStack equipment;
    private final String gemId;
    private final boolean passive;
    private final @Nullable String skill;
    private boolean cancelled;

    public GemSocketEvent(Player player, ItemStack equipment, String gemId, boolean passive, @Nullable String skill) {
        super(player);
        this.equipment = equipment;
        this.gemId = gemId;
        this.passive = passive;
        this.skill = skill;
    }

    /** @return the weapon/armor receiving the gem (do not modify) */
    public ItemStack getEquipment() { return equipment; }

    /** @return the gems.yml id, or empty for ad-hoc gems */
    public String getGemId() { return gemId; }

    /** @return true for passive runes */
    public boolean isPassive() { return passive; }

    /** @return the granted skill for active gems, null for passive runes */
    public @Nullable String getSkill() { return skill; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    /** @return the handler list (Bukkit requirement) */
    public static HandlerList getHandlerList() { return HANDLERS; }
}

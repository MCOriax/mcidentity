package io.gitlab.mcoriax.bukkit.event;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class MCIdentityPreEquipEvent extends MCIdentityEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final ItemStack itemToEquip;
    private final boolean isInteract;
    private boolean cancelled;
    private Component cancelReason;

    public MCIdentityPreEquipEvent(Player player, ItemStack itemToEquip, boolean isInteract) {
        this.player = player;
        this.itemToEquip = itemToEquip;
        this.isInteract = isInteract;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getItemToEquip() {
        return itemToEquip;
    }

    public boolean isInteract() {
        return isInteract;
    }

    public Component getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(Component cancelReason) {
        this.cancelReason = cancelReason;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

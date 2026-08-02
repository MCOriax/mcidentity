package io.github.mcoriax.mcidentity.bukkit.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class MCIdentityPreEquipEvent extends MCIdentityEvent implements Cancellable {
    /**
     * The handlers for this event.
     */
    private static final HandlerList handlers = new HandlerList();
    /**
     * The player attempting to equip the identity.
     */
    private final Player player;
    /**
     * The item representing the identity to be equipped.
     */
    private final ItemStack itemToEquip;
    /**
     * Whether the equip was triggered by an interaction (true) or GUI (false).
     */
    private final boolean isInteract;
    /**
     * Whether the event is cancelled.
     */
    private boolean cancelled;
    /**
     * The reason for cancellation, if any.
     */
    private String cancelReason;

    /**
     * Creates a new MCIdentityPreEquipEvent.
     * @param player The player involved.
     * @param itemToEquip The item being equipped.
     * @param isInteract Whether it was an interaction.
     */
    public MCIdentityPreEquipEvent(Player player, ItemStack itemToEquip, boolean isInteract) {
        this.player = player;
        this.itemToEquip = itemToEquip;
        this.isInteract = isInteract;
    }

    /**
     * Gets the player involved in this event.
     * @return The player.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the item being equipped.
     * @return The item to equip.
     */
    public ItemStack getItemToEquip() {
        return itemToEquip;
    }

    /**
     * Checks if the event was triggered by an interaction.
     * @return true if interaction, false if GUI.
     */
    public boolean isInteract() {
        return isInteract;
    }

    /**
     * Gets the reason for cancellation.
     * @return The cancel reason.
     */
    public String getCancelReason() {
        return cancelReason;
    }

    /**
     * Sets the reason for cancellation.
     * @param cancelReason The cancel reason.
     */
    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    /**
     * Gets whether this event has been cancelled by another plugin.
     * @return True when the action will not be carried out.
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets whether this event is cancelled.
     * @param cancel True to stop the action from being carried out.
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    /**
     * Gets the handlers for this event.
     * @return The handler list.
     */
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    /**
     * Gets the handler list for this event.
     * @return The handler list.
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }
}

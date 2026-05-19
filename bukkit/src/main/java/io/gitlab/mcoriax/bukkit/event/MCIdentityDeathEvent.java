package io.gitlab.mcoriax.bukkit.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Called when a player with an active identity dies and drops their identity key.
 */
public class MCIdentityDeathEvent extends MCIdentityEvent {

    /**
     * The handlers for this event.
     */
    private static final HandlerList HANDLERS = new HandlerList();
    /**
     * The player who died.
     */
    private final Player player;
    /**
     * The unique ID of the identity being worn.
     */
    private final UUID identityUuid;
    /**
     * The item representing the identity key to be dropped.
     */
    private final ItemStack droppedItem;
    /**
     * Whether the identity key should be dropped.
     */
    private boolean drop = false;

    /**
     * Creates a new MCIdentityDeathEvent.
     * @param player The player involved.
     * @param identityUuid The identity UUID.
     * @param droppedItem The item to be dropped.
     */
    public MCIdentityDeathEvent(Player player, UUID identityUuid, ItemStack droppedItem) {
        this.player = player;
        this.identityUuid = identityUuid;
        this.droppedItem = droppedItem;
    }

    /**
     * Gets the player involved in this event.
     * @return The player.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the unique ID of the identity.
     * @return The identity UUID.
     */
    public UUID getIdentityUuid() {
        return identityUuid;
    }

    /**
     * Gets the item to be dropped.
     * @return The dropped item.
     */
    public ItemStack getDroppedItem() {
        return droppedItem;
    }

    /**
     * Checks if the identity key should be dropped.
     * @return true if it should drop, false otherwise.
     */
    public boolean isDrop() {
        return drop;
    }

    /**
     * Sets whether the identity key should be dropped.
     * @param drop true to drop, false to keep.
     */
    public void setDrop(boolean drop) {
        this.drop = drop;
    }

    /**
     * Gets the handlers for this event.
     * @return The handler list.
     */
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Gets the handler list for this event.
     * @return The handler list.
     */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}

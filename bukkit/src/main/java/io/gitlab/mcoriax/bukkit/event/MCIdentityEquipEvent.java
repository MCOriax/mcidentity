package io.gitlab.mcoriax.bukkit.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Called when a player equips an identity.
 */
public class MCIdentityEquipEvent extends MCIdentityEvent implements Cancellable {

    /**
     * The handlers for this event.
     */
    private static final HandlerList HANDLERS = new HandlerList();
    /**
     * The player equipping the identity.
     */
    private final Player player;
    /**
     * The unique ID of the identity being equipped.
     */
    private final UUID identityUuid;
    /**
     * The item representing the identity.
     */
    private final ItemStack identityItem;
    /**
     * Whether the event is cancelled.
     */
    private boolean cancelled;

    /**
     * Creates a new MCIdentityEquipEvent.
     * @param player The player involved.
     * @param identityUuid The identity UUID.
     * @param identityItem The identity item.
     */
    public MCIdentityEquipEvent(Player player, UUID identityUuid, ItemStack identityItem) {
        this.player = player;
        this.identityUuid = identityUuid;
        this.identityItem = identityItem;
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
     * Gets the identity item being equipped.
     * @return The identity item.
     */
    public ItemStack getIdentityItem() {
        return identityItem;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
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

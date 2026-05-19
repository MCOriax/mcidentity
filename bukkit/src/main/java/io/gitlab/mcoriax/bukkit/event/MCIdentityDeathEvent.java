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

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final UUID identityUuid;
    private final ItemStack droppedItem;
    private boolean drop = false;

    public MCIdentityDeathEvent(Player player, UUID identityUuid, ItemStack droppedItem) {
        this.player = player;
        this.identityUuid = identityUuid;
        this.droppedItem = droppedItem;
    }

    public Player getPlayer() {
        return player;
    }

    public UUID getIdentityUuid() {
        return identityUuid;
    }

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

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}

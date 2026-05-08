package io.gitlab.mcoriax.bukkit.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Called when a player with an active identity dies and drops their identity key.
 */
public class IdentityDeathEvent extends MCIdentityEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final UUID identityUuid;
    private final ItemStack droppedItem;

    public IdentityDeathEvent(Player player, UUID identityUuid, ItemStack droppedItem) {
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

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}

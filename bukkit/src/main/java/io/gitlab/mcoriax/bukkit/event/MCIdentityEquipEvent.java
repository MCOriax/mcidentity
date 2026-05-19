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

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final UUID identityUuid;
    private final ItemStack identityItem;
    private boolean cancelled;

    public MCIdentityEquipEvent(Player player, UUID identityUuid, ItemStack identityItem) {
        this.player = player;
        this.identityUuid = identityUuid;
        this.identityItem = identityItem;
    }

    public Player getPlayer() {
        return player;
    }

    public UUID getIdentityUuid() {
        return identityUuid;
    }

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

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}

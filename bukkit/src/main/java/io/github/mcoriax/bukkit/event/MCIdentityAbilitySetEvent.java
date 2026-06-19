package io.github.mcoriax.bukkit.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Called when an identity's ability is set or cleared.
 */
public class MCIdentityAbilitySetEvent extends MCIdentityEvent implements Cancellable {

    /**
     * The handlers for this event.
     */
    private static final HandlerList HANDLERS = new HandlerList();
    /**
     * The player whose identity ability is being set.
     */
    private final Player player;
    /**
     * The unique ID of the identity.
     */
    private final UUID identityUuid;
    /**
     * The slot index where the ability is being set.
     */
    private final int slot;
    /**
     * The code of the ability being set.
     */
    private final String abilityCode;
    /**
     * Whether the event is cancelled.
     */
    private boolean cancelled;

    /**
     * Creates a new MCIdentityAbilitySetEvent.
     * @param player The player involved.
     * @param identityUuid The identity UUID.
     * @param slot The slot index.
     * @param abilityCode The ability code (null if clearing).
     */
    public MCIdentityAbilitySetEvent(Player player, UUID identityUuid, int slot, @Nullable String abilityCode) {
        this.player = player;
        this.identityUuid = identityUuid;
        this.slot = slot;
        this.abilityCode = abilityCode;
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
     * Gets the slot index where the ability is being set.
     * @return The slot index.
     */
    public int getSlot() {
        return slot;
    }

    /**
     * Gets the ability code being set.
     * @return The ability code, or null if clearing the slot.
     */
    public @Nullable String getAbilityCode() {
        return abilityCode;
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

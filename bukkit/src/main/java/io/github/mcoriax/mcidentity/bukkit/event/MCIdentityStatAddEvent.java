package io.github.mcoriax.mcidentity.bukkit.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Called when a specific stat is added/incremented for an identity.
 */
public class MCIdentityStatAddEvent extends MCIdentityEvent implements Cancellable {

    /**
     * The handlers for this event.
     */
    private static final HandlerList HANDLERS = new HandlerList();
    /**
     * The player involved in the stat change, if any.
     */
    private final Player player;
    /**
     * The unique ID of the identity.
     */
    private final UUID identityUuid;
    /**
     * The key of the stat being modified.
     */
    private final String statKey;
    /**
     * The amount being added to the stat.
     */
    private final int amount;
    /**
     * Whether the event is cancelled.
     */
    private boolean cancelled;

    /**
     * Creates a new MCIdentityStatAddEvent.
     * @param player The player involved (nullable).
     * @param identityUuid The identity UUID.
     * @param statKey The stat key.
     * @param amount The amount to add.
     */
    public MCIdentityStatAddEvent(@Nullable Player player, UUID identityUuid, String statKey, int amount) {
        this.player = player;
        this.identityUuid = identityUuid;
        this.statKey = statKey;
        this.amount = amount;
    }

    /**
     * Gets the player involved in this stat change, if any.
     * @return The player, or null if the change was triggered without a direct player context.
     */
    public @Nullable Player getPlayer() {
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
     * Gets the key of the stat being modified (e.g., "strength").
     * @return The stat key.
     */
    public String getStatKey() {
        return statKey;
    }

    /**
     * Gets the amount being added to the stat.
     * @return The amount (can be negative).
     */
    public int getAmount() {
        return amount;
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

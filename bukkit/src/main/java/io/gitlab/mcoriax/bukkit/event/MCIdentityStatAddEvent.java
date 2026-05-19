package io.gitlab.mcoriax.bukkit.event;

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

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final UUID identityUuid;
    private final String statKey;
    private final int amount;
    private boolean cancelled;

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

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}

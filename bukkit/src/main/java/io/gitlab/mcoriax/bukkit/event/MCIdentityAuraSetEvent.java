package io.gitlab.mcoriax.bukkit.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Called when an identity's aura cosmetic is set or cleared.
 */
public class MCIdentityAuraSetEvent extends MCIdentityEvent implements Cancellable {

    /**
     * The handlers for this event.
     */
    private static final HandlerList HANDLERS = new HandlerList();
    /**
     * The player whose identity aura is being set.
     */
    private final Player player;
    /**
     * The unique ID of the identity.
     */
    private final UUID identityUuid;
    /**
     * The aura system code being set.
     */
    private final String aura;
    /**
     * Whether the event is cancelled.
     */
    private boolean cancelled;

    /**
     * Creates a new MCIdentityAuraSetEvent.
     * @param player The player involved.
     * @param identityUuid The identity UUID.
     * @param aura The aura code (null if clearing).
     */
    public MCIdentityAuraSetEvent(Player player, UUID identityUuid, @Nullable String aura) {
        this.player = player;
        this.identityUuid = identityUuid;
        this.aura = aura;
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
     * Gets the aura system code being set.
     * @return The aura code, or null if clearing.
     */
    public @Nullable String getAura() {
        return aura;
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

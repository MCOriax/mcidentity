package io.github.mcoriax.mcidentity.bukkit.event;

import io.github.mcoriax.mcidentity.api.model.IdentityKey;
import io.github.mcoriax.mcidentity.api.model.IdentityProperties;
import io.github.mcoriax.mcidentity.api.model.IdentityStats;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a new identity is created.
 */
public class MCIdentityCreationEvent extends MCIdentityEvent {

    /**
     * The handlers for this event.
     */
    private static final HandlerList HANDLERS = new HandlerList();
    /**
     * The identity key of the newly created identity.
     */
    private final IdentityKey identityKey;
    /**
     * The properties of the newly created identity.
     */
    private final IdentityProperties properties;
    /**
     * The base stats of the newly created identity.
     */
    private final IdentityStats stats;

    /**
     * Creates a new MCIdentityCreationEvent.
     * @param identityKey The identity key.
     * @param properties The identity properties.
     * @param stats The identity stats.
     */
    public MCIdentityCreationEvent(IdentityKey identityKey, IdentityProperties properties, IdentityStats stats) {
        this.identityKey = identityKey;
        this.properties = properties;
        this.stats = stats;
    }

    /**
     * Gets the identity key.
     * @return The identity key.
     */
    public IdentityKey getIdentityKey() {
        return identityKey;
    }

    /**
     * Gets the identity properties.
     * @return The identity properties.
     */
    public IdentityProperties getProperties() {
        return properties;
    }

    /**
     * Gets the identity stats.
     * @return The identity stats.
     */
    public IdentityStats getStats() {
        return stats;
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

package io.gitlab.mcoriax.bukkit.event;

import io.gitlab.mcoriax.api.model.IdentityKey;
import io.gitlab.mcoriax.api.model.IdentityProperties;
import io.gitlab.mcoriax.api.model.IdentityStats;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a new identity is created.
 */
public class MCIdentityCreationEvent extends MCIdentityEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final IdentityKey identityKey;
    private final IdentityProperties properties;
    private final IdentityStats stats;

    public MCIdentityCreationEvent(IdentityKey identityKey, IdentityProperties properties, IdentityStats stats) {
        this.identityKey = identityKey;
        this.properties = properties;
        this.stats = stats;
    }

    public IdentityKey getIdentityKey() {
        return identityKey;
    }

    public IdentityProperties getProperties() {
        return properties;
    }

    public IdentityStats getStats() {
        return stats;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}

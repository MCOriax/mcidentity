package io.github.mcoriax.api.model;

import java.util.UUID;

/**
 * Represents an active identity session for a player.
 */
public interface IdentitySession {
    /**
     * Gets the UUID of the player.
     * @return The player UUID.
     */
    UUID getPlayerUniqueId();

    /**
     * Gets the UUID of the active identity.
     * @return The identity UUID.
     */
    UUID getIdentityUniqueId();

    /**
     * Gets the content key (serialized item).
     * @return The content key.
     */
    String getContentKey();
}

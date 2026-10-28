package io.github.mcoriax.mcidentity.common.model;

import io.github.mcoriax.mcidentity.api.model.IdentitySession;

import java.util.UUID;

/**
 * Immutable implementation of {@link IdentitySession}, as every storage backend
 * reads a session row back.
 *
 * @param playerUniqueId The player wearing the identity.
 * @param identityUniqueId The identity worn.
 * @param contentKey The serialized identity item, or null when there is none.
 */
public record IdentitySessionRecord(UUID playerUniqueId, UUID identityUniqueId, String contentKey)
        implements IdentitySession {

    /** {@inheritDoc} */
    @Override
    public UUID getPlayerUniqueId() {
        return playerUniqueId;
    }

    /** {@inheritDoc} */
    @Override
    public UUID getIdentityUniqueId() {
        return identityUniqueId;
    }

    /** {@inheritDoc} */
    @Override
    public String getContentKey() {
        return contentKey;
    }
}

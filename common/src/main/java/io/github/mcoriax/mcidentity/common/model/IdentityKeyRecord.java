package io.github.mcoriax.mcidentity.common.model;

import io.github.mcoriax.mcidentity.api.model.IdentityKey;

import java.util.UUID;

/**
 * Immutable implementation of {@link IdentityKey}.
 *
 * <p>Replaces the anonymous key views every storage backend and the provider
 * used to build on their own.</p>
 *
 * @param uniqueId The identity UUID.
 * @param professionCode The profession system code.
 */
public record IdentityKeyRecord(UUID uniqueId, String professionCode) implements IdentityKey {

    /** {@inheritDoc} */
    @Override
    public UUID getUniqueId() {
        return uniqueId;
    }

    /** {@inheritDoc} */
    @Override
    public String getProfessionCode() {
        return professionCode;
    }
}

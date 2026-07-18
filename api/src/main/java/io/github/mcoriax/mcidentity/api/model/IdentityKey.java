package io.github.mcoriax.mcidentity.api.model;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.List;

/**
 * Represents a unique Identity Key item.
 */
public interface IdentityKey {
    /**
     * Gets the unique identifier of this identity key.
     * @return The UUID of the key.
     */
    UUID getUniqueId();

    /**
     * Gets the system code of the profession associated with this key.
     * @return The profession system code.
     */
    String getProfessionCode();
}

package io.github.mcoriax.mcidentity.tools;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * What the identity tools need from the running server, as plain data.
 *
 * <p>Implemented by the platform module, so this module stays free of every
 * Bukkit, Paper, and Folia type. Implementations must not block the calling
 * thread and must not touch a player entity off its owning thread.</p>
 */
public interface IdentityToolPort {

    /**
     * Checks whether a player is online on this server.
     * @param playerUuid The player's UUID.
     * @return A future holding true when the player is online.
     */
    CompletableFuture<Boolean> isOnlinePlayer(UUID playerUuid);

    /**
     * Reads the player's active identity.
     * @param playerUuid The player's UUID.
     * @return A future holding the active identity, or empty when none is equipped.
     */
    CompletableFuture<Optional<IdentitySnapshot>> activeIdentity(UUID playerUuid);
}

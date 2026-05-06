package io.gitlab.mcoriax.api.db;

import io.gitlab.mcoriax.api.model.*;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for Identity database operations.
 */
public interface IMCIdentityDB {

    /**
     * Retrieves an identity key by its UUID.
     * @param uuid The UUID of the identity key.
     * @return A CompletableFuture containing the identity key.
     */
    CompletableFuture<Optional<IdentityKey>> getIdentityKey(UUID uuid);

    /**
     * Retrieves properties for an identity.
     * @param identityUuid The UUID of the identity.
     * @return A CompletableFuture containing the properties.
     */
    CompletableFuture<Optional<IdentityProperties>> getProperties(UUID identityUuid);

    /**
     * Retrieves stats for an identity.
     * @param identityUuid The UUID of the identity.
     * @return A CompletableFuture containing the stats.
     */
    CompletableFuture<Optional<IdentityStats>> getStats(UUID identityUuid);

    /**
     * Retrieves a specific stat for an identity.
     * @param identityUuid The UUID of the identity.
     * @param statKey The system code of the stat.
     * @return A CompletableFuture containing the stat value.
     */
    CompletableFuture<Integer> getStat(UUID identityUuid, String statKey);

    /**
     * Retrieves equipped abilities for an identity.
     * @param identityUuid The UUID of the identity.
     * @return A CompletableFuture containing the equipped abilities map.
     */
    CompletableFuture<Map<Integer, String>> getEquippedAbilities(UUID identityUuid);

    /**
     * Retrieves runes for an identity.
     * @param identityUuid The UUID of the identity.
     * @return A CompletableFuture containing the runes map.
     */
    CompletableFuture<Map<Integer, String>> getRunes(UUID identityUuid);

    /**
     * Gets the active identity for a player.
     * @param playerUuid The UUID of the player.
     * @return A CompletableFuture containing the active identity UUID.
     */
    CompletableFuture<Optional<UUID>> getActiveIdentity(UUID playerUuid);

    /**
     * Logs an action for an identity.
     * @param playerUuid The UUID of the player.
     * @param identityUuid The UUID of the identity.
     * @param actionType The type of action.
     * @return A CompletableFuture that completes when logged.
     */
    CompletableFuture<Void> logAction(UUID playerUuid, UUID identityUuid, ActionType actionType);

    /**
     * Updates properties for an identity.
     * @param identityUuid The UUID of the identity.
     * @param properties The new properties.
     * @return A CompletableFuture that completes when updated.
     */
    CompletableFuture<Void> updateProperties(UUID identityUuid, IdentityProperties properties);

    /**
     * Adds a value to a specific stat.
     * @param identityUuid The UUID of the identity.
     * @param statKey The system code of the stat.
     * @param amount The amount to add.
     * @return A CompletableFuture that completes when updated.
     */
    CompletableFuture<Void> addStat(UUID identityUuid, String statKey, int amount);
}

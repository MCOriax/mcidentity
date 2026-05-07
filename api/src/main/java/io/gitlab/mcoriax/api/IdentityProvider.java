package io.gitlab.mcoriax.api;

import io.gitlab.mcoriax.api.model.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.Optional;

/**
 * Main API service for managing identities.
 */
public interface IdentityProvider {

    /**
     * Retrieves an identity key by its UUID.
     * @param uuid The unique ID of the identity key item.
     * @return A CompletableFuture containing an Optional with the IdentityKey if found.
     */
    CompletableFuture<Optional<IdentityKey>> getIdentityKey(UUID uuid);

    /**
     * Retrieves properties for a specific identity.
     * @param identityUuid The UUID of the identity.
     * @return A CompletableFuture containing an Optional with the IdentityProperties if found.
     */
    CompletableFuture<Optional<IdentityProperties>> getProperties(UUID identityUuid);

    /**
     * Retrieves stats for a specific identity.
     * @param identityUuid The UUID of the identity.
     * @return A CompletableFuture containing an Optional with the IdentityStats if found.
     */
    CompletableFuture<Optional<IdentityStats>> getStats(UUID identityUuid);

    /**
     * Retrieves equipped abilities for an identity.
     * @param identityUuid The UUID of the identity.
     * @return A CompletableFuture containing a map of slot number to ability system code.
     */
    CompletableFuture<Map<Integer, String>> getEquippedAbilities(UUID identityUuid);

    /**
     * Retrieves permanent runes for an identity.
     * @param identityUuid The UUID of the identity.
     * @return A CompletableFuture containing a map of slot number to rune system code.
     */
    CompletableFuture<Map<Integer, String>> getRunes(UUID identityUuid);

    /**
     * Gets the currently equipped identity for a player.
     * @param playerUuid The UUID of the player.
     * @return A CompletableFuture containing an Optional with the active identity UUID if any.
     */
    CompletableFuture<Optional<UUID>> getActiveIdentity(UUID playerUuid);

    /**
     * Records an action in the audit trail.
     * @param playerUuid The UUID of the player performing the action.
     * @param identityUuid The UUID of the identity key involved.
     * @param actionType The type of action performed.
     * @return A CompletableFuture that completes when the action is logged.
     */
    CompletableFuture<Void> logAction(UUID playerUuid, UUID identityUuid, ActionType actionType);

    /**
     * Updates the properties for a specific identity.
     * @param identityUuid The UUID of the identity.
     * @param properties The new properties to save.
     * @return A CompletableFuture that completes when the update is finished.
     */
    CompletableFuture<Void> updateProperties(UUID identityUuid, IdentityProperties properties);

    /**
     * Increments a specific stat for an identity.
     * @param identityUuid The UUID of the identity.
     * @param statKey The system name of the stat (e.g., "strength").
     * @param amount The amount to add to the current value.
     * @return A CompletableFuture that completes when the update is finished.
     */
    CompletableFuture<Void> addStat(UUID identityUuid, String statKey, int amount);

    /**
     * Retrieves a specific stat for an identity.
     * @param identityUuid The UUID of the identity.
     * @param statKey The system name of the stat.
     * @return A CompletableFuture containing the current value of the stat.
     */
    CompletableFuture<Integer> getStat(UUID identityUuid, String statKey);
}

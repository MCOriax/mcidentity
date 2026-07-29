package io.github.mcoriax.mcidentity.api.db;

import io.github.mcoriax.mcidentity.api.model.*;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for Identity database operations.
 *
 * <p>Base profession contract: every player owns a permanent "player" base
 * identity whose UUID equals the player's own UUID. Implementations must treat
 * these rows as undeletable — no operation on this interface removes an
 * identity, and none may be added that would allow the base identity to be
 * deleted or exchanged for an identity key item. Players may additionally
 * unlock alternative base identities; the number of unlocked slots is tracked
 * per player through {@link #getBaseIdentitySlots(UUID)} and
 * {@link #addBaseIdentitySlots(UUID, int)}, and the alternative identity rows
 * themselves are ordinary identities keyed by deterministic UUIDs.</p>
 */
public interface IMCIdentityDB {

    /**
     * Initializes the database schema (creates tables if they don't exist).
     * @return A CompletableFuture that completes when initialization is finished.
     */
    CompletableFuture<Void> initializeSchema();

    /**
     * Creates a new profession type.
     * @param systemCode The unique system code.
     * @param displayName The display name.
     * @return A CompletableFuture that completes when created.
     */
    CompletableFuture<Void> createProfession(String systemCode, String displayName);

    /**
     * Retrieves all available professions.
     * @return A CompletableFuture containing a collection of professions.
     */
    CompletableFuture<Collection<Profession>> getProfessions();

    /**
     * Creates a new identity key.
     * <p>
     * A key whose UUID equals a player's UUID is that player's permanent base
     * "player" identity: it is bound to the player forever and can never be
     * deleted or converted into an item.
     * </p>
     * @param key The identity key details.
     * @param properties The initial properties.
     * @param stats The initial stats.
     * @return A CompletableFuture that completes when created.
     */
    CompletableFuture<Void> createIdentity(IdentityKey key, IdentityProperties properties, IdentityStats stats);

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
     * Gets the active identity for a player.
     * @param playerUuid The UUID of the player.
     * @return A CompletableFuture containing the active identity UUID.
     */
    CompletableFuture<Optional<UUID>> getActiveIdentity(UUID playerUuid);

    /**
     * Gets the full session info for a player.
     * @param playerUuid The UUID of the player.
     * @return A CompletableFuture containing the session info.
     */
    CompletableFuture<Optional<IdentitySession>> getSession(UUID playerUuid);

    /**
     * Sets the active identity for a player.
     * @param playerUuid The UUID of the player.
     * @param identityUuid The UUID of the identity, or null to clear.
     * @param contentKey The serialized item stack for quick dropping.
     * @return A CompletableFuture that completes when updated.
     */
    CompletableFuture<Void> setActiveIdentity(UUID playerUuid, UUID identityUuid, String contentKey);

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
     * Updates stats for an identity.
     * @param identityUuid The UUID of the identity.
     * @param stats The new stats.
     * @return A CompletableFuture that completes when updated.
     */
    CompletableFuture<Void> updateStats(UUID identityUuid, IdentityStats stats);

    /**
     * Adds a value to a specific stat.
     * @param identityUuid The UUID of the identity.
     * @param statKey The system code of the stat.
     * @param amount The amount to add.
     * @return A CompletableFuture that completes when updated.
     */
    CompletableFuture<Void> addStat(UUID identityUuid, String statKey, int amount);

    /**
     * Updates the content key (serialized item stack) for a player session.
     * @param playerUuid The UUID of the player.
     * @param contentKey The new serialized item stack.
     * @return A CompletableFuture that completes when updated.
     */
    CompletableFuture<Void> updateSessionContentKey(UUID playerUuid, String contentKey);

    /**
     * Gets the number of unlocked alternative base "player" identities a
     * player owns. The primary base identity is always available and is not
     * counted here.
     * @param playerUuid The UUID of the player.
     * @return A CompletableFuture containing the unlocked slot count (0 when
     *         the player has never been granted any).
     */
    CompletableFuture<Integer> getBaseIdentitySlots(UUID playerUuid);

    /**
     * Adds to the number of unlocked alternative base "player" identities a
     * player owns. Negative amounts reduce the count; the stored value never
     * drops below zero.
     * @param playerUuid The UUID of the player.
     * @param amount The signed amount of slots to add.
     * @return A CompletableFuture containing the new unlocked slot count.
     */
    CompletableFuture<Integer> addBaseIdentitySlots(UUID playerUuid, int amount);

    /**
     * Gracefully shuts down the database connection pool or client.
     */
    void shutdown();
}

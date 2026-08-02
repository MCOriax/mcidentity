package io.github.mcoriax.mcidentity.api;

import io.github.mcoriax.mcidentity.api.model.*;
import java.util.Collection;
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
     * Resolves the identity currently holding an alias.
     * <p>
     * An alias is the optional, player chosen name of a single identity. It is
     * unique across every identity on the network and is matched without regard
     * to case, so any casing of a claimed alias resolves to the same identity.
     * @param alias The alias to resolve.
     * @return A CompletableFuture containing an Optional with the identity UUID
     *         if the alias is claimed.
     */
    CompletableFuture<Optional<UUID>> getIdentityByAlias(String alias);

    /**
     * Claims an alias for a specific identity.
     * <p>
     * The alias may contain only alphanumeric characters and must not already
     * be held by another identity, compared without regard to case. It is
     * stored with the casing supplied here.
     * @param identityUuid The UUID of the identity.
     * @param alias The alias to claim, or null to release the current one.
     * @return A CompletableFuture containing true when the alias was applied,
     *         and false when it is malformed or already taken.
     */
    CompletableFuture<Boolean> setIdentityAlias(UUID identityUuid, String alias);

    /**
     * Gets the currently equipped identity for a player.
     * @param playerUuid The UUID of the player.
     * @return A CompletableFuture containing an Optional with the active identity UUID if any.
     */
    CompletableFuture<Optional<UUID>> getActiveIdentity(UUID playerUuid);

    /**
     * Gets the profession system code of the player's currently active identity.
     * <p>
     * This is a synchronous, RAM-only lookup backed by the active session cache,
     * intended for hot-path checks such as region profession whitelists or
     * blacklists (e.g., deciding whether a profession may enter an area). It
     * returns the profession code of the identity the player is currently
     * playing as, or empty when the player has no active session cached (for
     * example, offline or between identity swaps).
     * @param playerUuid The UUID of the player.
     * @return An Optional containing the active profession system code, or empty if none.
     */
    Optional<String> getActiveProfession(UUID playerUuid);

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

    /**
     * Creates a new profession type.
     * @param systemCode The system code for the profession.
     * @param displayName The display name for the profession.
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
     * @param key The identity key.
     * @param properties The identity properties.
     * @param stats The identity stats.
     * @return A CompletableFuture that completes when created.
     */
    CompletableFuture<Void> createIdentity(IdentityKey key, IdentityProperties properties, IdentityStats stats);

    /**
     * Gets the full session info for a player.
     * @param playerUuid The UUID of the player.
     * @return A CompletableFuture containing an Optional with the IdentitySession if found.
     */
    CompletableFuture<Optional<IdentitySession>> getSession(UUID playerUuid);

    /**
     * Sets the active identity for a player.
     * @param playerUuid The UUID of the player.
     * @param identityUuid The UUID of the identity.
     * @param contentKey The serialized content key.
     * @return A CompletableFuture that completes when set.
     */
    CompletableFuture<Void> setActiveIdentity(UUID playerUuid, UUID identityUuid, String contentKey);

    /**
     * Updates the content key for a player's session in the database and RAM.
     * @param playerUuid The UUID of the player.
     * @param contentKey The new serialized item stack string.
     * @return A CompletableFuture that completes when updated.
     */
    CompletableFuture<Void> updateSessionContentKey(UUID playerUuid, String contentKey);

    /**
     * Updates stats for an identity.
     * @param identityUuid The UUID of the identity.
     * @param stats The new stats to save.
     * @return A CompletableFuture that completes when updated.
     */
    CompletableFuture<Void> updateStats(UUID identityUuid, IdentityStats stats);

    /**
     * Loads an identity session into RAM.
     * @param playerUuid The UUID of the player.
     * @param identityUuid The UUID of the identity.
     * @param contentKey The serialized identity item.
     * @return A CompletableFuture that completes when loaded.
     */
    CompletableFuture<Void> loadSessionToRam(UUID playerUuid, UUID identityUuid, String contentKey);

    /**
     * Gets the inventory contents for an identity.
     * @param identityUuid The UUID of the identity.
     * @return A CompletableFuture containing the serialized inventory string.
     */
    CompletableFuture<String> getIdentityInventory(UUID identityUuid);

    /**
     * Gets the enderchest contents for an identity.
     * @param identityUuid The UUID of the identity.
     * @return A CompletableFuture containing the serialized enderchest string.
     */
    CompletableFuture<String> getIdentityEnderchest(UUID identityUuid);

    /**
     * Updates the inventory and enderchest contents for an identity.
     * Pass null to leave a specific storage unmodified.
     * @param identityUuid The UUID of the identity.
     * @param inventoryContents The new serialized inventory string (or null to skip).
     * @param enderchestContents The new serialized enderchest string (or null to skip).
     * @return A CompletableFuture that completes when updated.
     */
    CompletableFuture<Void> updateIdentityStorage(UUID identityUuid, String inventoryContents, String enderchestContents);

    /**
     * Checks if a player is on swap cooldown.
     * @param playerUuid The UUID of the player.
     * @return True if on cooldown.
     */
    boolean isOnSwapCooldown(UUID playerUuid);

    /**
     * Sets the swap cooldown for a player.
     * @param playerUuid The UUID of the player.
     */
    void setSwapCooldown(UUID playerUuid);

    /**
     * Clears the swap cooldown for a player.
     * @param playerUuid The UUID of the player.
     */
    void clearSwapCooldown(UUID playerUuid);

    /**
     * Gets the remaining swap cooldown in milliseconds.
     * @param playerUuid The UUID of the player.
     * @return Remaining milliseconds.
     */
    long getRemainingSwapCooldownMillis(UUID playerUuid);

    /**
     * Checks if a player has a pending equipment operation.
     * @param playerUuid The UUID of the player.
     * @return True if pending.
     */
    boolean isEquipPending(UUID playerUuid);

    /**
     * Sets whether a player has a pending equipment operation.
     * @param playerUuid The UUID of the player.
     * @param pending True to set as pending.
     */
    void setEquipPending(UUID playerUuid, boolean pending);
}

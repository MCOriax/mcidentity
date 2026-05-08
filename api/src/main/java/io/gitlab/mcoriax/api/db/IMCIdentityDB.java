package io.gitlab.mcoriax.api.db;

import io.gitlab.mcoriax.api.model.*;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for Identity database operations.
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
     * Creates a new ability type.
     * @param systemCode The unique system code.
     * @param displayName The display name.
     * @param type The type of ability (ACTIVE/PASSIVE).
     * @param isGlobal Whether the ability is global.
     * @return A CompletableFuture that completes when created.
     */
    CompletableFuture<Void> createAbility(String systemCode, String displayName, AbilityType type, boolean isGlobal);

    /**
     * Retrieves all available abilities.
     * @return A CompletableFuture containing a collection of abilities.
     */
    CompletableFuture<Collection<Ability>> getAbilities();

    /**
     * Retrieves all available runes.
     * @return A CompletableFuture containing a collection of runes.
     */
    CompletableFuture<Collection<Rune>> getRunes();

    /**
     * Creates a new identity key.
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
     * Retrieves equipped abilities for an identity.
     * @param identityUuid The UUID of the identity.
     * @return A CompletableFuture containing the equipped abilities map (slot -> ability system code).
     */
    CompletableFuture<Map<Integer, String>> getEquippedAbilities(UUID identityUuid);

    /**
     * Updates an equipped ability for an identity.
     * @param identityUuid The UUID of the identity.
     * @param slot The slot number.
     * @param abilityCode The ability system code, or null to clear the slot.
     * @return A CompletableFuture that completes when updated.
     */
    CompletableFuture<Void> setEquippedAbility(UUID identityUuid, int slot, String abilityCode);

    /**
     * Retrieves runes for an identity.
     * @param identityUuid The UUID of the identity.
     * @return A CompletableFuture containing the runes map (slot -> rune system code).
     */
    CompletableFuture<Map<Integer, String>> getRunes(UUID identityUuid);

    /**
     * Updates a rune for an identity.
     * @param identityUuid The UUID of the identity.
     * @param slot The slot number.
     * @param runeCode The rune system code, or null to clear the slot.
     * @return A CompletableFuture that completes when updated.
     */
    CompletableFuture<Void> setRune(UUID identityUuid, int slot, String runeCode);

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
}

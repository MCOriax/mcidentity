package io.gitlab.mcoriax.api;

import io.gitlab.mcoriax.api.model.*;
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
     * Retrieves equipped abilities for an identity.
     * @param identityUuid The UUID of the identity.
     * @return A CompletableFuture containing a map of slot number to EquippedAbility.
     */
    CompletableFuture<Map<Integer, EquippedAbility>> getEquippedAbilities(UUID identityUuid);

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

    /**
     * Creates a new profession type.
     * @param systemCode The system code for the profession.
     * @param displayName The display name for the profession.
     * @return A CompletableFuture that completes when created.
     */
    CompletableFuture<Void> createProfession(String systemCode, String displayName);

    /**
     * Creates a new ability type.
     * @param systemCode The system code for the ability.
     * @param displayName The display name for the ability.
     * @param type The type of the ability.
     * @param isGlobal Whether the ability is global.
     * @return A CompletableFuture that completes when created.
     */
    CompletableFuture<Void> createAbility(String systemCode, String displayName, AbilityType type, boolean isGlobal);

    /**
     * Retrieves all available professions.
     * @return A CompletableFuture containing a collection of professions.
     */
    CompletableFuture<Collection<Profession>> getProfessions();

    /**
     * Retrieves all available abilities.
     * @return A CompletableFuture containing a collection of abilities.
     */
    CompletableFuture<Collection<Ability>> getAbilities();

    /**
     * Creates a new identity key.
     * @param key The identity key.
     * @param properties The identity properties.
     * @param stats The identity stats.
     * @return A CompletableFuture that completes when created.
     */
    CompletableFuture<Void> createIdentity(IdentityKey key, IdentityProperties properties, IdentityStats stats);

    /**
     * Updates an equipped ability for an identity.
     * @param identityUuid The UUID of the identity.
     * @param slot The slot number.
     * @param abilityCode The system code of the ability.
     * @param modelId The custom model ID, or null.
     * @return A CompletableFuture that completes when updated.
     */
    CompletableFuture<Void> setEquippedAbility(UUID identityUuid, int slot, String abilityCode, String modelId);

    /**
     * Updates a rune for an identity.
     * @param identityUuid The UUID of the identity.
     * @param slot The slot number.
     * @param runeCode The system code of the rune.
     * @return A CompletableFuture that completes when updated.
     */
    CompletableFuture<Void> setRune(UUID identityUuid, int slot, String runeCode);

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
     * Retrieves cosmetics for an identity.
     * @param uuid The UUID of the identity.
     * @return A CompletableFuture containing a map of cosmetic keys and values.
     */
    CompletableFuture<Map<String, String>> getCosmetics(UUID uuid);

    /**
     * Saves skin cosmetic for an identity.
     * @param uuid The UUID of the identity.
     * @param skin The skin value.
     * @return A CompletableFuture that completes when saved.
     */
    CompletableFuture<Void> saveSkin(UUID uuid, String skin);

    /**
     * Saves aura cosmetic for an identity.
     * @param uuid The UUID of the identity.
     * @param aura The aura value.
     * @return A CompletableFuture that completes when saved.
     */
    CompletableFuture<Void> saveAura(UUID uuid, String aura);

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

package io.gitlab.mcoriax.common;

import io.gitlab.mcoriax.api.IdentityProvider;
import io.gitlab.mcoriax.api.db.IMCIdentityDB;
import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.api.model.ActiveIdentitySession;
import io.gitlab.mcoriax.common.MCIdentityManager;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Global provider for the Identity API.
 * Acts as a central hub for all identity-related database and service operations.
 */
public final class MCIdentityProvider {

    /**
     * The singleton instance of the identity provider.
     */
    private static IdentityProvider instance;
    /**
     * The singleton instance of the identity database.
     */
    private static IMCIdentityDB database;

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private MCIdentityProvider() {
        throw new UnsupportedOperationException("Static provider class");
    }

    /**
     * Gets the current identity database implementation.
     * @return The identity database.
     * @throws IllegalStateException if the database has not been initialized.
     */
    public static IMCIdentityDB getDatabase() {
        if (database == null) {
            throw new IllegalStateException("Database has not been initialized!");
        }
        return database;
    }

    /**
     * Sets the identity database implementation.
     * @param db The database implementation to use.
     */
    public static void setDatabase(IMCIdentityDB db) {
        Objects.requireNonNull(db, "Database cannot be null");
        database = db;
    }

    /**
     * Gets the current identity provider implementation.
     * @return The identity provider.
     * @throws IllegalStateException if the provider has not been set.
     */
    public static IdentityProvider get() {
        if (instance == null) {
            throw new IllegalStateException("Identity provider has not been initialized!");
        }
        return instance;
    }

    /**
     * Sets the identity provider implementation.
     * @param provider The provider implementation to use.
     */
    public static void set(IdentityProvider provider) {
        Objects.requireNonNull(provider, "Provider cannot be null");
        instance = provider;
    }

    // --- Database Operations ---

    /**
     * Initializes the database schema.
     */
    public static CompletableFuture<Void> initializeSchema() {
        return getDatabase().initializeSchema();
    }

    /**
     * Creates a new profession type.
     */
    public static CompletableFuture<Void> createProfession(String systemCode, String displayName) {
        return getDatabase().createProfession(systemCode, displayName);
    }

    /**
     * Creates a new ability type.
     */
    public static CompletableFuture<Void> createAbility(String systemCode, String displayName, AbilityType type, boolean isGlobal) {
        return getDatabase().createAbility(systemCode, displayName, type, isGlobal);
    }

    /**
     * Retrieves all available professions.
     */
    public static CompletableFuture<Collection<Profession>> getProfessions() {
        return getDatabase().getProfessions();
    }

    /**
     * Retrieves all available abilities.
     */
    public static CompletableFuture<Collection<Ability>> getAbilities() {
        return getDatabase().getAbilities();
    }

    /**
     * Retrieves all available runes.
     */
    public static CompletableFuture<Collection<Rune>> getRunes() {
        return getDatabase().getRunes();
    }

    /**
     * Creates a new identity key.
     */
    public static CompletableFuture<Void> createIdentity(IdentityKey key, IdentityProperties properties, IdentityStats stats) {
        return getDatabase().createIdentity(key, properties, stats);
    }

    /**
     * Retrieves an identity key by its UUID.
     */
    public static CompletableFuture<Optional<IdentityKey>> getIdentityKey(UUID uuid) {
        return getDatabase().getIdentityKey(uuid);
    }

    /**
     * Retrieves properties for an identity.
     */
    public static CompletableFuture<Optional<IdentityProperties>> getProperties(UUID identityUuid) {
        return getDatabase().getProperties(identityUuid);
    }

    /**
     * Retrieves stats for an identity.
     */
    public static CompletableFuture<Optional<IdentityStats>> getStats(UUID identityUuid) {
        return getDatabase().getStats(identityUuid);
    }

    /**
     * Retrieves a specific stat for an identity.
     */
    public static CompletableFuture<Integer> getStat(UUID identityUuid, String statKey) {
        return getDatabase().getStat(identityUuid, statKey);
    }

    /**
     * Retrieves equipped abilities for an identity.
     */
    public static CompletableFuture<Map<Integer, String>> getEquippedAbilities(UUID identityUuid) {
        return getDatabase().getEquippedAbilities(identityUuid);
    }

    /**
     * Updates an equipped ability for an identity.
     */
    public static CompletableFuture<Void> setEquippedAbility(UUID identityUuid, int slot, String abilityCode) {
        return getDatabase().setEquippedAbility(identityUuid, slot, abilityCode);
    }

    /**
     * Retrieves runes for an identity.
     */
    public static CompletableFuture<Map<Integer, String>> getRunes(UUID identityUuid) {
        return getDatabase().getRunes(identityUuid);
    }

    /**
     * Updates a rune for an identity.
     */
    public static CompletableFuture<Void> setRune(UUID identityUuid, int slot, String runeCode) {
        return getDatabase().setRune(identityUuid, slot, runeCode);
    }

    /**
     * Gets the active identity for a player.
     */
    public static CompletableFuture<Optional<UUID>> getActiveIdentity(UUID playerUuid) {
        return getDatabase().getActiveIdentity(playerUuid);
    }

    /**
     * Gets the full session info for a player.
     */
    public static CompletableFuture<Optional<IdentitySession>> getSession(UUID playerUuid) {
        return getDatabase().getSession(playerUuid);
    }

    /**
     * Sets the active identity for a player.
     */
    public static CompletableFuture<Void> setActiveIdentity(UUID playerUuid, UUID identityUuid, String contentKey) {
        return getDatabase().setActiveIdentity(playerUuid, identityUuid, contentKey).thenCompose(v -> {
            if (identityUuid == null) {
                MCIdentityManager.removeSession(playerUuid);
                return CompletableFuture.completedFuture(null);
            } else {
                return loadSessionToRam(playerUuid, identityUuid);
            }
        });
    }

    /**
     * Logs an action for an identity.
     */
    public static CompletableFuture<Void> logAction(UUID playerUuid, UUID identityUuid, ActionType actionType) {
        return getDatabase().logAction(playerUuid, identityUuid, actionType);
    }

    /**
     * Updates properties for an identity.
     */
    public static CompletableFuture<Void> updateProperties(UUID identityUuid, IdentityProperties properties) {
        return getDatabase().updateProperties(identityUuid, properties).thenAccept(v -> {
            MCIdentityManager.getSessionByIdentity(identityUuid).ifPresent(session -> {
                MCIdentityManager.updateProperties(session.playerUuid(), properties);
            });
        });
    }

    /**
     * Updates stats for an identity.
     */
    public static CompletableFuture<Void> updateStats(UUID identityUuid, IdentityStats stats) {
        return getDatabase().updateStats(identityUuid, stats).thenAccept(v -> {
            MCIdentityManager.getSessionByIdentity(identityUuid).ifPresent(session -> {
                MCIdentityManager.updateStats(session.playerUuid(), stats);
            });
        });
    }

    /**
     * Loads an identity session into RAM.
     * @param playerUuid The UUID of the player.
     * @param identityUuid The UUID of the identity.
     * @return A CompletableFuture that completes when loaded.
     */
    public static CompletableFuture<Void> loadSessionToRam(UUID playerUuid, UUID identityUuid) {
        return getIdentityKey(identityUuid).thenCompose(optKey -> {
            if (optKey.isEmpty()) return CompletableFuture.completedFuture(null);
            IdentityKey key = optKey.get();

            return getProperties(identityUuid).thenCompose(optProps -> {
                if (optProps.isEmpty()) return CompletableFuture.completedFuture(null);
                IdentityProperties props = optProps.get();

                return getStats(identityUuid).thenAccept(optStats -> {
                    if (optStats.isPresent()) {
                        IdentityStats stats = optStats.get();
                        MCIdentityManager.cacheSession(new ActiveIdentitySession(
                            playerUuid,
                            identityUuid,
                            key,
                            props,
                            stats
                        ));
                    }
                });
            });
        });
    }

    /**
     * Adds a value to a specific stat.
     */
    public static CompletableFuture<Void> addStat(UUID identityUuid, String statKey, int amount) {
        return getDatabase().addStat(identityUuid, statKey, amount);
    }
}

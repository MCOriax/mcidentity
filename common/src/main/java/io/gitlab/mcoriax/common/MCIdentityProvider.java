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
public class MCIdentityProvider implements IdentityProvider {

    /**
     * The singleton instance of the identity provider.
     */
    public static MCIdentityProvider instance;
    /**
     * The instance of the identity database.
     */
    private final IMCIdentityDB database;

    /**
     * Initializes the identity provider with a database.
     * @param db The database implementation to use.
     */
    public MCIdentityProvider(IMCIdentityDB db) {
        this.database = Objects.requireNonNull(db, "Database cannot be null");
        instance = this;
    }

    /**
     * Gets whether the identity database has been initialized.
     * @return True if initialized.
     */
    public static boolean hasDatabase() {
        return instance != null && instance.database != null;
    }

    // --- Database Operations ---

    /**
     * Initializes the database schema.
     */
    public CompletableFuture<Void> initializeSchema() {
        return database.initializeSchema();
    }

    /**
     * Creates a new profession type.
     */
    public CompletableFuture<Void> createProfession(String systemCode, String displayName) {
        return database.createProfession(systemCode.toLowerCase(), displayName);
    }

    /**
     * Creates a new ability type.
     */
    public CompletableFuture<Void> createAbility(String systemCode, String displayName, AbilityType type, boolean isGlobal) {
        return database.createAbility(systemCode.toLowerCase(), displayName, type, isGlobal);
    }

    /**
     * Retrieves all available professions.
     */
    public CompletableFuture<Collection<Profession>> getProfessions() {
        return database.getProfessions();
    }

    /**
     * Retrieves all available abilities.
     */
    public CompletableFuture<Collection<Ability>> getAbilities() {
        return database.getAbilities();
    }

    /**
     * Retrieves all available runes.
     */
    public CompletableFuture<Collection<Rune>> getRunes() {
        return database.getRunes();
    }

    /**
     * Creates a new identity key.
     */
    public CompletableFuture<Void> createIdentity(IdentityKey key, IdentityProperties properties, IdentityStats stats) {
        return database.createIdentity(key, properties, stats);
    }

    /**
     * Retrieves an identity key by its UUID.
     */
    @Override
    public CompletableFuture<Optional<IdentityKey>> getIdentityKey(UUID uuid) {
        return database.getIdentityKey(uuid);
    }

    /**
     * Retrieves properties for an identity.
     */
    @Override
    public CompletableFuture<Optional<IdentityProperties>> getProperties(UUID identityUuid) {
        return database.getProperties(identityUuid);
    }

    /**
     * Retrieves stats for an identity.
     */
    @Override
    public CompletableFuture<Optional<IdentityStats>> getStats(UUID identityUuid) {
        return database.getStats(identityUuid);
    }

    /**
     * Retrieves a specific stat for an identity.
     */
    @Override
    public CompletableFuture<Integer> getStat(UUID identityUuid, String statKey) {
        return database.getStat(identityUuid, statKey.toLowerCase());
    }

    /**
     * Retrieves equipped abilities for an identity.
     */
    @Override
    public CompletableFuture<Map<Integer, String>> getEquippedAbilities(UUID identityUuid) {
        return database.getEquippedAbilities(identityUuid);
    }

    /**
     * Updates an equipped ability for an identity.
     */
    public CompletableFuture<Void> setEquippedAbility(UUID identityUuid, int slot, String abilityCode) {
        return database.setEquippedAbility(identityUuid, slot, abilityCode != null ? abilityCode.toLowerCase() : null);
    }

    /**
     * Retrieves runes for an identity.
     */
    @Override
    public CompletableFuture<Map<Integer, String>> getRunes(UUID identityUuid) {
        return database.getRunes(identityUuid);
    }

    /**
     * Updates a rune for an identity.
     */
    public CompletableFuture<Void> setRune(UUID identityUuid, int slot, String runeCode) {
        return database.setRune(identityUuid, slot, runeCode != null ? runeCode.toLowerCase() : null);
    }

    /**
     * Gets the active identity for a player.
     */
    @Override
    public CompletableFuture<Optional<UUID>> getActiveIdentity(UUID playerUuid) {
        return database.getActiveIdentity(playerUuid);
    }

    /**
     * Gets the full session info for a player.
     */
    public CompletableFuture<Optional<IdentitySession>> getSession(UUID playerUuid) {
        return database.getSession(playerUuid);
    }

    /**
     * Sets the active identity for a player.
     */
    public CompletableFuture<Void> setActiveIdentity(UUID playerUuid, UUID identityUuid, String contentKey) {
        return database.setActiveIdentity(playerUuid, identityUuid, contentKey).thenCompose(v -> {
            if (identityUuid == null) {
                MCIdentityManager.removeSession(playerUuid);
                return CompletableFuture.completedFuture(null);
            } else {
                return loadSessionToRam(playerUuid, identityUuid, contentKey);
            }
        });
    }

    /**
     * Logs an action for an identity.
     */
    @Override
    public CompletableFuture<Void> logAction(UUID playerUuid, UUID identityUuid, ActionType actionType) {
        return database.logAction(playerUuid, identityUuid, actionType);
    }

    /**
     * Updates properties for an identity.
     */
    @Override
    public CompletableFuture<Void> updateProperties(UUID identityUuid, IdentityProperties properties) {
        return database.updateProperties(identityUuid, properties).thenAccept(v -> {
            MCIdentityManager.getSessionByIdentity(identityUuid).ifPresent(session -> {
                MCIdentityManager.updateProperties(session.playerUuid(), properties);
            });
        });
    }

    /**
     * Updates stats for an identity.
     */
    public CompletableFuture<Void> updateStats(UUID identityUuid, IdentityStats stats) {
        return database.updateStats(identityUuid, stats).thenAccept(v -> {
            MCIdentityManager.getSessionByIdentity(identityUuid).ifPresent(session -> {
                MCIdentityManager.updateStats(session.playerUuid(), stats);
            });
        });
    }

    /**
     * Loads an identity session into RAM.
     * @param playerUuid The UUID of the player.
     * @param identityUuid The UUID of the identity.
     * @param contentKey The serialized identity item.
     * @return A CompletableFuture that completes when loaded.
     */
    public CompletableFuture<Void> loadSessionToRam(UUID playerUuid, UUID identityUuid, String contentKey) {
        return getIdentityKey(identityUuid).thenCompose(optKey -> {
            if (optKey.isEmpty()) return CompletableFuture.completedFuture(null);
            IdentityKey key = optKey.get();

            return getProperties(identityUuid).thenCompose(optProps -> {
                if (optProps.isEmpty()) return CompletableFuture.completedFuture(null);
                IdentityProperties props = optProps.get();

                return getCosmetics(identityUuid).thenCompose(cosmetics -> {
                    props.setSkin(cosmetics.get("skin"));
                    props.setAura(cosmetics.get("aura"));

                    return getStats(identityUuid).thenAccept(optStats -> {
                        if (optStats.isPresent()) {
                            IdentityStats stats = optStats.get();
                            MCIdentityManager.cacheSession(new ActiveIdentitySession(
                                playerUuid,
                                identityUuid,
                                key,
                                props,
                                stats,
                                contentKey,
                                stats.getHealth(),
                                stats.getEnergy(),
                                stats.getFood()
                            ));
                        }
                    });
                });
            });
        });
    }

    /**
     * Adds a value to a specific stat.
     */
    @Override
    public CompletableFuture<Void> addStat(UUID identityUuid, String statKey, int amount) {
        return database.addStat(identityUuid, statKey.toLowerCase(), amount);
    }

    /**
     * Retrieves cosmetics for an identity.
     */
    public CompletableFuture<Map<String, String>> getCosmetics(UUID uuid) {
        return database.getCosmetics(uuid);
    }

    /**
     * Saves skin cosmetic for an identity.
     */
    public CompletableFuture<Void> saveSkin(UUID uuid, String skin) {
        return database.saveSkin(uuid, skin);
    }

    /**
     * Saves aura cosmetic for an identity.
     */
    public CompletableFuture<Void> saveAura(UUID uuid, String aura) {
        return database.saveAura(uuid, aura);
    }

    /**
     * Gracefully shuts down the identity provider and its database.
     */
    public void shutdown() {
        if (database != null) {
            database.shutdown();
        }
    }
}

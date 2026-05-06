package io.gitlab.mcoriax.common;

import io.gitlab.mcoriax.api.IdentityProvider;
import io.gitlab.mcoriax.api.db.IMCIdentityDB;
import io.gitlab.mcoriax.api.model.IdentityStats;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Global provider for the Identity API.
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

    /**
     * Shortcut to add a value to a specific stat via the database.
     * @param identityUuid The UUID of the identity.
     * @param statKey The system code of the stat.
     * @param amount The amount to add.
     * @return A CompletableFuture that completes when updated.
     */
    public static CompletableFuture<Void> addStat(UUID identityUuid, String statKey, int amount) {
        return getDatabase().addStat(identityUuid, statKey, amount);
    }

    /**
     * Shortcut to get a specific stat value via the database.
     * @param identityUuid The UUID of the identity.
     * @param statKey The system code of the stat.
     * @return A CompletableFuture containing the stat value.
     */
    public static CompletableFuture<Integer> getStat(UUID identityUuid, String statKey) {
        return getDatabase().getStat(identityUuid, statKey);
    }

    /**
     * Shortcut to get all stats for an identity via the database.
     * @param identityUuid The UUID of the identity.
     * @return A CompletableFuture containing the identity stats.
     */
    public static CompletableFuture<Optional<IdentityStats>> getStats(UUID identityUuid) {
        return getDatabase().getStats(identityUuid);
    }
}

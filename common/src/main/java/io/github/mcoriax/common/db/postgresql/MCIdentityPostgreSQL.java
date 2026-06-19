package io.github.mcoriax.common.db.postgresql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.mcoriax.api.model.*;
import io.github.mcoriax.common.db.postgresql.utils.*;
import io.github.mcoriax.common.db.sql.AbstractSqlIdentityDB;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * PostgreSQL implementation of IMCIdentityDB.
 */
public abstract class MCIdentityPostgreSQL extends AbstractSqlIdentityDB {

    /**
     * Initializes a new PostgreSQL database provider with connection pooling.
     * @param host The host address.
     * @param port The port number.
     * @param database The database name.
     * @param username The username.
     * @param password The password.
     */
    public MCIdentityPostgreSQL(String host, int port, String database, String username, String password) {
        super(buildDataSource(host, port, database, username, password));

        // Ensure the schema exists before the database is used.
        initializeSchema().join();
    }

    protected MCIdentityPostgreSQL(HikariDataSource dataSource) {
        super(dataSource);
    }

    /**
     * Builds the Hikari data source with PostgreSQL-specific configuration.
     * @param host The host address.
     * @param port The port number.
     * @param database The database name.
     * @param username The username.
     * @param password The password.
     * @return The configured data source.
     */
    private static HikariDataSource buildDataSource(String host, int port, String database, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, database));
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(10);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("MCIdentityPostgreSQLPool");

        // PostgreSQL specific performance tweaks
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        return new HikariDataSource(config);
    }

    @Override
    public CompletableFuture<Void> initializeSchema() {
        return runAsync(InitializeSchema::invoke);
    }

    @Override
    public CompletableFuture<Void> createProfession(String systemCode, String displayName) {
        return runAsync(conn -> CreateProfession.invoke(conn, systemCode, displayName));
    }

    @Override
    public CompletableFuture<Void> createAbility(String systemCode, String displayName, AbilityType type, boolean isGlobal) {
        return runAsync(conn -> CreateAbility.invoke(conn, systemCode, displayName, type, isGlobal));
    }

    @Override
    public CompletableFuture<Collection<Profession>> getProfessions() {
        return supplyAsync(Collections.emptyList(), GetProfessions::invoke);
    }

    @Override
    public CompletableFuture<Collection<Ability>> getAbilities() {
        return supplyAsync(Collections.emptyList(), GetAbilities::invoke);
    }

    @Override
    public CompletableFuture<Collection<Rune>> getRunes() {
        return supplyAsync(Collections.emptyList(), conn -> GetRunes.invoke(conn));
    }

    @Override
    public CompletableFuture<Void> createIdentity(IdentityKey key, IdentityProperties properties, IdentityStats stats) {
        return runAsyncOrThrow(conn -> CreateIdentity.invoke(conn, key, properties, stats), "Failed to create identity in PostgreSQL");
    }

    @Override
    public CompletableFuture<Optional<IdentityKey>> getIdentityKey(UUID uuid) {
        return supplyAsync(Optional.empty(), conn -> GetIdentityKey.invoke(conn, uuid));
    }

    @Override
    public CompletableFuture<Optional<IdentityProperties>> getProperties(UUID identityUuid) {
        return supplyAsync(Optional.empty(), conn -> GetProperties.invoke(conn, identityUuid));
    }

    @Override
    public CompletableFuture<Optional<IdentityStats>> getStats(UUID identityUuid) {
        return supplyAsync(Optional.empty(), conn -> GetStats.invoke(conn, identityUuid));
    }

    @Override
    public CompletableFuture<Integer> getStat(UUID identityUuid, String statKey) {
        return supplyAsync(0, conn -> GetStat.invoke(conn, identityUuid, statKey));
    }

    @Override
    public CompletableFuture<Map<Integer, EquippedAbility>> getEquippedAbilities(UUID identityUuid) {
        return supplyAsync(Collections.emptyMap(), conn -> GetEquippedAbilities.invoke(conn, identityUuid));
    }

    @Override
    public CompletableFuture<Void> setEquippedAbility(UUID identityUuid, int slot, String abilityCode, String modelId) {
        return runAsync(conn -> SetEquippedAbility.invoke(conn, identityUuid, slot, abilityCode, modelId));
    }

    @Override
    public CompletableFuture<Map<Integer, String>> getRunes(UUID identityUuid) {
        return supplyAsync(Collections.emptyMap(), conn -> GetRunes.invoke(conn, identityUuid));
    }

    @Override
    public CompletableFuture<Void> setRune(UUID identityUuid, int slot, String runeCode) {
        return runAsync(conn -> SetRune.invoke(conn, identityUuid, slot, runeCode));
    }

    @Override
    public CompletableFuture<Optional<UUID>> getActiveIdentity(UUID playerUuid) {
        return supplyAsync(Optional.empty(), conn -> GetActiveIdentity.invoke(conn, playerUuid));
    }

    @Override
    public CompletableFuture<Optional<IdentitySession>> getSession(UUID playerUuid) {
        return supplyAsync(Optional.empty(), conn -> GetSession.invoke(conn, playerUuid));
    }

    @Override
    public CompletableFuture<Void> setActiveIdentity(UUID playerUuid, UUID identityUuid, String contentKey) {
        return runAsync(conn -> SetActiveIdentity.invoke(conn, playerUuid, identityUuid, contentKey));
    }

    @Override
    public CompletableFuture<Void> logAction(UUID playerUuid, UUID identityUuid, ActionType actionType) {
        return runAsync(conn -> LogAction.invoke(conn, playerUuid, identityUuid, actionType));
    }

    @Override
    public CompletableFuture<Void> updateProperties(UUID identityUuid, IdentityProperties properties) {
        return runAsync(conn -> UpdateProperties.invoke(conn, identityUuid, properties));
    }

    @Override
    public CompletableFuture<Void> updateStats(UUID identityUuid, IdentityStats stats) {
        return runAsync(conn -> UpdateStats.invoke(conn, identityUuid, stats));
    }

    @Override
    public CompletableFuture<Void> addStat(UUID identityUuid, String statKey, int amount) {
        return runAsync(conn -> AddStat.invoke(conn, identityUuid, statKey, amount));
    }

    @Override
    public CompletableFuture<Map<String, String>> getCosmetics(UUID uuid) {
        return supplyAsync(Collections.emptyMap(), conn -> GetCosmetics.invoke(conn, uuid));
    }

    @Override
    public CompletableFuture<Void> saveSkin(UUID uuid, String skin) {
        return runAsync(conn -> SaveSkin.invoke(conn, uuid, skin));
    }

    @Override
    public CompletableFuture<Void> saveAura(UUID uuid, String aura) {
        return runAsync(conn -> SaveAura.invoke(conn, uuid, aura));
    }

    @Override
    public CompletableFuture<Void> updateSessionContentKey(UUID playerUuid, String contentKey) {
        return runAsync(conn -> UpdateSessionContentKey.invoke(conn, playerUuid, contentKey));
    }
}

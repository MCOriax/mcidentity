package io.gitlab.mcoriax.common.db.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.gitlab.mcoriax.api.db.IMCIdentityDB;
import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.common.db.mongodb.utils.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * MongoDB implementation of IMCIdentityDB.
 */
public abstract class MCIdentityMongoDB implements IMCIdentityDB {

    private final MongoClient mongoClient;
    private final MongoDatabase database;

    /**
     * Initializes a new MongoDB provider.
     * @param uri The MongoDB connection URI.
     * @param dbName The database name.
     */
    public MCIdentityMongoDB(String uri, String dbName) {
        this.mongoClient = MongoClients.create(uri);
        this.database = this.mongoClient.getDatabase(dbName);
    }

    /**
     * Closes the MongoDB client connection.
     */
    @Override
    public void shutdown() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @Override
    public CompletableFuture<Void> initializeSchema() {
        return CompletableFuture.runAsync(() -> InitializeSchema.invoke(database));
    }

    @Override
    public CompletableFuture<Void> createProfession(String systemCode, String displayName) {
        return CompletableFuture.runAsync(() -> CreateProfession.invoke(database, systemCode, displayName));
    }

    @Override
    public CompletableFuture<Void> createAbility(String systemCode, String displayName, AbilityType type, boolean isGlobal) {
        return CompletableFuture.runAsync(() -> CreateAbility.invoke(database, systemCode, displayName, type, isGlobal));
    }

    @Override
    public CompletableFuture<Collection<Profession>> getProfessions() {
        return CompletableFuture.supplyAsync(() -> GetProfessions.invoke(database));
    }

    @Override
    public CompletableFuture<Collection<Ability>> getAbilities() {
        return CompletableFuture.supplyAsync(() -> GetAbilities.invoke(database));
    }

    @Override
    public CompletableFuture<Collection<Rune>> getRunes() {
        return CompletableFuture.supplyAsync(() -> GetRunes.invoke(database));
    }

    @Override
    public CompletableFuture<Void> createIdentity(IdentityKey key, IdentityProperties properties, IdentityStats stats) {
        return CompletableFuture.runAsync(() -> CreateIdentity.invoke(database, key, properties, stats));
    }

    @Override
    public CompletableFuture<Optional<IdentityKey>> getIdentityKey(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> GetIdentityKey.invoke(database, uuid));
    }

    @Override
    public CompletableFuture<Optional<IdentityProperties>> getProperties(UUID identityUuid) {
        return CompletableFuture.supplyAsync(() -> GetProperties.invoke(database, identityUuid));
    }

    @Override
    public CompletableFuture<Optional<IdentityStats>> getStats(UUID identityUuid) {
        return CompletableFuture.supplyAsync(() -> GetStats.invoke(database, identityUuid));
    }

    @Override
    public CompletableFuture<Integer> getStat(UUID identityUuid, String statKey) {
        return CompletableFuture.supplyAsync(() -> GetStat.invoke(database, identityUuid, statKey));
    }

    @Override
    public CompletableFuture<Map<Integer, EquippedAbility>> getEquippedAbilities(UUID identityUuid) {
        return CompletableFuture.supplyAsync(() -> GetEquippedAbilities.invoke(database, identityUuid));
    }

    @Override
    public CompletableFuture<Void> setEquippedAbility(UUID identityUuid, int slot, String abilityCode, String modelId) {
        return CompletableFuture.runAsync(() -> SetEquippedAbility.invoke(database, identityUuid, slot, abilityCode, modelId));
    }

    @Override
    public CompletableFuture<Map<Integer, String>> getRunes(UUID identityUuid) {
        return CompletableFuture.supplyAsync(() -> GetRunes.invoke(database, identityUuid));
    }

    @Override
    public CompletableFuture<Void> setRune(UUID identityUuid, int slot, String runeCode) {
        return CompletableFuture.runAsync(() -> SetRune.invoke(database, identityUuid, slot, runeCode));
    }

    @Override
    public CompletableFuture<Optional<UUID>> getActiveIdentity(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> GetActiveIdentity.invoke(database, playerUuid));
    }

    @Override
    public CompletableFuture<Optional<IdentitySession>> getSession(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> GetSession.invoke(database, playerUuid));
    }

    @Override
    public CompletableFuture<Void> setActiveIdentity(UUID playerUuid, UUID identityUuid, String contentKey) {
        return CompletableFuture.runAsync(() -> SetActiveIdentity.invoke(database, playerUuid, identityUuid, contentKey));
    }

    @Override
    public CompletableFuture<Void> logAction(UUID playerUuid, UUID identityUuid, ActionType actionType) {
        return CompletableFuture.runAsync(() -> LogAction.invoke(database, playerUuid, identityUuid, actionType));
    }

    @Override
    public CompletableFuture<Void> updateProperties(UUID identityUuid, IdentityProperties properties) {
        return CompletableFuture.runAsync(() -> UpdateProperties.invoke(database, identityUuid, properties));
    }

    @Override
    public CompletableFuture<Void> updateStats(UUID identityUuid, IdentityStats stats) {
        return CompletableFuture.runAsync(() -> UpdateStats.invoke(database, identityUuid, stats));
    }

    @Override
    public CompletableFuture<Void> addStat(UUID identityUuid, String statKey, int amount) {
        return CompletableFuture.runAsync(() -> AddStat.invoke(database, identityUuid, statKey, amount));
    }

    @Override
    public CompletableFuture<Map<String, String>> getCosmetics(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> GetCosmetics.invoke(database, uuid));
    }

    @Override
    public CompletableFuture<Void> saveSkin(UUID uuid, String skin) {
        return CompletableFuture.runAsync(() -> SaveSkin.invoke(database, uuid, skin));
    }

    @Override
    public CompletableFuture<Void> saveAura(UUID uuid, String aura) {
        return CompletableFuture.runAsync(() -> SaveAura.invoke(database, uuid, aura));
    }

    @Override
    public CompletableFuture<Void> updateSessionContentKey(UUID playerUuid, String contentKey) {
        return CompletableFuture.runAsync(() -> UpdateSessionContentKey.invoke(database, playerUuid, contentKey));
    }
}

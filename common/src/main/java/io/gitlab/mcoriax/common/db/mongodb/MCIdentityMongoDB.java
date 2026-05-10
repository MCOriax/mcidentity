package io.gitlab.mcoriax.common.db.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import io.gitlab.mcoriax.api.db.IMCIdentityDB;
import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.common.MCIdentityManager;
import org.bson.Document;

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
        return CompletableFuture.runAsync(() -> {
            // Create unique indexes to act as constraints similar to SQL
            IndexOptions uniqueIndex = new IndexOptions().unique(true);
            database.getCollection("professions").createIndex(new Document("system_code", 1), uniqueIndex);
            database.getCollection("abilities").createIndex(new Document("system_code", 1), uniqueIndex);
            database.getCollection("runes").createIndex(new Document("system_code", 1), uniqueIndex);
        });
    }

    @Override
    public CompletableFuture<Void> createProfession(String systemCode, String displayName) {
        return CompletableFuture.runAsync(() -> {
            MongoCollection<Document> collection = database.getCollection("professions");
            Document doc = new Document("system_code", systemCode).append("display_name", displayName);
            collection.updateOne(Filters.eq("system_code", systemCode), new Document("$set", doc), new UpdateOptions().upsert(true));
        });
    }

    @Override
    public CompletableFuture<Void> createAbility(String systemCode, String displayName, AbilityType type, boolean isGlobal) {
        return CompletableFuture.runAsync(() -> {
            MongoCollection<Document> collection = database.getCollection("abilities");
            Document doc = new Document("system_code", systemCode)
                    .append("display_name", displayName)
                    .append("ability_type", type.name())
                    .append("is_global", isGlobal);
            collection.updateOne(Filters.eq("system_code", systemCode), new Document("$set", doc), new UpdateOptions().upsert(true));
        });
    }

    @Override
    public CompletableFuture<Collection<Profession>> getProfessions() {
        return CompletableFuture.supplyAsync(() -> {
            List<Profession> professions = new ArrayList<>();
            for (Document doc : database.getCollection("professions").find()) {
                String code = doc.getString("system_code");
                professions.add(new Profession() {
                    @Override public String getSystemCode() { return code; }
                    @Override public String getDisplayName() { return doc.getString("display_name"); }
                    @Override public Map<String, Object> getExpConfig() {
                        return MCIdentityManager.getProfessionExpConfig(code);
                    }
                    @Override public Map<String, Integer> getBaseStats() {
                        return MCIdentityManager.getProfessionBaseStats(code);
                    }
                    @Override public String getStatsRandom() {
                        return MCIdentityManager.getProfessionStatsRandom(code);
                    }
                });
            }
            return professions;
        });
    }

    @Override
    public CompletableFuture<Collection<Ability>> getAbilities() {
        return CompletableFuture.supplyAsync(() -> {
            List<Ability> abilities = new ArrayList<>();
            for (Document doc : database.getCollection("abilities").find()) {
                abilities.add(new Ability() {
                    @Override public String getSystemCode() { return doc.getString("system_code"); }
                    @Override public String getDisplayName() { return doc.getString("display_name"); }
                    @Override public AbilityType getAbilityType() { return AbilityType.valueOf(doc.getString("ability_type")); }
                    @Override public boolean isGlobal() { return doc.getBoolean("is_global", false); }
                });
            }
            return abilities;
        });
    }

    @Override
    public CompletableFuture<Collection<Rune>> getRunes() {
        return CompletableFuture.supplyAsync(() -> {
            List<Rune> runes = new ArrayList<>();
            for (Document doc : database.getCollection("runes").find()) {
                runes.add(new Rune() {
                    @Override public String getSystemCode() { return doc.getString("system_code"); }
                    @Override public String getDisplayName() { return doc.getString("display_name"); }
                });
            }
            return runes;
        });
    }

    @Override
    public CompletableFuture<Void> createIdentity(IdentityKey key, IdentityProperties properties, IdentityStats stats) {
        return createProfession(key.getProfessionCode(), key.getProfessionCode())
                .thenRunAsync(() -> {
                    MongoCollection<Document> identities = database.getCollection("identities");
                    
                    Document propsDoc = new Document("level", properties.getLevel())
                            .append("exp", properties.getExp())
                            .append("stat_points", properties.getStatPoints())
                            .append("head_text", properties.getHeadText())
                            .append("model", properties.getModel());

                    Document statsDoc = new Document("energy", stats.getEnergy())
                            .append("health", stats.getHealth())
                            .append("food", stats.getFood())
                            .append("strength", stats.getStrength())
                            .append("defense", stats.getDefense())
                            .append("wisdom", stats.getWisdom())
                            .append("luck", stats.getLuck())
                            .append("agility", stats.getAgility())
                            .append("resilience", stats.getResilience());

                    Document identityDoc = new Document("_id", key.getUniqueId().toString())
                            .append("profession_code", key.getProfessionCode())
                            .append("properties", propsDoc)
                            .append("stats", statsDoc)
                            .append("equipped_abilities", new Document())
                            .append("equipped_runes", new Document());

                    identities.insertOne(identityDoc);
                });
    }

    @Override
    public CompletableFuture<Optional<IdentityKey>> getIdentityKey(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = database.getCollection("identities").find(Filters.eq("_id", uuid.toString())).first();
            if (doc != null) {
                return Optional.of(new IdentityKey() {
                    @Override public UUID getUniqueId() { return uuid; }
                    @Override public String getProfessionCode() { return doc.getString("profession_code"); }
                });
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Optional<IdentityProperties>> getProperties(UUID identityUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = database.getCollection("identities").find(Filters.eq("_id", identityUuid.toString())).first();
            if (doc != null && doc.containsKey("properties")) {
                Document props = doc.get("properties", Document.class);
                return Optional.of(new IdentityProperties() {
                    @Override public int getLevel() { return props.getInteger("level", 1); }
                    @Override public int getExp() { return props.getInteger("exp", 0); }
                    @Override public int getStatPoints() { return props.getInteger("stat_points", 0); }
                    @Override public String getHeadText() { return props.getString("head_text"); }
                    @Override public String getModel() { return props.getString("model"); }
                });
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Optional<IdentityStats>> getStats(UUID identityUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = database.getCollection("identities").find(Filters.eq("_id", identityUuid.toString())).first();
            if (doc != null && doc.containsKey("stats")) {
                Document stats = doc.get("stats", Document.class);
                return Optional.of(new IdentityStats() {
                    @Override public int getEnergy() { return stats.getInteger("energy", 0); }
                    @Override public int getHealth() { return stats.getInteger("health", 0); }
                    @Override public int getFood() { return stats.getInteger("food", 20); }
                    @Override public int getStrength() { return stats.getInteger("strength", 0); }
                    @Override public int getDefense() { return stats.getInteger("defense", 0); }
                    @Override public int getWisdom() { return stats.getInteger("wisdom", 0); }
                    @Override public int getLuck() { return stats.getInteger("luck", 0); }
                    @Override public int getAgility() { return stats.getInteger("agility", 0); }
                    @Override public int getResilience() { return stats.getInteger("resilience", 0); }
                });
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Integer> getStat(UUID identityUuid, String statKey) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = database.getCollection("identities").find(Filters.eq("_id", identityUuid.toString())).first();
            if (doc != null && doc.containsKey("stats")) {
                Document stats = doc.get("stats", Document.class);
                return stats.getInteger(statKey, 0);
            }
            return 0;
        });
    }

    @Override
    public CompletableFuture<Map<Integer, String>> getEquippedAbilities(UUID identityUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Map<Integer, String> map = new HashMap<>();
            Document doc = database.getCollection("identities").find(Filters.eq("_id", identityUuid.toString())).first();
            if (doc != null && doc.containsKey("equipped_abilities")) {
                Document equipped = doc.get("equipped_abilities", Document.class);
                for (String key : equipped.keySet()) {
                    map.put(Integer.parseInt(key), equipped.getString(key));
                }
            }
            return map;
        });
    }

    @Override
    public CompletableFuture<Void> setEquippedAbility(UUID identityUuid, int slot, String abilityCode) {
        return CompletableFuture.runAsync(() -> {
            MongoCollection<Document> identities = database.getCollection("identities");
            if (abilityCode == null) {
                identities.updateOne(Filters.eq("_id", identityUuid.toString()), Updates.unset("equipped_abilities." + slot));
            } else {
                identities.updateOne(Filters.eq("_id", identityUuid.toString()), Updates.set("equipped_abilities." + slot, abilityCode));
            }
        });
    }

    @Override
    public CompletableFuture<Map<Integer, String>> getRunes(UUID identityUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Map<Integer, String> map = new HashMap<>();
            Document doc = database.getCollection("identities").find(Filters.eq("_id", identityUuid.toString())).first();
            if (doc != null && doc.containsKey("equipped_runes")) {
                Document equipped = doc.get("equipped_runes", Document.class);
                for (String key : equipped.keySet()) {
                    map.put(Integer.parseInt(key), equipped.getString(key));
                }
            }
            return map;
        });
    }

    @Override
    public CompletableFuture<Void> setRune(UUID identityUuid, int slot, String runeCode) {
        return CompletableFuture.runAsync(() -> {
            MongoCollection<Document> identities = database.getCollection("identities");
            if (runeCode == null) {
                identities.updateOne(Filters.eq("_id", identityUuid.toString()), Updates.unset("equipped_runes." + slot));
            } else {
                identities.updateOne(Filters.eq("_id", identityUuid.toString()), Updates.set("equipped_runes." + slot, runeCode));
            }
        });
    }

    @Override
    public CompletableFuture<Optional<UUID>> getActiveIdentity(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Document session = database.getCollection("sessions").find(Filters.eq("_id", playerUuid.toString())).first();
            if (session != null && session.containsKey("identity_uuid")) {
                return Optional.of(UUID.fromString(session.getString("identity_uuid")));
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Optional<IdentitySession>> getSession(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Document session = database.getCollection("sessions").find(Filters.eq("_id", playerUuid.toString())).first();
            if (session != null && session.containsKey("identity_uuid")) {
                return Optional.of(new IdentitySession() {
                    @Override public UUID getPlayerUniqueId() { return playerUuid; }
                    @Override public UUID getIdentityUniqueId() { return UUID.fromString(session.getString("identity_uuid")); }
                    @Override public String getContentKey() { return session.getString("content_key"); }
                });
            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Void> setActiveIdentity(UUID playerUuid, UUID identityUuid, String contentKey) {
        return CompletableFuture.runAsync(() -> {
            MongoCollection<Document> sessions = database.getCollection("sessions");
            if (identityUuid == null) {
                sessions.deleteOne(Filters.eq("_id", playerUuid.toString()));
            } else {
                Document doc = new Document("_id", playerUuid.toString())
                        .append("identity_uuid", identityUuid.toString())
                        .append("content_key", contentKey)
                        .append("equipped_at", new Date());
                sessions.updateOne(Filters.eq("_id", playerUuid.toString()), new Document("$set", doc), new UpdateOptions().upsert(true));
            }
        });
    }

    @Override
    public CompletableFuture<Void> logAction(UUID playerUuid, UUID identityUuid, ActionType actionType) {
        return CompletableFuture.runAsync(() -> {
            Document log = new Document("player_uuid", playerUuid.toString())
                    .append("identity_uuid", identityUuid.toString())
                    .append("action_type", actionType.name())
                    .append("action_time", new Date());
            database.getCollection("logs").insertOne(log);
        });
    }

    @Override
    public CompletableFuture<Void> updateProperties(UUID identityUuid, IdentityProperties properties) {
        return CompletableFuture.runAsync(() -> {
            Document propsDoc = new Document("level", properties.getLevel())
                    .append("exp", properties.getExp())
                    .append("stat_points", properties.getStatPoints())
                    .append("head_text", properties.getHeadText())
                    .append("model", properties.getModel());

            database.getCollection("identities").updateOne(
                    Filters.eq("_id", identityUuid.toString()),
                    Updates.set("properties", propsDoc)
            );
        });
    }

    @Override
    public CompletableFuture<Void> updateStats(UUID identityUuid, IdentityStats stats) {
        return CompletableFuture.runAsync(() -> {
            Document statsDoc = new Document("energy", stats.getEnergy())
                    .append("health", stats.getHealth())
                    .append("food", stats.getFood())
                    .append("strength", stats.getStrength())
                    .append("defense", stats.getDefense())
                    .append("wisdom", stats.getWisdom())
                    .append("luck", stats.getLuck())
                    .append("agility", stats.getAgility())
                    .append("resilience", stats.getResilience());

            database.getCollection("identities").updateOne(
                    Filters.eq("_id", identityUuid.toString()),
                    Updates.set("stats", statsDoc)
            );
        });
    }

    @Override
    public CompletableFuture<Void> addStat(UUID identityUuid, String statKey, int amount) {
        return CompletableFuture.runAsync(() -> {
            database.getCollection("identities").updateOne(
                    Filters.eq("_id", identityUuid.toString()),
                    Updates.inc("stats." + statKey, amount)
            );
        });
    }
}

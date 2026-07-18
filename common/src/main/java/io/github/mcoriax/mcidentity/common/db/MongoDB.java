package io.github.mcoriax.mcidentity.common.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import io.github.mcoriax.mcidentity.api.db.IMCIdentityDB;
import io.github.mcoriax.mcidentity.api.model.*;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.model.MutableIdentityProperties;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MongoDB implementation of {@link IMCIdentityDB}.
 *
 * <p>Each identity is one document in the {@code identities} collection with
 * embedded properties, stats, cosmetics, and equipped ability and rune maps.
 * The profession, ability, and rune registries and player sessions live in
 * their own collections keyed by unique indexes. Every operation runs
 * asynchronously on a dedicated daemon executor.</p>
 *
 * <p>Failure semantics mirror the other backends: reads resolve to their
 * neutral fallback and writes complete normally after logging, while
 * {@link #createIdentity} and {@link #initializeSchema} complete
 * exceptionally so callers can react.</p>
 */
public class MongoDB implements IMCIdentityDB {

    /**
     * Logger for driver and command failures.
     */
    private static final Logger LOGGER = Logger.getLogger(MongoDB.class.getName());

    /**
     * The client owning the connection pool, or null when a database handle
     * was injected directly for tests.
     */
    private final MongoClient mongoClient;

    /**
     * The database holding every MCIdentity collection.
     */
    private final MongoDatabase database;

    /**
     * Dedicated executor running every database operation off the game thread.
     */
    private final ExecutorService executor;

    /**
     * Creates a MongoDB backend for the given connection string.
     * @param uri The MongoDB connection URI, e.g. {@code mongodb://localhost:27017}.
     * @param dbName The database name.
     */
    public MongoDB(String uri, String dbName) {
        this.mongoClient = MongoClients.create(uri);
        this.database = this.mongoClient.getDatabase(dbName);
        this.executor = createExecutor();
    }

    /**
     * Creates a backend around an existing database handle, used by tests.
     * @param database The database handle.
     */
    protected MongoDB(MongoDatabase database) {
        this.mongoClient = null;
        this.database = database;
        this.executor = createExecutor();
    }

    /**
     * Builds the daemon worker pool shared by every operation.
     * @return The executor service.
     */
    private static ExecutorService createExecutor() {
        AtomicInteger counter = new AtomicInteger(1);
        return Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "MCIdentity-MongoDB-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> initializeSchema() {
        return CompletableFuture.runAsync(() -> {
            IndexOptions uniqueIndex = new IndexOptions().unique(true);
            database.getCollection("identity_professions").createIndex(new Document("system_code", 1), uniqueIndex);
            database.getCollection("identity_abilities").createIndex(new Document("system_code", 1), uniqueIndex);
            database.getCollection("identity_runes").createIndex(new Document("system_code", 1), uniqueIndex);
        }, executor);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> createProfession(String systemCode, String displayName) {
        return runAsync(() -> upsertRegistryEntry("identity_professions",
            new Document("system_code", systemCode).append("display_name", displayName), systemCode));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Collection<Profession>> getProfessions() {
        return supplyAsync(Collections.emptyList(), () -> {
            List<Profession> professions = new ArrayList<>();
            for (Document doc : database.getCollection("identity_professions").find()) {
                String code = doc.getString("system_code");
                String name = doc.getString("display_name");
                professions.add(new Profession() {
                    @Override public String getSystemCode() { return code; }
                    @Override public String getDisplayName() { return name; }
                    @Override public Map<String, Object> getExpConfig() { return MCIdentityManager.getProfessionExpConfig(code); }
                    @Override public Map<String, Integer> getBaseStats() { return MCIdentityManager.getProfessionBaseStats(code); }
                    @Override public String getStatsRandom() { return MCIdentityManager.getProfessionStatsRandom(code); }
                    @Override public Map<Integer, String> getSkins() { return MCIdentityManager.getProfessionSkins(code); }
                    @Override public void setSkins(Map<Integer, String> skins) { MCIdentityManager.setProfessionSkins(code, skins); }
                });
            }
            return professions;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> createAbility(String abilityCode, String displayName, String abilitySkin) {
        return runAsync(() -> upsertRegistryEntry("identity_abilities",
            new Document("system_code", abilityCode)
                .append("display_name", displayName)
                .append("ability_skin", abilitySkin), abilityCode));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Collection<Ability>> getAbilities() {
        return supplyAsync(Collections.emptyList(), () -> {
            List<Ability> abilities = new ArrayList<>();
            for (Document doc : database.getCollection("identity_abilities").find()) {
                abilities.add(new Ability() {
                    @Override public String getAbilityCode() { return doc.getString("system_code"); }
                    @Override public String getDisplayName() { return doc.getString("display_name"); }
                    @Override public String getAbilitySkin() { return doc.getString("ability_skin"); }
                });
            }
            return abilities;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Collection<Rune>> getRunes() {
        return supplyAsync(Collections.emptyList(), () -> {
            List<Rune> runes = new ArrayList<>();
            for (Document doc : database.getCollection("identity_runes").find()) {
                runes.add(new Rune() {
                    @Override public String getSystemCode() { return doc.getString("system_code"); }
                    @Override public String getDisplayName() { return doc.getString("display_name"); }
                });
            }
            return runes;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> createIdentity(IdentityKey key, IdentityProperties properties, IdentityStats stats) {
        String professionCode = key.getProfessionCode();
        if (professionCode == null || professionCode.trim().isEmpty() || professionCode.equalsIgnoreCase("random")) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid profession code: " + professionCode));
        }
        return CompletableFuture.runAsync(() -> {
            try {
                upsertRegistryEntry("identity_professions",
                    new Document("system_code", professionCode).append("display_name", professionCode), professionCode);

                Document propsDoc = new Document("level", properties.getLevel())
                    .append("exp", properties.getExp())
                    .append("stat_points", properties.getStatPoints())
                    .append("head_text", properties.getHeadText());

                Document identityDoc = new Document("_id", key.getUniqueId().toString())
                    .append("profession_code", professionCode)
                    .append("properties", propsDoc)
                    .append("stats", toStatsDocument(stats))
                    .append("cosmetics", new Document("skin", null).append("aura", null))
                    .append("equipped_abilities", new Document())
                    .append("equipped_runes", new Document());

                database.getCollection("identities").insertOne(identityDoc);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create identity in MongoDB", e);
            }
        }, executor);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<IdentityKey>> getIdentityKey(UUID uuid) {
        return supplyAsync(Optional.empty(), () -> {
            Document doc = findIdentity(uuid);
            if (doc == null) {
                return Optional.empty();
            }
            String profession = doc.getString("profession_code");
            return Optional.of(new IdentityKey() {
                @Override public UUID getUniqueId() { return uuid; }
                @Override public String getProfessionCode() { return profession; }
            });
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<IdentityProperties>> getProperties(UUID identityUuid) {
        return supplyAsync(Optional.empty(), () -> {
            Document doc = findIdentity(identityUuid);
            if (doc == null || !doc.containsKey("properties")) {
                return Optional.empty();
            }
            Document props = doc.get("properties", Document.class);
            return Optional.of(new MutableIdentityProperties(
                props.getInteger("level", 1),
                props.getInteger("exp", 0),
                props.getInteger("stat_points", 0),
                props.getString("head_text"),
                null,
                null,
                props.getString("inventory_contents"),
                props.getString("enderchest_contents")
            ));
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<IdentityStats>> getStats(UUID identityUuid) {
        return supplyAsync(Optional.empty(), () -> {
            Document doc = findIdentity(identityUuid);
            if (doc == null || !doc.containsKey("stats")) {
                return Optional.empty();
            }
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
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Integer> getStat(UUID identityUuid, String statKey) {
        if (!StatKeys.isValid(statKey)) {
            return CompletableFuture.completedFuture(0);
        }
        return supplyAsync(0, () -> {
            Document doc = findIdentity(identityUuid);
            if (doc == null || !doc.containsKey("stats")) {
                return 0;
            }
            return doc.get("stats", Document.class).getInteger(statKey, 0);
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Map<Integer, EquippedAbility>> getEquippedAbilities(UUID identityUuid) {
        return supplyAsync(Collections.emptyMap(), () -> {
            Map<Integer, EquippedAbility> slots = new HashMap<>();
            Document doc = findIdentity(identityUuid);
            if (doc != null && doc.containsKey("equipped_abilities")) {
                Document equipped = doc.get("equipped_abilities", Document.class);
                for (String key : equipped.keySet()) {
                    Object value = equipped.get(key);
                    if (value instanceof Document abilityDoc) {
                        slots.put(Integer.parseInt(key), new EquippedAbility(
                            abilityDoc.getString("system_code"),
                            abilityDoc.getString("ability_skin")
                        ));
                    } else if (value instanceof String abilityCode) {
                        slots.put(Integer.parseInt(key), new EquippedAbility(abilityCode, null));
                    }
                }
            }
            return slots;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> setEquippedAbility(UUID identityUuid, int slot, String abilityCode, String abilitySkin) {
        return runAsync(() -> {
            MongoCollection<Document> identities = database.getCollection("identities");
            if (abilityCode == null) {
                identities.updateOne(Filters.eq("_id", identityUuid.toString()), Updates.unset("equipped_abilities." + slot));
            } else {
                Document abilityDoc = new Document("system_code", abilityCode).append("ability_skin", abilitySkin);
                identities.updateOne(Filters.eq("_id", identityUuid.toString()), Updates.set("equipped_abilities." + slot, abilityDoc));
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Map<Integer, String>> getRunes(UUID identityUuid) {
        return supplyAsync(Collections.emptyMap(), () -> {
            Map<Integer, String> slots = new HashMap<>();
            Document doc = findIdentity(identityUuid);
            if (doc != null && doc.containsKey("equipped_runes")) {
                Document equipped = doc.get("equipped_runes", Document.class);
                for (String key : equipped.keySet()) {
                    slots.put(Integer.parseInt(key), equipped.getString(key));
                }
            }
            return slots;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> setRune(UUID identityUuid, int slot, String runeCode) {
        return runAsync(() -> {
            MongoCollection<Document> identities = database.getCollection("identities");
            if (runeCode == null) {
                identities.updateOne(Filters.eq("_id", identityUuid.toString()), Updates.unset("equipped_runes." + slot));
            } else {
                identities.updateOne(Filters.eq("_id", identityUuid.toString()), Updates.set("equipped_runes." + slot, runeCode));
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<UUID>> getActiveIdentity(UUID playerUuid) {
        return supplyAsync(Optional.empty(), () -> {
            Document session = findSession(playerUuid);
            if (session != null && session.getString("identity_uuid") != null) {
                return Optional.of(UUID.fromString(session.getString("identity_uuid")));
            }
            return Optional.empty();
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<IdentitySession>> getSession(UUID playerUuid) {
        return supplyAsync(Optional.empty(), () -> {
            Document session = findSession(playerUuid);
            if (session == null || session.getString("identity_uuid") == null) {
                return Optional.empty();
            }
            UUID identityUuid = UUID.fromString(session.getString("identity_uuid"));
            String contentKey = session.getString("content_key");
            return Optional.of(new IdentitySession() {
                @Override public UUID getPlayerUniqueId() { return playerUuid; }
                @Override public UUID getIdentityUniqueId() { return identityUuid; }
                @Override public String getContentKey() { return contentKey; }
            });
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> setActiveIdentity(UUID playerUuid, UUID identityUuid, String contentKey) {
        return runAsync(() -> {
            MongoCollection<Document> sessions = database.getCollection("identity_sessions");
            if (identityUuid == null) {
                sessions.deleteOne(Filters.eq("_id", playerUuid.toString()));
            } else {
                Document doc = new Document("_id", playerUuid.toString())
                    .append("identity_uuid", identityUuid.toString())
                    .append("content_key", contentKey)
                    .append("equipped_at", new Date());
                sessions.updateOne(Filters.eq("_id", playerUuid.toString()),
                    new Document("$set", doc), new UpdateOptions().upsert(true));
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> logAction(UUID playerUuid, UUID identityUuid, ActionType actionType) {
        return runAsync(() -> {
            Document log = new Document("player_uuid", playerUuid.toString())
                .append("identity_uuid", identityUuid.toString())
                .append("action_type", actionType.name())
                .append("action_time", new Date());
            database.getCollection("identity_logs").insertOne(log);
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateProperties(UUID identityUuid, IdentityProperties properties) {
        return runAsync(() -> {
            Document propsDoc = new Document("level", properties.getLevel())
                .append("exp", properties.getExp())
                .append("stat_points", properties.getStatPoints())
                .append("head_text", properties.getHeadText())
                .append("inventory_contents", properties.getInventoryContents())
                .append("enderchest_contents", properties.getEnderchestContents());
            database.getCollection("identities").updateOne(
                Filters.eq("_id", identityUuid.toString()),
                Updates.set("properties", propsDoc));
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateStats(UUID identityUuid, IdentityStats stats) {
        return runAsync(() -> database.getCollection("identities").updateOne(
            Filters.eq("_id", identityUuid.toString()),
            Updates.set("stats", toStatsDocument(stats))));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> addStat(UUID identityUuid, String statKey, int amount) {
        if (!StatKeys.isValid(statKey)) {
            return CompletableFuture.completedFuture(null);
        }
        return runAsync(() -> database.getCollection("identities").updateOne(
            Filters.eq("_id", identityUuid.toString()),
            Updates.inc("stats." + statKey, amount)));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Map<String, String>> getCosmetics(UUID uuid) {
        return supplyAsync(Collections.emptyMap(), () -> {
            Map<String, String> cosmetics = new HashMap<>();
            Document doc = findIdentity(uuid);
            if (doc != null && doc.containsKey("cosmetics")) {
                Document embedded = doc.get("cosmetics", Document.class);
                cosmetics.put("skin", embedded.getString("skin"));
                cosmetics.put("aura", embedded.getString("aura"));
            }
            return cosmetics;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> saveSkin(UUID uuid, String skin) {
        return runAsync(() -> database.getCollection("identities").updateOne(
            Filters.eq("_id", uuid.toString()), Updates.set("cosmetics.skin", skin)));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> saveAura(UUID uuid, String aura) {
        return runAsync(() -> database.getCollection("identities").updateOne(
            Filters.eq("_id", uuid.toString()), Updates.set("cosmetics.aura", aura)));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateSessionContentKey(UUID playerUuid, String contentKey) {
        return runAsync(() -> database.getCollection("identity_sessions").updateOne(
            Filters.eq("_id", playerUuid.toString()), Updates.set("content_key", contentKey)));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Integer> getBaseIdentitySlots(UUID playerUuid) {
        return supplyAsync(0, () -> {
            Document doc = database.getCollection("player_base_slots")
                .find(Filters.eq("_id", playerUuid.toString())).first();
            return doc != null ? doc.getInteger("slots", 0) : 0;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Integer> addBaseIdentitySlots(UUID playerUuid, int amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MongoCollection<Document> slots = database.getCollection("player_base_slots");
                Document updated = slots.findOneAndUpdate(
                    Filters.eq("_id", playerUuid.toString()),
                    Updates.inc("slots", amount),
                    new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
                int next = updated != null ? updated.getInteger("slots", 0) : 0;
                if (next < 0) {
                    // The slot count never drops below zero.
                    slots.updateOne(Filters.eq("_id", playerUuid.toString()), Updates.set("slots", 0));
                    next = 0;
                }
                return next;
            } catch (Exception e) {
                throw new RuntimeException("Failed to update base identity slots in MongoDB", e);
            }
        }, executor);
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown() {
        executor.shutdown();
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    // --- Internal helpers ---

    /**
     * Upserts one registry entry keyed by its system code.
     * @param collection The registry collection name.
     * @param entry The full entry document.
     * @param systemCode The unique system code.
     */
    private void upsertRegistryEntry(String collection, Document entry, String systemCode) {
        database.getCollection(collection).updateOne(
            Filters.eq("system_code", systemCode),
            new Document("$set", entry),
            new UpdateOptions().upsert(true));
    }

    /**
     * Finds one identity document by UUID.
     * @param uuid The identity UUID.
     * @return The document, or null when absent.
     */
    private Document findIdentity(UUID uuid) {
        return database.getCollection("identities").find(Filters.eq("_id", uuid.toString())).first();
    }

    /**
     * Finds one session document by player UUID.
     * @param playerUuid The player UUID.
     * @return The document, or null when absent.
     */
    private Document findSession(UUID playerUuid) {
        return database.getCollection("identity_sessions").find(Filters.eq("_id", playerUuid.toString())).first();
    }

    /**
     * Serializes a stats view into its embedded document form.
     * @param stats The stats to serialize.
     * @return The document with all nine stat fields.
     */
    private static Document toStatsDocument(IdentityStats stats) {
        return new Document("energy", stats.getEnergy())
            .append("health", stats.getHealth())
            .append("food", stats.getFood())
            .append("strength", stats.getStrength())
            .append("defense", stats.getDefense())
            .append("wisdom", stats.getWisdom())
            .append("luck", stats.getLuck())
            .append("agility", stats.getAgility())
            .append("resilience", stats.getResilience());
    }

    /**
     * Runs a write asynchronously, logging any driver failure without
     * failing the returned future.
     * @param action The write to run.
     * @return A future that completes once the write finishes.
     */
    private CompletableFuture<Void> runAsync(Runnable action) {
        return CompletableFuture.runAsync(() -> {
            try {
                action.run();
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "MongoDB write failed", e);
            }
        }, executor);
    }

    /**
     * Supplies a read asynchronously, returning a fallback on failure.
     * @param fallback The value to return if the read fails.
     * @param action The read producing the result.
     * @param <T> The result type.
     * @return A future that completes with the result, or the fallback on failure.
     */
    @SuppressWarnings("unchecked")
    private <T> CompletableFuture<T> supplyAsync(Object fallback, Supplier<T> action) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return action.get();
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "MongoDB read failed", e);
                return (T) fallback;
            }
        }, executor);
    }
}

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
import io.github.mcoriax.mcidentity.common.IdentityAlias;
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
 * embedded properties and stats. The profession registry and player sessions
 * live in their own collections keyed by unique indexes. Every operation runs asynchronously on
 * a dedicated daemon executor.</p>
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
     * Path of the embedded field holding the alias exactly as the player typed it.
     */
    private static final String ALIAS_FIELD = "properties.alias";

    /**
     * Path of the embedded field holding the folded alias. MongoDB compares
     * strings byte by byte under the default collation, so the folded mirror is
     * what makes uniqueness and lookups case-insensitive.
     */
    private static final String ALIAS_COMPARABLE_FIELD = "properties.alias_comparable";

    /**
     * Collection holding one document per identity, properties and stats embedded.
     */
    private static final String COLLECTION_IDENTITIES = "identities";

    /**
     * Collection holding the profession registry.
     */
    private static final String COLLECTION_PROFESSIONS = "identity_professions";

    /**
     * Collection holding one document per player's equipped-identity session.
     */
    private static final String COLLECTION_SESSIONS = "identity_sessions";

    /**
     * Field carrying a profession's stable system code.
     */
    private static final String FIELD_SYSTEM_CODE = "system_code";

    /**
     * Field carrying a profession's human-readable name.
     */
    private static final String FIELD_DISPLAY_NAME = "display_name";

    /**
     * Embedded document holding an identity's properties.
     */
    private static final String FIELD_PROPERTIES = "properties";

    /**
     * Embedded document holding an identity's stats.
     */
    private static final String FIELD_STATS = "stats";

    /**
     * Field carrying the identity UUID a session points at.
     */
    private static final String FIELD_IDENTITY_UUID = "identity_uuid";

    /**
     * Field carrying a session's content key.
     */
    private static final String FIELD_CONTENT_KEY = "content_key";

    /**
     * Field carrying a player's unlocked base-identity slot count.
     */
    private static final String FIELD_SLOTS = "slots";

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
            dropRetiredAddonData();

            IndexOptions uniqueIndex = new IndexOptions().unique(true);
            database.getCollection(COLLECTION_PROFESSIONS).createIndex(new Document(FIELD_SYSTEM_CODE, 1), uniqueIndex);

            // Aliases are unique network-wide and compared without case, so the
            // index covers the folded mirror field rather than the stored
            // value. It is sparse because most identities have no alias, and
            // a unique index would otherwise reject every one of them past the
            // first.
            database.getCollection(COLLECTION_IDENTITIES).createIndex(
                new Document(ALIAS_COMPARABLE_FIELD, 1),
                new IndexOptions().unique(true).sparse(true));
        }, executor);
    }

    /**
     * Removes the ability data this plugin no longer owns.
     *
     * <p>Three systems have moved out of the core plugin into standalone
     * add-ons: skills to MCIdentitySkill, and runes and cosmetics to
     * MCIdentityRune and MCIdentityCosmetic. Their collections and per-identity
     * fields are therefore removed on the first start after upgrading, and are
     * <strong>not</strong> recoverable afterwards.</p>
     */
    private void dropRetiredAddonData() {
        database.getCollection("identity_abilities").drop();
        database.getCollection("identity_runes").drop();
        for (String field : new String[] {"equipped_abilities", "equipped_runes", "cosmetics"}) {
            database.getCollection(COLLECTION_IDENTITIES).updateMany(
                Filters.exists(field), Updates.unset(field));
        }
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> createProfession(String systemCode, String displayName) {
        return runAsync(() -> upsertRegistryEntry(COLLECTION_PROFESSIONS,
            new Document(FIELD_SYSTEM_CODE, systemCode).append(FIELD_DISPLAY_NAME, displayName), systemCode));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Collection<Profession>> getProfessions() {
        return supplyAsync(Collections.emptyList(), () -> {
            List<Profession> professions = new ArrayList<>();
            for (Document doc : database.getCollection(COLLECTION_PROFESSIONS).find()) {
                String code = doc.getString(FIELD_SYSTEM_CODE);
                String name = doc.getString(FIELD_DISPLAY_NAME);
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
    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> createIdentity(IdentityKey key, IdentityProperties properties, IdentityStats stats) {
        String professionCode = key.getProfessionCode();
        if (professionCode == null || professionCode.trim().isEmpty() || professionCode.equalsIgnoreCase("random")) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid profession code: " + professionCode));
        }
        return CompletableFuture.runAsync(() -> {
            try {
                upsertRegistryEntry(COLLECTION_PROFESSIONS,
                    new Document(FIELD_SYSTEM_CODE, professionCode).append(FIELD_DISPLAY_NAME, professionCode), professionCode);

                Document propsDoc = new Document("level", properties.getLevel())
                    .append("exp", properties.getExp())
                    .append("stat_points", properties.getStatPoints())
                    .append("head_text", properties.getHeadText());

                Document identityDoc = new Document("_id", key.getUniqueId().toString())
                    .append("profession_code", professionCode)
                    .append(FIELD_PROPERTIES, propsDoc)
                    .append(FIELD_STATS, toStatsDocument(stats));

                database.getCollection(COLLECTION_IDENTITIES).insertOne(identityDoc);
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
            if (doc == null || !doc.containsKey(FIELD_PROPERTIES)) {
                return Optional.empty();
            }
            Document props = doc.get(FIELD_PROPERTIES, Document.class);
            return Optional.of(new MutableIdentityProperties(
                props.getString("alias"),
                props.getInteger("level", 1),
                props.getInteger("exp", 0),
                props.getInteger("stat_points", 0),
                props.getString("head_text"),
                props.getString("inventory_contents"),
                props.getString("enderchest_contents")
            ));
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<UUID>> getIdentityByAlias(String alias) {
        if (!IdentityAlias.isValid(alias)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return supplyAsync(Optional.empty(), () -> {
            Document doc = findByAlias(alias);
            return doc != null ? Optional.of(UUID.fromString(doc.getString("_id"))) : Optional.empty();
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Boolean> setAlias(UUID identityUuid, String alias) {
        if (alias != null && !IdentityAlias.isValid(alias)) {
            return CompletableFuture.completedFuture(false);
        }
        return supplyAsync(Boolean.FALSE, () -> {
            if (alias != null) {
                Document holder = findByAlias(alias);
                if (holder != null && !identityUuid.toString().equals(holder.getString("_id"))) {
                    return Boolean.FALSE;
                }
            }
            // The unique index is the backstop for a lost race here: a
            // concurrent claim makes this write throw, which supplyAsync
            // resolves to false, so the alias is never duplicated.
            return database.getCollection(COLLECTION_IDENTITIES).updateOne(
                Filters.eq("_id", identityUuid.toString()),
                alias != null
                    ? Updates.combine(
                        Updates.set(ALIAS_FIELD, alias),
                        Updates.set(ALIAS_COMPARABLE_FIELD, IdentityAlias.comparable(alias)))
                    : Updates.combine(
                        Updates.unset(ALIAS_FIELD),
                        Updates.unset(ALIAS_COMPARABLE_FIELD))
            ).getMatchedCount() > 0;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<IdentityStats>> getStats(UUID identityUuid) {
        return supplyAsync(Optional.empty(), () -> {
            Document doc = findIdentity(identityUuid);
            if (doc == null || !doc.containsKey(FIELD_STATS)) {
                return Optional.empty();
            }
            Document stats = doc.get(FIELD_STATS, Document.class);
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
            if (doc == null || !doc.containsKey(FIELD_STATS)) {
                return 0;
            }
            return doc.get(FIELD_STATS, Document.class).getInteger(statKey, 0);
        });
    }

    /** {@inheritDoc} */
    /** {@inheritDoc} */
    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<UUID>> getActiveIdentity(UUID playerUuid) {
        return supplyAsync(Optional.empty(), () -> {
            Document session = findSession(playerUuid);
            if (session != null && session.getString(FIELD_IDENTITY_UUID) != null) {
                return Optional.of(UUID.fromString(session.getString(FIELD_IDENTITY_UUID)));
            }
            return Optional.empty();
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<IdentitySession>> getSession(UUID playerUuid) {
        return supplyAsync(Optional.empty(), () -> {
            Document session = findSession(playerUuid);
            if (session == null || session.getString(FIELD_IDENTITY_UUID) == null) {
                return Optional.empty();
            }
            UUID identityUuid = UUID.fromString(session.getString(FIELD_IDENTITY_UUID));
            String contentKey = session.getString(FIELD_CONTENT_KEY);
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
            MongoCollection<Document> sessions = database.getCollection(COLLECTION_SESSIONS);
            if (identityUuid == null) {
                sessions.deleteOne(Filters.eq("_id", playerUuid.toString()));
            } else {
                Document doc = new Document("_id", playerUuid.toString())
                    .append(FIELD_IDENTITY_UUID, identityUuid.toString())
                    .append(FIELD_CONTENT_KEY, contentKey)
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
                .append(FIELD_IDENTITY_UUID, identityUuid.toString())
                .append("action_type", actionType.name())
                .append("action_time", new Date());
            database.getCollection("identity_logs").insertOne(log);
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateProperties(UUID identityUuid, IdentityProperties properties) {
        return runAsync(() -> {
            // Each field is set individually rather than replacing the whole
            // subdocument, so the alias — claimed through setAlias, which
            // checks uniqueness — survives a property write untouched.
            database.getCollection(COLLECTION_IDENTITIES).updateOne(
                Filters.eq("_id", identityUuid.toString()),
                Updates.combine(
                    Updates.set("properties.level", properties.getLevel()),
                    Updates.set("properties.exp", properties.getExp()),
                    Updates.set("properties.stat_points", properties.getStatPoints()),
                    Updates.set("properties.head_text", properties.getHeadText()),
                    Updates.set("properties.inventory_contents", properties.getInventoryContents()),
                    Updates.set("properties.enderchest_contents", properties.getEnderchestContents())));
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateStats(UUID identityUuid, IdentityStats stats) {
        return runAsync(() -> database.getCollection(COLLECTION_IDENTITIES).updateOne(
            Filters.eq("_id", identityUuid.toString()),
            Updates.set(FIELD_STATS, toStatsDocument(stats))));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> addStat(UUID identityUuid, String statKey, int amount) {
        if (!StatKeys.isValid(statKey)) {
            return CompletableFuture.completedFuture(null);
        }
        return runAsync(() -> database.getCollection(COLLECTION_IDENTITIES).updateOne(
            Filters.eq("_id", identityUuid.toString()),
            Updates.inc("stats." + statKey, amount)));
    }

    /** {@inheritDoc} */
    /** {@inheritDoc} */
    /** {@inheritDoc} */
    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateSessionContentKey(UUID playerUuid, String contentKey) {
        return runAsync(() -> database.getCollection(COLLECTION_SESSIONS).updateOne(
            Filters.eq("_id", playerUuid.toString()), Updates.set(FIELD_CONTENT_KEY, contentKey)));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Integer> getBaseIdentitySlots(UUID playerUuid) {
        return supplyAsync(0, () -> {
            Document doc = database.getCollection("player_base_slots")
                .find(Filters.eq("_id", playerUuid.toString())).first();
            return doc != null ? doc.getInteger(FIELD_SLOTS, 0) : 0;
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
                    Updates.inc(FIELD_SLOTS, amount),
                    new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
                int next = updated != null ? updated.getInteger(FIELD_SLOTS, 0) : 0;
                if (next < 0) {
                    // The slot count never drops below zero.
                    slots.updateOne(Filters.eq("_id", playerUuid.toString()), Updates.set(FIELD_SLOTS, 0));
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
            Filters.eq(FIELD_SYSTEM_CODE, systemCode),
            new Document("$set", entry),
            new UpdateOptions().upsert(true));
    }

    /**
     * Finds one identity document by UUID.
     * @param uuid The identity UUID.
     * @return The document, or null when absent.
     */
    private Document findIdentity(UUID uuid) {
        return database.getCollection(COLLECTION_IDENTITIES).find(Filters.eq("_id", uuid.toString())).first();
    }

    /**
     * Finds the identity document holding an alias, ignoring case.
     * @param alias The alias to resolve.
     * @return The document, or null when the alias is unclaimed.
     */
    private Document findByAlias(String alias) {
        return database.getCollection(COLLECTION_IDENTITIES)
            .find(Filters.eq(ALIAS_COMPARABLE_FIELD, IdentityAlias.comparable(alias))).first();
    }

    /**
     * Finds one session document by player UUID.
     * @param playerUuid The player UUID.
     * @return The document, or null when absent.
     */
    private Document findSession(UUID playerUuid) {
        return database.getCollection(COLLECTION_SESSIONS).find(Filters.eq("_id", playerUuid.toString())).first();
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

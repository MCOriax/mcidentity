package io.github.mcoriax.mcidentity.common.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import io.github.mcoriax.mcidentity.api.db.IMCIdentityDB;
import io.github.mcoriax.mcidentity.api.model.*;
import io.github.mcoriax.mcidentity.common.IdentityAlias;
import io.github.mcoriax.mcidentity.common.model.ConfiguredProfession;
import io.github.mcoriax.mcidentity.common.model.IdentityKeyRecord;
import io.github.mcoriax.mcidentity.common.model.IdentitySessionRecord;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.ALIAS_COMPARABLE_FIELD;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.COLLECTION_IDENTITIES;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.COLLECTION_PROFESSIONS;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.COLLECTION_SESSIONS;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.FIELD_CONTENT_KEY;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.FIELD_DISPLAY_NAME;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.FIELD_IDENTITY_UUID;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.FIELD_PROPERTIES;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.FIELD_SLOTS;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.FIELD_STATS;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.FIELD_SYSTEM_CODE;

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
        this.executor = DatabaseExecutors.daemonPool(4, "MCIdentity-MongoDB");
    }

    /**
     * Creates a backend around an existing database handle, used by tests.
     * @param database The database handle.
     */
    protected MongoDB(MongoDatabase database) {
        this.mongoClient = null;
        this.database = database;
        this.executor = DatabaseExecutors.daemonPool(4, "MCIdentity-MongoDB");
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> initializeSchema() {
        return CompletableFuture.runAsync(() -> MongoSchema.initialize(database), executor);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> createProfession(String systemCode, String displayName) {
        return runAsync(() -> upsertProfession(systemCode, displayName));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Collection<Profession>> getProfessions() {
        return supplyAsync(Collections.emptyList(), () -> {
            List<Profession> professions = new ArrayList<>();
            for (Document doc : database.getCollection(COLLECTION_PROFESSIONS).find()) {
                professions.add(new ConfiguredProfession(doc.getString(FIELD_SYSTEM_CODE), doc.getString(FIELD_DISPLAY_NAME)));
            }
            return professions;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> createIdentity(IdentityKey key, IdentityProperties properties, IdentityStats stats) {
        String professionCode = key.getProfessionCode();
        if (!StorableProfession.isStorable(professionCode)) {
            return StorableProfession.rejected(professionCode);
        }
        return CompletableFuture.runAsync(() -> {
            try {
                upsertProfession(professionCode, professionCode);
                database.getCollection(COLLECTION_IDENTITIES).insertOne(
                    MongoDocuments.newIdentity(key, properties, stats));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to create identity in MongoDB", e);
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
            return Optional.of(new IdentityKeyRecord(uuid, doc.getString("profession_code")));
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
            return Optional.of(MongoDocuments.readProperties(doc.get(FIELD_PROPERTIES, Document.class)));
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
                MongoDocuments.aliasUpdate(alias)
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
            return Optional.of(MongoDocuments.readStats(doc.get(FIELD_STATS, Document.class)));
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
            return Optional.of(new IdentitySessionRecord(playerUuid,
                UUID.fromString(session.getString(FIELD_IDENTITY_UUID)), session.getString(FIELD_CONTENT_KEY)));
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
                sessions.updateOne(Filters.eq("_id", playerUuid.toString()),
                    new Document("$set", MongoDocuments.session(playerUuid, identityUuid, contentKey)),
                    new UpdateOptions().upsert(true));
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> logAction(UUID playerUuid, UUID identityUuid, ActionType actionType) {
        return runAsync(() -> database.getCollection("identity_logs")
            .insertOne(MongoDocuments.logEntry(playerUuid, identityUuid, actionType)));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateProperties(UUID identityUuid, IdentityProperties properties) {
        // The alias is left out of the update; see MongoDocuments.propertiesUpdate.
        return runAsync(() -> database.getCollection(COLLECTION_IDENTITIES).updateOne(
            Filters.eq("_id", identityUuid.toString()),
            MongoDocuments.propertiesUpdate(properties)));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateStats(UUID identityUuid, IdentityStats stats) {
        return runAsync(() -> database.getCollection(COLLECTION_IDENTITIES).updateOne(
            Filters.eq("_id", identityUuid.toString()),
            Updates.set(FIELD_STATS, MongoDocuments.toStatsDocument(stats))));
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
                throw new IllegalStateException("Failed to update base identity slots in MongoDB", e);
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
     * Upserts one profession registry entry keyed by its system code.
     * @param systemCode The unique system code.
     * @param displayName The display name.
     */
    private void upsertProfession(String systemCode, String displayName) {
        database.getCollection(COLLECTION_PROFESSIONS).updateOne(
            Filters.eq(FIELD_SYSTEM_CODE, systemCode),
            new Document("$set", MongoDocuments.professionEntry(systemCode, displayName)),
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

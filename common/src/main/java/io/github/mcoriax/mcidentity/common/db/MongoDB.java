package io.github.mcoriax.mcidentity.common.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import io.github.mcoriax.mcidentity.api.db.IMCIdentityDB;
import io.github.mcoriax.mcidentity.api.model.ActionType;
import io.github.mcoriax.mcidentity.api.model.PlayerStats;
import io.github.mcoriax.mcidentity.api.model.Profession;
import io.github.mcoriax.mcidentity.api.model.ProfessionProgress;
import io.github.mcoriax.mcidentity.common.model.ConfiguredProfession;
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

import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.COLLECTION_ACTIVE;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.COLLECTION_LOGS;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.COLLECTION_PROFESSIONS;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.COLLECTION_PROGRESS;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.COLLECTION_STATS;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.FIELD_ACTIVE_PROFESSION;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.FIELD_DISPLAY_NAME;
import static io.github.mcoriax.mcidentity.common.db.MongoDocuments.FIELD_SYSTEM_CODE;

/**
 * MongoDB implementation of {@link IMCIdentityDB}.
 *
 * <p>Each player has one document in {@code player_stats} and one in
 * {@code player_professions}, both keyed by the player UUID, and one document
 * per profession they have worn in {@code player_profession_progress}. The
 * profession registry lives in {@code identity_professions}. Every operation
 * runs asynchronously on a dedicated daemon executor.</p>
 *
 * <p>Failure semantics mirror the other backends: reads resolve to their
 * neutral fallback and writes complete normally after logging, while
 * {@link #createPlayer} and {@link #initializeSchema} complete
 * exceptionally so callers can react.</p>
 */
public class MongoDB implements IMCIdentityDB {

    /**
     * Logger for read and write failures.
     */
    private static final Logger LOGGER = Logger.getLogger(MongoDB.class.getName());

    /**
     * The client this backend opened, or null when the database was handed in.
     */
    private final MongoClient mongoClient;

    /**
     * The database holding every collection.
     */
    private final MongoDatabase database;

    /**
     * Runs every operation off the game thread.
     */
    private final ExecutorService executor;

    /**
     * Connects to a MongoDB deployment.
     * @param uri The connection string.
     * @param dbName The database name.
     */
    public MongoDB(String uri, String dbName) {
        this.mongoClient = MongoClients.create(uri);
        this.database = this.mongoClient.getDatabase(dbName);
        this.executor = DatabaseExecutors.daemonPool(4, "MCIdentity-MongoDB");
    }

    /**
     * Uses an already opened database, for tests.
     * @param database The database.
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
        return runAsync(() -> database.getCollection(COLLECTION_PROFESSIONS).updateOne(
            Filters.eq(FIELD_SYSTEM_CODE, systemCode),
            new Document("$set", MongoDocuments.professionEntry(systemCode, displayName)),
            new UpdateOptions().upsert(true)));
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
    public CompletableFuture<Void> createPlayer(UUID playerUuid, PlayerStats stats) {
        return CompletableFuture.runAsync(() -> {
            try {
                database.getCollection(COLLECTION_STATS).updateOne(MongoDocuments.byPlayer(playerUuid),
                    new Document("$setOnInsert", MongoDocuments.toStatsDocument(stats)),
                    new UpdateOptions().upsert(true));
            } catch (RuntimeException e) {
                throw new IllegalStateException("Failed to create the player's stats in MongoDB", e);
            }
        }, executor);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<ProfessionProgress>> getProgress(UUID playerUuid, String professionCode) {
        return supplyAsync(Optional.empty(), () -> {
            Document doc = database.getCollection(COLLECTION_PROGRESS)
                .find(MongoDocuments.byProgress(playerUuid, professionCode)).first();
            return doc == null ? Optional.empty() : Optional.of(MongoDocuments.readProgress(doc));
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateProgress(UUID playerUuid, String professionCode, ProfessionProgress progress) {
        return runAsync(() -> database.getCollection(COLLECTION_PROGRESS).updateOne(
            MongoDocuments.byProgress(playerUuid, professionCode),
            new Document("$set", MongoDocuments.toProgressDocument(progress)),
            new UpdateOptions().upsert(true)));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<PlayerStats>> getStats(UUID playerUuid) {
        return supplyAsync(Optional.empty(), () -> {
            Document doc = database.getCollection(COLLECTION_STATS).find(MongoDocuments.byPlayer(playerUuid)).first();
            return doc == null ? Optional.empty() : Optional.of(MongoDocuments.readStats(doc));
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Integer> getStat(UUID playerUuid, String statKey) {
        if (!StatKeys.isValid(statKey)) {
            return CompletableFuture.completedFuture(0);
        }
        return supplyAsync(0, () -> {
            Document doc = database.getCollection(COLLECTION_STATS).find(MongoDocuments.byPlayer(playerUuid)).first();
            return doc == null ? 0 : doc.getInteger(statKey, 0);
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<String>> getActiveProfession(UUID playerUuid) {
        return supplyAsync(Optional.empty(), () -> {
            Document doc = database.getCollection(COLLECTION_ACTIVE).find(MongoDocuments.byPlayer(playerUuid)).first();
            return doc == null ? Optional.empty() : Optional.ofNullable(doc.getString(FIELD_ACTIVE_PROFESSION));
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> setActiveProfession(UUID playerUuid, String professionCode) {
        return runAsync(() -> database.getCollection(COLLECTION_ACTIVE).updateOne(
            MongoDocuments.byPlayer(playerUuid),
            new Document("$set", MongoDocuments.activeProfession(professionCode)),
            new UpdateOptions().upsert(true)));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> logAction(UUID playerUuid, String professionCode, ActionType actionType) {
        return runAsync(() -> database.getCollection(COLLECTION_LOGS)
            .insertOne(MongoDocuments.logEntry(playerUuid, professionCode, actionType)));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateStats(UUID playerUuid, PlayerStats stats) {
        return runAsync(() -> database.getCollection(COLLECTION_STATS).updateOne(
            MongoDocuments.byPlayer(playerUuid),
            new Document("$set", MongoDocuments.toStatsDocument(stats))));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> addStat(UUID playerUuid, String statKey, int amount) {
        if (!StatKeys.isValid(statKey)) {
            return CompletableFuture.completedFuture(null);
        }
        return runAsync(() -> database.getCollection(COLLECTION_STATS).updateOne(
            MongoDocuments.byPlayer(playerUuid), Updates.inc(statKey, amount)));
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
     * Runs a write asynchronously, logging any failure without failing the future.
     * @param action The write.
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
     * Runs a read asynchronously, resolving to a fallback on failure.
     * @param fallback The value returned when the read fails.
     * @param action The read.
     * @param <T> The result type.
     * @return A future with the result, or the fallback.
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

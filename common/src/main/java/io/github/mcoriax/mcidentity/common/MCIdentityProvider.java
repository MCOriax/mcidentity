package io.github.mcoriax.mcidentity.common;

import io.github.mcoriax.mcidentity.api.IdentityProvider;
import io.github.mcoriax.mcidentity.api.db.IMCIdentityDB;
import io.github.mcoriax.mcidentity.api.model.ActionType;
import io.github.mcoriax.mcidentity.api.model.ActiveProfessionSession;
import io.github.mcoriax.mcidentity.api.model.PlayerStats;
import io.github.mcoriax.mcidentity.api.model.Profession;
import io.github.mcoriax.mcidentity.api.model.ProfessionProgress;
import io.github.mcoriax.mcidentity.common.model.ProfessionProgressRecord;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Global provider of the MCIdentity API, the profession manager.
 *
 * <p>Owns persistence: every read and write goes through the configured
 * {@link IMCIdentityDB}, and every write that touches what an online player
 * wears also refreshes the RAM session in {@link MCIdentityManager}. All data is
 * keyed by the player's own UUID.</p>
 */
public class MCIdentityProvider implements IdentityProvider {

    /**
     * The provider of the running plugin, read reflectively by add-ons.
     */
    public static volatile MCIdentityProvider instance;

    /**
     * The storage backend.
     */
    private final IMCIdentityDB database;

    /**
     * Creates the provider over a storage backend and publishes it as {@link #instance}.
     * @param db The storage backend.
     */
    public MCIdentityProvider(IMCIdentityDB db) {
        this.database = Objects.requireNonNull(db, "Database cannot be null");
        instance = this;
    }

    /**
     * Creates the provider over the backend named by {@code database.type}.
     * @param type The backend type, such as {@code sqlite} or {@code central}.
     * @param options The backend's connection options.
     * @return The provider.
     */
    public static MCIdentityProvider create(String type, Map<String, Object> options) {
        return new MCIdentityProvider(DatabaseFactory.create(type, options));
    }

    /**
     * Reports whether the provider is up and has a storage backend.
     * @return true once the plugin has created the provider.
     */
    public static boolean hasDatabase() {
        return instance != null && instance.database != null;
    }

    /**
     * Creates or migrates the storage schema.
     * @return A future that completes once storage is ready.
     */
    public CompletableFuture<Void> initializeStorage() {
        return database.initializeSchema();
    }

    /**
     * Reports whether storage lives on the central server.
     * @return true for the central backend.
     */
    public boolean isRemoteStorage() {
        return DatabaseFactory.isRemote(database);
    }

    /**
     * Checks that storage is reachable.
     * @return A future with true when it answers.
     */
    public CompletableFuture<Boolean> pingStorage() {
        return DatabaseFactory.ping(database);
    }

    // --- Professions ---

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> createProfession(String systemCode, String displayName) {
        return database.createProfession(systemCode.toLowerCase(Locale.ROOT), displayName);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Collection<Profession>> getProfessions() {
        return database.getProfessions();
    }

    // --- The worn profession ---

    /** {@inheritDoc} */
    @Override
    public Optional<String> getActiveProfession(UUID playerUuid) {
        return MCIdentityManager.getActiveProfession(playerUuid);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<String>> loadActiveProfession(UUID playerUuid) {
        return database.getActiveProfession(playerUuid);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> setActiveProfession(UUID playerUuid, String professionCode) {
        String code = normalize(professionCode);
        return database.setActiveProfession(playerUuid, code)
            .thenCompose(v -> progressOrStarting(playerUuid, code))
            .thenAccept(progress -> MCIdentityManager.updateProfession(playerUuid, code, progress));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> clearActiveProfession(UUID playerUuid) {
        return setActiveProfession(playerUuid, BaseProfession.CODE);
    }

    // --- Progress and stats ---

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<ProfessionProgress>> getProgress(UUID playerUuid, String professionCode) {
        return database.getProgress(playerUuid, normalize(professionCode));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateProgress(UUID playerUuid, String professionCode, ProfessionProgress progress) {
        String code = normalize(professionCode);
        return database.updateProgress(playerUuid, code, progress)
            .thenRun(() -> MCIdentityManager.updateProgress(playerUuid, code, progress));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<PlayerStats>> getStats(UUID playerUuid) {
        return database.getStats(playerUuid);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateStats(UUID playerUuid, PlayerStats stats) {
        return database.updateStats(playerUuid, stats)
            .thenRun(() -> MCIdentityManager.updateStats(playerUuid, stats));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> addStat(UUID playerUuid, String statKey, int amount) {
        return database.addStat(playerUuid, statKey.toLowerCase(Locale.ROOT), amount);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Integer> getStat(UUID playerUuid, String statKey) {
        return database.getStat(playerUuid, statKey.toLowerCase(Locale.ROOT));
    }

    /**
     * Creates a player's stats row when it does not exist yet.
     * @param playerUuid The player's UUID.
     * @param stats The starting stats.
     * @return A future that completes once the row exists.
     */
    public CompletableFuture<Void> createPlayer(UUID playerUuid, PlayerStats stats) {
        return database.createPlayer(playerUuid, stats);
    }

    /**
     * Loads an online player's worn profession, its progress and their stats
     * into RAM, defaulting to the base profession when none is stored.
     * <p>The player's stats row must already exist; see {@link #createPlayer}.</p>
     * @param playerUuid The player's UUID.
     * @return A future with the cached session, or empty when the stats could not be read.
     */
    public CompletableFuture<Optional<ActiveProfessionSession>> loadSession(UUID playerUuid) {
        return database.getActiveProfession(playerUuid).thenCompose(optCode -> {
            String code = normalize(optCode.orElse(BaseProfession.CODE));
            CompletableFuture<ProfessionProgress> progressFuture = progressOrStarting(playerUuid, code);
            CompletableFuture<Optional<PlayerStats>> statsFuture = database.getStats(playerUuid);
            return progressFuture.thenCombine(statsFuture, (progress, optStats) -> optStats.map(stats -> {
                ActiveProfessionSession session = new ActiveProfessionSession(playerUuid, code, progress, stats,
                    stats.getHealth(), stats.getEnergy(), stats.getFood());
                MCIdentityManager.cacheSession(session);
                return session;
            }));
        });
    }

    // --- Audit ---

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> logAction(UUID playerUuid, String professionCode, ActionType actionType) {
        return database.logAction(playerUuid, normalize(professionCode), actionType);
    }

    /**
     * Releases the storage backend.
     */
    public void shutdown() {
        if (database != null) {
            database.shutdown();
        }
    }

    // --- Swap cooldown ---

    /** {@inheritDoc} */
    @Override
    public boolean isOnSwapCooldown(UUID playerUuid) {
        return MCIdentityManager.isOnSwapCooldown(playerUuid);
    }

    /** {@inheritDoc} */
    @Override
    public void setSwapCooldown(UUID playerUuid) {
        MCIdentityManager.setSwapCooldown(playerUuid);
    }

    /** {@inheritDoc} */
    @Override
    public void clearSwapCooldown(UUID playerUuid) {
        MCIdentityManager.clearSwapCooldown(playerUuid);
    }

    /** {@inheritDoc} */
    @Override
    public long getRemainingSwapCooldownMillis(UUID playerUuid) {
        return MCIdentityManager.getRemainingSwapCooldownMillis(playerUuid);
    }

    /**
     * Formats a duration as minutes and seconds.
     * @param millis The duration.
     * @return The text, such as {@code 2m 5s}.
     */
    public String formatTime(long millis) {
        return MCIdentityManager.formatTime(millis);
    }

    /**
     * Reports whether a player already has a profession change in flight.
     * @param playerUuid The player's UUID.
     * @return true while one is pending.
     */
    public boolean isEquipPending(UUID playerUuid) {
        return MCIdentityManager.isEquipPending(playerUuid);
    }

    /**
     * Marks whether a player has a profession change in flight.
     * @param playerUuid The player's UUID.
     * @param pending true to mark one as pending, false to clear it.
     */
    public void setEquipPending(UUID playerUuid, boolean pending) {
        MCIdentityManager.setEquipPending(playerUuid, pending);
    }

    // --- Internal helpers ---

    /**
     * Reads a profession's progress, falling back to the starting progress for a
     * profession the player never wore.
     * @param playerUuid The player's UUID.
     * @param professionCode The normalized profession code.
     * @return A future with the progress.
     */
    private CompletableFuture<ProfessionProgress> progressOrStarting(UUID playerUuid, String professionCode) {
        return database.getProgress(playerUuid, professionCode)
            .thenApply(opt -> opt.orElseGet(ProfessionProgressRecord::starting));
    }

    /**
     * Folds a profession code to the lower case form storage keys by.
     * @param professionCode The code as given.
     * @return The folded code.
     */
    private static String normalize(String professionCode) {
        return Objects.requireNonNull(professionCode, "professionCode").trim().toLowerCase(Locale.ROOT);
    }
}

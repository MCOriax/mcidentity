package io.github.mcoriax.mcidentity.api.db;

import io.github.mcoriax.mcidentity.api.model.ActionType;
import io.github.mcoriax.mcidentity.api.model.PlayerStats;
import io.github.mcoriax.mcidentity.api.model.Profession;
import io.github.mcoriax.mcidentity.api.model.ProfessionProgress;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The storage contract every MCIdentity backend implements.
 *
 * <p>Everything a player owns is keyed by the player's own UUID: the profession
 * they wear, their stats and unspent stat points, and, together with a
 * profession code, the level and experience of each profession they have worn.
 * Professions live in a shared registry keyed by system code.</p>
 *
 * <p>Reads that fail resolve to a neutral value (empty, zero or an empty
 * collection) and writes complete normally after logging, except
 * {@link #initializeSchema()} and {@link #createPlayer}, which complete
 * exceptionally so the caller can react.</p>
 */
public interface IMCIdentityDB {

    /**
     * Creates the schema when it is missing and migrates a 4.x schema, where a
     * player's primary identity already used their player UUID, into the
     * player keyed tables.
     * @return A future that completes once the schema is ready.
     */
    CompletableFuture<Void> initializeSchema();

    /**
     * Registers a profession when it is not stored yet.
     * @param systemCode The unique system code.
     * @param displayName The display name.
     * @return A future that completes once the profession is stored.
     */
    CompletableFuture<Void> createProfession(String systemCode, String displayName);

    /**
     * Retrieves every registered profession.
     * @return A future with the professions.
     */
    CompletableFuture<Collection<Profession>> getProfessions();

    /**
     * Creates a player's stats row when it does not exist yet; an existing row
     * is left untouched.
     * @param playerUuid The player's UUID.
     * @param stats The starting stats and stat points.
     * @return A future that completes once the row exists.
     */
    CompletableFuture<Void> createPlayer(UUID playerUuid, PlayerStats stats);

    /**
     * Retrieves how far a player has levelled one profession.
     * @param playerUuid The player's UUID.
     * @param professionCode The profession's system code.
     * @return A future with the progress, or empty when the player never wore it.
     */
    CompletableFuture<Optional<ProfessionProgress>> getProgress(UUID playerUuid, String professionCode);

    /**
     * Stores how far a player has levelled one profession, inserting the row
     * the first time.
     * @param playerUuid The player's UUID.
     * @param professionCode The profession's system code.
     * @param progress The new progress.
     * @return A future that completes once the write finishes.
     */
    CompletableFuture<Void> updateProgress(UUID playerUuid, String professionCode, ProfessionProgress progress);

    /**
     * Retrieves a player's stats and unspent stat points.
     * @param playerUuid The player's UUID.
     * @return A future with the stats, or empty when the player has no row yet.
     */
    CompletableFuture<Optional<PlayerStats>> getStats(UUID playerUuid);

    /**
     * Reads one stat of a player.
     * @param playerUuid The player's UUID.
     * @param statKey A validated stat name.
     * @return A future with the value, or zero when unknown.
     */
    CompletableFuture<Integer> getStat(UUID playerUuid, String statKey);

    /**
     * Reads the profession a player wears.
     * @param playerUuid The player's UUID.
     * @return A future with the profession code, or empty when none is stored.
     */
    CompletableFuture<Optional<String>> getActiveProfession(UUID playerUuid);

    /**
     * Stores the profession a player wears, replacing the previous one.
     * @param playerUuid The player's UUID.
     * @param professionCode The profession's system code.
     * @return A future that completes once the write finishes.
     */
    CompletableFuture<Void> setActiveProfession(UUID playerUuid, String professionCode);

    /**
     * Appends an entry to the profession audit log.
     * @param playerUuid The player's UUID.
     * @param professionCode The profession put on or taken off.
     * @param actionType What happened.
     * @return A future that completes once the entry is written.
     */
    CompletableFuture<Void> logAction(UUID playerUuid, String professionCode, ActionType actionType);

    /**
     * Overwrites a player's stats and unspent stat points.
     * @param playerUuid The player's UUID.
     * @param stats The new stats.
     * @return A future that completes once the write finishes.
     */
    CompletableFuture<Void> updateStats(UUID playerUuid, PlayerStats stats);

    /**
     * Adds to one stat of a player.
     * @param playerUuid The player's UUID.
     * @param statKey A validated stat name.
     * @param amount The amount to add.
     * @return A future that completes once the write finishes.
     */
    CompletableFuture<Void> addStat(UUID playerUuid, String statKey, int amount);

    /**
     * Releases the backend's threads and connections.
     */
    void shutdown();
}

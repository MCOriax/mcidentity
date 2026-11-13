package io.github.mcoriax.mcidentity.api;

import io.github.mcoriax.mcidentity.api.model.ActionType;
import io.github.mcoriax.mcidentity.api.model.PlayerStats;
import io.github.mcoriax.mcidentity.api.model.Profession;
import io.github.mcoriax.mcidentity.api.model.ProfessionProgress;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Main API of MCIdentity, the profession manager.
 *
 * <p>Every player has exactly one character, keyed by their own player UUID, and
 * wears one profession at a time. The base profession {@code player} is what a
 * player wears when they wear nothing else. All data is read and written by the
 * player UUID; there is no other key.</p>
 */
public interface IdentityProvider {

    /**
     * Reads the profession an online player wears, from RAM.
     * <p>
     * Safe on any thread and never touches the database. Empty while the player
     * is offline or their character is still loading.
     * </p>
     * @param playerUuid The player's UUID.
     * @return The worn profession's system code ({@code player} when none), or empty.
     */
    Optional<String> getActiveProfession(UUID playerUuid);

    /**
     * Reads the profession a player wears from storage, online or not.
     * @param playerUuid The player's UUID.
     * @return A future with the stored profession code, or empty when the player has none stored.
     */
    CompletableFuture<Optional<String>> loadActiveProfession(UUID playerUuid);

    /**
     * Stores the profession a player wears and refreshes the RAM session.
     * <p>
     * This is the storage operation only. Gameplay code wears a profession
     * through the plugin, which also checks permissions and the swap cooldown
     * and fires the equip events.
     * </p>
     * @param playerUuid The player's UUID.
     * @param professionCode The system code of a registered profession.
     * @return A future that completes once the change is stored.
     */
    CompletableFuture<Void> setActiveProfession(UUID playerUuid, String professionCode);

    /**
     * Puts the player back in the base {@code player} profession.
     * @param playerUuid The player's UUID.
     * @return A future that completes once the change is stored.
     */
    CompletableFuture<Void> clearActiveProfession(UUID playerUuid);

    /**
     * Retrieves how far a player has levelled one profession.
     * @param playerUuid The player's UUID.
     * @param professionCode The profession's system code.
     * @return A future with the progress, or empty when the player never wore it.
     */
    CompletableFuture<Optional<ProfessionProgress>> getProgress(UUID playerUuid, String professionCode);

    /**
     * Retrieves the player's stats.
     * @param playerUuid The player's UUID.
     * @return A future with the stats, or empty when the player has no character yet.
     */
    CompletableFuture<Optional<PlayerStats>> getStats(UUID playerUuid);

    /**
     * Overwrites how far a player has levelled one profession, and refreshes the
     * RAM session when that profession is the one worn.
     * @param playerUuid The player's UUID.
     * @param professionCode The profession's system code.
     * @param progress The new progress.
     * @return A future that completes once the write finishes.
     */
    CompletableFuture<Void> updateProgress(UUID playerUuid, String professionCode, ProfessionProgress progress);

    /**
     * Overwrites the player's stats and unspent stat points.
     * @param playerUuid The player's UUID.
     * @param stats The new stats.
     * @return A future that completes once the write finishes.
     */
    CompletableFuture<Void> updateStats(UUID playerUuid, PlayerStats stats);

    /**
     * Adds to one stat of the player.
     * @param playerUuid The player's UUID.
     * @param statKey The stat name, such as {@code strength}.
     * @param amount The amount to add.
     * @return A future that completes once the write finishes.
     */
    CompletableFuture<Void> addStat(UUID playerUuid, String statKey, int amount);

    /**
     * Reads one stat of the player.
     * @param playerUuid The player's UUID.
     * @param statKey The stat name, such as {@code strength}.
     * @return A future with the value, or zero when unknown.
     */
    CompletableFuture<Integer> getStat(UUID playerUuid, String statKey);

    /**
     * Appends an entry to the profession audit log.
     * @param playerUuid The player's UUID.
     * @param professionCode The profession put on or taken off.
     * @param actionType What happened.
     * @return A future that completes once the entry is written.
     */
    CompletableFuture<Void> logAction(UUID playerUuid, String professionCode, ActionType actionType);

    /**
     * Registers a profession in storage when it is not there yet.
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
     * Checks whether a player must wait before changing profession again.
     * @param playerUuid The player's UUID.
     * @return true while the swap cooldown runs.
     */
    boolean isOnSwapCooldown(UUID playerUuid);

    /**
     * Starts the swap cooldown for a player.
     * @param playerUuid The player's UUID.
     */
    void setSwapCooldown(UUID playerUuid);

    /**
     * Clears the swap cooldown of a player.
     * @param playerUuid The player's UUID.
     */
    void clearSwapCooldown(UUID playerUuid);

    /**
     * Gets the time left on a player's swap cooldown.
     * @param playerUuid The player's UUID.
     * @return The remaining milliseconds, zero when none.
     */
    long getRemainingSwapCooldownMillis(UUID playerUuid);
}

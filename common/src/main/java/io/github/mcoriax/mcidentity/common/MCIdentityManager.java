package io.github.mcoriax.mcidentity.common;

import io.github.mcoriax.mcidentity.api.db.IMCIdentityDB;
import io.github.mcoriax.mcidentity.api.model.ActiveIdentitySession;
import io.github.mcoriax.mcidentity.api.model.IdentityProperties;
import io.github.mcoriax.mcidentity.api.model.IdentityStats;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The single entry point for everything MCIdentity keeps in RAM.
 *
 * <p>Together with {@link MCIdentityProvider} — which owns persistence — this
 * class is one of the two public facades of the plugin. Gameplay code, commands,
 * listeners, and third-party plugins read and write in-memory state exclusively
 * through the static methods here; nothing outside the {@code common} package
 * touches the underlying collections.</p>
 *
 * <p>Four kinds of state live behind this facade, each in its own package-private
 * holder so this class stays a facade rather than a container:</p>
 * <ul>
 *   <li><strong>Active sessions</strong> — {@link SessionCache}: which identity
 *   each online player is currently playing as, with their live stats.</li>
 *   <li><strong>Profession configuration</strong> — {@link ProfessionRegistry}:
 *   everything the profession files declare.</li>
 *   <li><strong>Per-player gameplay state</strong> — {@link PlayerStateStore}:
 *   swap cooldowns and the in-flight equip guard.</li>
 *   <li><strong>Platform handles</strong> — the three {@link Object} fields
 *   below, which let {@code common} hold Bukkit objects it must not import.</li>
 * </ul>
 *
 * <p>Every method is static and safe to call from any thread: the state holders
 * use concurrent collections throughout, because gameplay reads happen on the
 * main thread (and on region threads under Folia) while writes arrive from
 * asynchronous database callbacks.</p>
 */
public class MCIdentityManager {

    /**
     * The Bukkit {@code FileConfiguration} holding {@code textures.yml}.
     *
     * <p>Typed as {@link Object} because {@code common} is pure Java and must
     * never import a platform type; the platform module casts it back on the
     * way out. Set once during plugin enable.</p>
     */
    private static volatile Object texturesConfig;

    /**
     * The platform's experience manager instance.
     *
     * <p>Typed as {@link Object} for the same reason as {@link #texturesConfig}.</p>
     */
    private static volatile Object experienceManager;

    /**
     * The platform's registered stats listener instance.
     *
     * <p>Typed as {@link Object} for the same reason as {@link #texturesConfig}.</p>
     */
    private static volatile Object statsListener;

    /**
     * The level ceiling and per-level EXP requirement of one profession.
     * @param maxLevel The highest level an identity of this profession reaches.
     * @param expUp The experience needed to advance one level.
     */
    public record ProfessionLevelConfig(int maxLevel, int expUp) {}

    // --- Database Initialization ---

    /**
     * Initializes the database backend matching the configured
     * {@code database.type} value.
     *
     * @deprecated Handing out a raw {@link IMCIdentityDB} lets callers bypass
     *     the {@link MCIdentityProvider} facade, which is what every other
     *     operation goes through. Use
     *     {@link MCIdentityProvider#create(String, Map)} instead: it builds the
     *     same backend, installs it as the provider singleton, and keeps the
     *     handle private. This method now delegates there and will be removed in
     *     a future major release.
     *
     * @param type The configured database type.
     * @param options The connection settings for that type.
     * @return The database backend, not yet schema-initialized.
     * @throws IllegalArgumentException When the type is unknown.
     */
    @Deprecated
    public static IMCIdentityDB createDatabase(String type, Map<String, Object> options) {
        return DatabaseFactory.create(type, options);
    }

    // --- Per-player gameplay state ---

    /**
     * Sets how long players must wait between identity swaps.
     * @param minutes The cooldown in minutes; zero or less disables it.
     */
    public static void setIdentitySwapCooldownMinutes(int minutes) {
        PlayerStateStore.setSwapCooldownMinutes(minutes);
    }

    /**
     * Checks whether a player is still within their identity swap cooldown.
     * @param playerId The UUID of the player.
     * @return True while the player must still wait.
     */
    public static boolean isOnSwapCooldown(UUID playerId) {
        return PlayerStateStore.isOnSwapCooldown(playerId);
    }

    /**
     * Starts a player's identity swap cooldown from now.
     * @param playerId The UUID of the player.
     */
    public static void setSwapCooldown(UUID playerId) {
        PlayerStateStore.setSwapCooldown(playerId);
    }

    /**
     * Ends a player's identity swap cooldown immediately.
     * @param playerId The UUID of the player.
     */
    public static void clearSwapCooldown(UUID playerId) {
        PlayerStateStore.clearSwapCooldown(playerId);
    }

    /**
     * Checks whether a player already has an equip operation in flight.
     * @param playerId The UUID of the player.
     * @return True when one is pending.
     */
    public static boolean isEquipPending(UUID playerId) {
        return PlayerStateStore.isEquipPending(playerId);
    }

    /**
     * Marks whether a player has an equip operation in flight.
     * @param playerId The UUID of the player.
     * @param pending True to mark one as pending, false to clear it.
     */
    public static void setEquipPending(UUID playerId, boolean pending) {
        PlayerStateStore.setEquipPending(playerId, pending);
    }

    /**
     * Gets how long a player must still wait before swapping identity again.
     * @param playerId The UUID of the player.
     * @return The remaining milliseconds, never negative.
     */
    public static long getRemainingSwapCooldownMillis(UUID playerId) {
        return PlayerStateStore.getRemainingSwapCooldownMillis(playerId);
    }

    /**
     * Renders a duration as a short "1m 30s" style string, for cooldown messages.
     * @param millis The duration in milliseconds.
     * @return The formatted duration, "0s" for anything non-positive.
     */
    public static String formatTime(long millis) {
        return PlayerStateStore.formatTime(millis);
    }

    // --- Platform handles ---

    /**
     * Gets the {@code textures.yml} configuration handle.
     * @return The configuration, or null before the plugin has enabled. The
     *         platform module casts it back to its Bukkit type.
     */
    public static Object getTexturesConfig() {
        return texturesConfig;
    }

    /**
     * Stores the {@code textures.yml} configuration handle.
     * @param config The platform's configuration object.
     */
    public static void setTexturesConfig(Object config) {
        texturesConfig = config;
    }

    /**
     * Gets the experience manager handle.
     * @return The manager, or null before the plugin has enabled. The platform
     *         module casts it back to its Bukkit type.
     */
    public static Object getExperienceManager() {
        return experienceManager;
    }

    /**
     * Stores the experience manager handle.
     * @param manager The platform's experience manager.
     */
    public static void setExperienceManager(Object manager) {
        experienceManager = manager;
    }

    /**
     * Gets the stats listener handle.
     * @return The listener, or null before the plugin has enabled. The platform
     *         module casts it back to its Bukkit type.
     */
    public static Object getStatsListener() {
        return statsListener;
    }

    /**
     * Stores the stats listener handle.
     * @param listener The platform's stats listener.
     */
    public static void setStatsListener(Object listener) {
        statsListener = listener;
    }

    // --- Profession configuration ---

    /**
     * Stores the level configuration for a profession.
     * @param professionCode The system code of the profession.
     * @param maxLevel The maximum level.
     * @param expUp The EXP required for next level.
     */
    public static void setProfessionLevelConfig(String professionCode, int maxLevel, int expUp) {
        ProfessionRegistry.setLevelConfig(professionCode, maxLevel, expUp);
    }

    /**
     * Gets the level configuration for a profession.
     * @param professionCode The system code of the profession.
     * @return The level configuration.
     */
    public static ProfessionLevelConfig getProfessionLevelConfig(String professionCode) {
        return ProfessionRegistry.getLevelConfig(professionCode);
    }

    /**
     * Stores the EXP configuration for a profession.
     * @param professionCode The system code of the profession.
     * @param config The EXP configuration map.
     */
    public static void setProfessionExpConfig(String professionCode, Map<String, Object> config) {
        ProfessionRegistry.setExpConfig(professionCode, config);
    }

    /**
     * Gets the EXP configuration for a profession.
     * @param professionCode The system code of the profession.
     * @return The EXP configuration map.
     */
    public static Map<String, Object> getProfessionExpConfig(String professionCode) {
        return ProfessionRegistry.getExpConfig(professionCode);
    }

    /**
     * Stores the display name for a profession.
     * @param professionCode The system code of the profession.
     * @param displayName The display name.
     */
    public static void setProfessionDisplayName(String professionCode, String displayName) {
        ProfessionRegistry.setDisplayName(professionCode, displayName);
    }

    /**
     * Gets the display name for a profession.
     * @param professionCode The system code of the profession.
     * @return The display name.
     */
    public static String getProfessionDisplayName(String professionCode) {
        return ProfessionRegistry.getDisplayName(professionCode);
    }

    /**
     * Gets all registered profession system codes and their display names.
     * @return A map of system code to display name.
     */
    public static Map<String, String> getRegisteredProfessions() {
        return ProfessionRegistry.getRegistered();
    }

    /**
     * Stores the base stats for a profession.
     * @param professionCode The system code of the profession.
     * @param stats The base stats map.
     */
    public static void setProfessionBaseStats(String professionCode, Map<String, Integer> stats) {
        ProfessionRegistry.setBaseStats(professionCode, stats);
    }

    /**
     * Gets the base stats for a profession.
     * @param professionCode The system code of the profession.
     * @return The base stats map.
     */
    public static Map<String, Integer> getProfessionBaseStats(String professionCode) {
        return ProfessionRegistry.getBaseStats(professionCode);
    }

    /**
     * Stores the stats randomization for a profession.
     * @param professionCode The system code of the profession.
     * @param random The randomization config string.
     */
    public static void setProfessionStatsRandom(String professionCode, String random) {
        ProfessionRegistry.setStatsRandom(professionCode, random);
    }

    /**
     * Gets the stats randomization for a profession.
     * @param professionCode The system code of the profession.
     * @return The randomization config string.
     */
    public static String getProfessionStatsRandom(String professionCode) {
        return ProfessionRegistry.getStatsRandom(professionCode);
    }

    /**
     * Stores the skins for a profession.
     * @param professionCode The system code of the profession.
     * @param skins The skins map.
     */
    public static void setProfessionSkins(String professionCode, Map<Integer, String> skins) {
        ProfessionRegistry.setSkins(professionCode, skins);
    }

    /**
     * Gets the skins for a profession.
     * @param professionCode The system code of the profession.
     * @return The skins map.
     */
    public static Map<Integer, String> getProfessionSkins(String professionCode) {
        return ProfessionRegistry.getSkins(professionCode);
    }

    // --- Active sessions ---

    /**
     * Adds or updates a session in RAM.
     * @param session The session to store.
     */
    public static void cacheSession(ActiveIdentitySession session) {
        SessionCache.put(session);
    }

    /**
     * Removes a session from RAM.
     * @param playerUuid The UUID of the player.
     */
    public static void removeSession(UUID playerUuid) {
        SessionCache.remove(playerUuid);
    }

    /**
     * Gets a cached session.
     * @param playerUuid The UUID of the player.
     * @return An optional containing the session if found.
     */
    public static Optional<ActiveIdentitySession> getSession(UUID playerUuid) {
        return SessionCache.get(playerUuid);
    }

    /**
     * Updates the stats of a cached session atomically.
     * @param playerUuid The UUID of the player.
     * @param newStats The new stats.
     */
    public static void updateStats(UUID playerUuid, IdentityStats newStats) {
        SessionCache.update(playerUuid, session -> session.withStats(newStats));
    }

    /**
     * Updates the properties of a cached session atomically.
     * @param playerUuid The UUID of the player.
     * @param newProperties The new properties.
     */
    public static void updateProperties(UUID playerUuid, IdentityProperties newProperties) {
        SessionCache.update(playerUuid, session -> session.withProperties(newProperties));
    }

    /**
     * Updates the current health of a cached session.
     * @param playerUuid The UUID of the player.
     * @param health The new health.
     */
    public static void updateHealth(UUID playerUuid, double health) {
        SessionCache.update(playerUuid, session -> session.withCurrentHealth(health));
    }

    /**
     * Updates the current energy of a cached session.
     * @param playerUuid The UUID of the player.
     * @param energy The new energy.
     */
    public static void updateEnergy(UUID playerUuid, double energy) {
        SessionCache.update(playerUuid, session -> session.withCurrentEnergy(energy));
    }

    /**
     * Updates the current food of a cached session.
     * @param playerUuid The UUID of the player.
     * @param food The new food.
     */
    public static void updateFood(UUID playerUuid, double food) {
        SessionCache.update(playerUuid, session -> session.withCurrentFood(food));
    }

    /**
     * Gets a cached session by identity UUID.
     * @param identityUuid The UUID of the identity.
     * @return An optional containing the session if found.
     */
    public static Optional<ActiveIdentitySession> getSessionByIdentity(UUID identityUuid) {
        return SessionCache.getByIdentity(identityUuid);
    }

    /**
     * Caches how many alternative base identity slots a player has unlocked, so
     * base identities can be recognised synchronously without a database round
     * trip. The primary base identity is always available and not counted here.
     * @param playerUuid The UUID of the player.
     * @param slots The unlocked alternative slot count.
     */
    public static void cacheBaseIdentitySlots(UUID playerUuid, int slots) {
        SessionCache.putBaseIdentitySlots(playerUuid, slots);
    }

    /**
     * Gets the cached number of unlocked alternative base identity slots for a
     * player, defaulting to 0 when it has not been cached yet.
     * @param playerUuid The UUID of the player.
     * @return The cached unlocked alternative slot count.
     */
    public static int getCachedBaseIdentitySlots(UUID playerUuid) {
        return SessionCache.getBaseIdentitySlots(playerUuid);
    }

    /**
     * Drops a player's cached alternative base identity slot count.
     * @param playerUuid The UUID of the player.
     */
    public static void clearBaseIdentitySlots(UUID playerUuid) {
        SessionCache.removeBaseIdentitySlots(playerUuid);
    }

    /**
     * Checks whether the player's currently equipped identity is one of their
     * permanently bound base identities — the primary identity or any unlocked
     * alternative — using the cached slot count. Base identities are detected by
     * their deterministic UUID, not by profession, so alternatives of any
     * profession are still recognised.
     * @param playerUuid The UUID of the player.
     * @return True when the equipped identity is a base identity.
     */
    public static boolean isWearingBaseIdentity(UUID playerUuid) {
        return getSession(playerUuid)
            .map(session -> BaseProfession.identityIndex(playerUuid, session.identityUuid(),
                getCachedBaseIdentitySlots(playerUuid)) >= 0)
            .orElse(false);
    }

    /**
     * Clears all cached sessions and base identity slot counts.
     */
    public static void clear() {
        SessionCache.clear();
    }
}

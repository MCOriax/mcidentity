package io.gitlab.mcoriax.common;

import io.gitlab.mcoriax.api.model.ActiveIdentitySession;
import io.gitlab.mcoriax.api.model.IdentityKey;
import io.gitlab.mcoriax.api.model.IdentityProperties;
import io.gitlab.mcoriax.api.model.IdentityStats;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active identity sessions in RAM.
 */
public class MCIdentityManager {

    private static final Map<UUID, ActiveIdentitySession> ACTIVE_SESSIONS = new ConcurrentHashMap<>();
    private static final Map<String, String> PROFESSION_DISPLAY_NAMES = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> PROFESSION_EXP_CONFIGS = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Integer>> PROFESSION_BASE_STATS = new ConcurrentHashMap<>();
    private static final Map<String, String> PROFESSION_STATS_RANDOM = new ConcurrentHashMap<>();
    private static final Map<String, ProfessionLevelConfig> PROFESSION_LEVEL_CONFIGS = new ConcurrentHashMap<>();

    public record ProfessionLevelConfig(int maxLevel, int expUp) {}

    /**
     * Stores the level configuration for a profession.
     * @param professionCode The system code of the profession.
     * @param maxLevel The maximum level.
     * @param expUp The EXP required for next level.
     */
    public static void setProfessionLevelConfig(String professionCode, int maxLevel, int expUp) {
        PROFESSION_LEVEL_CONFIGS.put(professionCode.toLowerCase(), new ProfessionLevelConfig(maxLevel, expUp));
    }

    /**
     * Gets the level configuration for a profession.
     * @param professionCode The system code of the profession.
     * @return The level configuration.
     */
    public static ProfessionLevelConfig getProfessionLevelConfig(String professionCode) {
        return PROFESSION_LEVEL_CONFIGS.getOrDefault(professionCode.toLowerCase(), new ProfessionLevelConfig(100, 10000));
    }

    /**
     * Stores the EXP configuration for a profession.
     * @param professionCode The system code of the profession.
     * @param config The EXP configuration map.
     */
    public static void setProfessionExpConfig(String professionCode, Map<String, Object> config) {
        PROFESSION_EXP_CONFIGS.put(professionCode.toLowerCase(), config);
    }

    /**
     * Gets the EXP configuration for a profession.
     * @param professionCode The system code of the profession.
     * @return The EXP configuration map.
     */
    public static Map<String, Object> getProfessionExpConfig(String professionCode) {
        return PROFESSION_EXP_CONFIGS.getOrDefault(professionCode.toLowerCase(), java.util.Collections.emptyMap());
    }

    /**
     * Stores the display name for a profession.
     * @param professionCode The system code of the profession.
     * @param displayName The display name.
     */
    public static void setProfessionDisplayName(String professionCode, String displayName) {
        PROFESSION_DISPLAY_NAMES.put(professionCode.toLowerCase(), displayName);
    }

    /**
     * Gets the display name for a profession.
     * @param professionCode The system code of the profession.
     * @return The display name.
     */
    public static String getProfessionDisplayName(String professionCode) {
        return PROFESSION_DISPLAY_NAMES.getOrDefault(professionCode.toLowerCase(), professionCode);
    }

    /**
     * Gets all registered profession system codes and their display names.
     * @return A map of system code to display name.
     */
    public static Map<String, String> getRegisteredProfessions() {
        return java.util.Collections.unmodifiableMap(PROFESSION_DISPLAY_NAMES);
    }

    /**
     * Stores the base stats for a profession.
     * @param professionCode The system code of the profession.
     * @param stats The base stats map.
     */
    public static void setProfessionBaseStats(String professionCode, Map<String, Integer> stats) {
        PROFESSION_BASE_STATS.put(professionCode.toLowerCase(), stats);
    }

    /**
     * Gets the base stats for a profession.
     * @param professionCode The system code of the profession.
     * @return The base stats map.
     */
    public static Map<String, Integer> getProfessionBaseStats(String professionCode) {
        return PROFESSION_BASE_STATS.getOrDefault(professionCode.toLowerCase(), java.util.Collections.emptyMap());
    }

    /**
     * Stores the stats randomization for a profession.
     * @param professionCode The system code of the profession.
     * @param random The randomization config string.
     */
    public static void setProfessionStatsRandom(String professionCode, String random) {
        PROFESSION_STATS_RANDOM.put(professionCode.toLowerCase(), random);
    }

    /**
     * Gets the stats randomization for a profession.
     * @param professionCode The system code of the profession.
     * @return The randomization config string.
     */
    public static String getProfessionStatsRandom(String professionCode) {
        return PROFESSION_STATS_RANDOM.getOrDefault(professionCode.toLowerCase(), "0");
    }

    /**
     * Adds or updates a session in RAM.
     * @param session The session to store.
     */
    public static void cacheSession(ActiveIdentitySession session) {
        ACTIVE_SESSIONS.put(session.playerUuid(), session);
    }

    /**
     * Removes a session from RAM.
     * @param playerUuid The UUID of the player.
     */
    public static void removeSession(UUID playerUuid) {
        ACTIVE_SESSIONS.remove(playerUuid);
    }

    /**
     * Gets a cached session.
     * @param playerUuid The UUID of the player.
     * @return An optional containing the session if found.
     */
    public static Optional<ActiveIdentitySession> getSession(UUID playerUuid) {
        return Optional.ofNullable(ACTIVE_SESSIONS.get(playerUuid));
    }

    /**
     * Updates the stats of a cached session atomically.
     * @param playerUuid The UUID of the player.
     * @param newStats The new stats.
     */
    public static void updateStats(UUID playerUuid, IdentityStats newStats) {
        ACTIVE_SESSIONS.computeIfPresent(playerUuid, (uuid, oldSession) -> 
            new ActiveIdentitySession(
                oldSession.playerUuid(),
                oldSession.identityUuid(),
                oldSession.identityKey(),
                old_session.properties(),
                newStats,
                old_session.currentHealth(),
                old_session.currentEnergy(),
                old_session.currentFood()
            )
        );
    }

    /**
     * Updates the properties of a cached session atomically.
     * @param playerUuid The UUID of the player.
     * @param newProperties The new properties.
     */
    public static void updateProperties(UUID playerUuid, IdentityProperties newProperties) {
        ACTIVE_SESSIONS.computeIfPresent(playerUuid, (uuid, oldSession) -> 
            new ActiveIdentitySession(
                oldSession.playerUuid(),
                oldSession.identityUuid(),
                oldSession.identityKey(),
                newProperties,
                old_session.stats(),
                old_session.currentHealth(),
                old_session.currentEnergy(),
                old_session.currentFood()
            )
        );
    }

    /**
     * Updates the current health of a cached session.
     * @param playerUuid The UUID of the player.
     * @param health The new health.
     */
    public static void updateHealth(UUID playerUuid, double health) {
        ACTIVE_SESSIONS.computeIfPresent(playerUuid, (uuid, oldSession) ->
            new ActiveIdentitySession(
                oldSession.playerUuid(),
                oldSession.identityUuid(),
                oldSession.identityKey(),
                old_session.properties(),
                old_session.stats(),
                health,
                old_session.currentEnergy(),
                old_session.currentFood()
            )
        );
    }

    /**
     * Updates the current energy of a cached session.
     * @param playerUuid The UUID of the player.
     * @param energy The new energy.
     */
    public static void updateEnergy(UUID playerUuid, double energy) {
        ACTIVE_SESSIONS.computeIfPresent(playerUuid, (uuid, oldSession) ->
            new ActiveIdentitySession(
                oldSession.playerUuid(),
                oldSession.identityUuid(),
                oldSession.identityKey(),
                old_session.properties(),
                old_session.stats(),
                old_session.currentHealth(),
                energy,
                old_session.currentFood()
            )
        );
    }

    /**
     * Updates the current food of a cached session.
     * @param playerUuid The UUID of the player.
     * @param food The new food.
     */
    public static void updateFood(UUID playerUuid, double food) {
        ACTIVE_SESSIONS.computeIfPresent(playerUuid, (uuid, old_session) ->
            new ActiveIdentitySession(
                old_session.playerUuid(),
                old_session.identityUuid(),
                old_session.identityKey(),
                old_session.properties(),
                old_session.stats(),
                old_session.currentHealth(),
                old_session.currentEnergy(),
                food
            )
        );
    }

    /**
     * Gets a cached session by identity UUID.
     * @param identityUuid The UUID of the identity.
     * @return An optional containing the session if found.
     */
    public static Optional<ActiveIdentitySession> getSessionByIdentity(UUID identityUuid) {
        return ACTIVE_SESSIONS.values().stream()
            .filter(session -> session.identityUuid().equals(identityUuid))
            .findFirst();
    }

    /**
     * Clears all cached sessions.
     */
    public static void clear() {
        ACTIVE_SESSIONS.clear();
    }
}

package io.gitlab.mcoriax.common;

import io.gitlab.mcoriax.api.model.ActiveIdentitySession;
import io.gitlab.mcoriax.api.model.IdentityKey;
import io.gitlab.mcoriax.api.model.IdentityProperties;
import io.gitlab.mcoriax.api.model.IdentityStats;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
    private static final Map<String, Map<Integer, String>> PROFESSION_SKINS = new ConcurrentHashMap<>();

    // Relocated states from platform module
    private static Object texturesConfig;
    private static Object experienceManager;
    private static Object statsListener;
    private static final Map<UUID, Long> SWAP_COOLDOWNS = new ConcurrentHashMap<>();
    private static final Set<UUID> PENDING_EQUIPS = ConcurrentHashMap.newKeySet();
    private static int identitySwapCooldownMinutes = 0;

    public record ProfessionLevelConfig(int maxLevel, int expUp) {}

    // --- Relocated Methods ---

    public static void setIdentitySwapCooldownMinutes(int minutes) {
        identitySwapCooldownMinutes = minutes;
    }

    public static boolean isOnSwapCooldown(UUID playerId) {
        if (identitySwapCooldownMinutes <= 0) return false;

        Long lastSwap = SWAP_COOLDOWNS.get(playerId);
        if (lastSwap == null) return false;

        long elapsedMillis = System.currentTimeMillis() - lastSwap;
        long cooldownMillis = TimeUnit.MINUTES.toMillis(identitySwapCooldownMinutes);

        if (elapsedMillis >= cooldownMillis) {
            SWAP_COOLDOWNS.remove(playerId);
            return false;
        }
        return true;
    }

    public static void setSwapCooldown(UUID playerId) {
        SWAP_COOLDOWNS.put(playerId, System.currentTimeMillis());
    }

    public static void clearSwapCooldown(UUID playerId) {
        SWAP_COOLDOWNS.remove(playerId);
    }

    public static boolean isEquipPending(UUID playerId) {
        return PENDING_EQUIPS.contains(playerId);
    }

    public static void setEquipPending(UUID playerId, boolean pending) {
        if (pending) {
            PENDING_EQUIPS.add(playerId);
        } else {
            PENDING_EQUIPS.remove(playerId);
        }
    }

    public static long getRemainingSwapCooldownMillis(UUID playerId) {
        Long lastSwap = SWAP_COOLDOWNS.get(playerId);
        if (lastSwap == null) return 0;

        long elapsedMillis = System.currentTimeMillis() - lastSwap;
        long cooldownMillis = TimeUnit.MINUTES.toMillis(identitySwapCooldownMinutes);
        long remainingMillis = cooldownMillis - elapsedMillis;

        return Math.max(0, remainingMillis);
    }

    public static String formatTime(long millis) {
        if (millis <= 0) return "0s";
        
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (seconds > 0 || minutes == 0) {
            sb.append(seconds).append("s");
        }
        return sb.toString().trim();
    }

    // --- Getters and Setters for Managers/Listeners ---

    public static Object getTexturesConfig() {
        return texturesConfig;
    }

    public static void setTexturesConfig(Object config) {
        texturesConfig = config;
    }

    public static Object getExperienceManager() {
        return experienceManager;
    }

    public static void setExperienceManager(Object manager) {
        experienceManager = manager;
    }

    public static Object getStatsListener() {
        return statsListener;
    }

    public static void setStatsListener(Object listener) {
        statsListener = listener;
    }

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
     * Stores the skins for a profession.
     * @param professionCode The system code of the profession.
     * @param skins The skins map.
     */
    public static void setProfessionSkins(String professionCode, Map<Integer, String> skins) {
        PROFESSION_SKINS.put(professionCode.toLowerCase(), skins);
    }

    /**
     * Gets the skins for a profession.
     * @param professionCode The system code of the profession.
     * @return The skins map.
     */
    public static Map<Integer, String> getProfessionSkins(String professionCode) {
        return PROFESSION_SKINS.getOrDefault(professionCode.toLowerCase(), java.util.Collections.emptyMap());
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
                oldSession.properties(),
                newStats,
                oldSession.contentKey(),
                oldSession.currentHealth(),
                oldSession.currentEnergy(),
                oldSession.currentFood()
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
                oldSession.stats(),
                oldSession.contentKey(),
                oldSession.currentHealth(),
                oldSession.currentEnergy(),
                oldSession.currentFood()
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
                oldSession.properties(),
                oldSession.stats(),
                oldSession.contentKey(),
                health,
                oldSession.currentEnergy(),
                oldSession.currentFood()
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
                oldSession.properties(),
                oldSession.stats(),
                oldSession.contentKey(),
                oldSession.currentHealth(),
                energy,
                oldSession.currentFood()
            )
        );
    }

    /**
     * Updates the current food of a cached session.
     * @param playerUuid The UUID of the player.
     * @param food The new food.
     */
    public static void updateFood(UUID playerUuid, double food) {
        ACTIVE_SESSIONS.computeIfPresent(playerUuid, (uuid, oldSession) ->
            new ActiveIdentitySession(
                oldSession.playerUuid(),
                oldSession.identityUuid(),
                oldSession.identityKey(),
                oldSession.properties(),
                oldSession.stats(),
                oldSession.contentKey(),
                oldSession.currentHealth(),
                oldSession.currentEnergy(),
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

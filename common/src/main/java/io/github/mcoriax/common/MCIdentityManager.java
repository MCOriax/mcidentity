package io.github.mcoriax.common;

import io.github.mcoriax.api.db.IMCIdentityDB;
import io.github.mcoriax.api.model.ActiveIdentitySession;
import io.github.mcoriax.api.model.EquippedAbility;
import io.github.mcoriax.api.model.IdentityKey;
import io.github.mcoriax.api.model.IdentityProperties;
import io.github.mcoriax.api.model.IdentityStats;
import io.github.mcoriax.common.db.MongoDB;
import io.github.mcoriax.common.db.MySQLDB;
import io.github.mcoriax.common.db.PostgreSQLDB;
import io.github.mcoriax.common.db.SQLiteDB;
import io.github.mcoriax.common.central.MCIdentityCentralServer;

import java.util.Collections;
import java.util.Locale;
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
    private static final Map<UUID, Integer> BASE_IDENTITY_SLOTS = new ConcurrentHashMap<>();
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

    // --- Database Initialization ---

    /**
     * Initializes the database backend matching the configured
     * {@code database.type} value.
     *
     * <p>Supported types are {@code sqlite} (the default), {@code mysql},
     * {@code postgresql}, {@code mongodb}, and {@code central} for the
     * MCIdentity central REST server. The options map carries the connection
     * settings of the selected backend's configuration section, e.g.
     * {@code file} for SQLite; {@code host}, {@code port}, {@code database},
     * {@code username}, and {@code password} for MySQL and PostgreSQL;
     * {@code uri} and {@code database} for MongoDB; and {@code url},
     * {@code token}, and {@code timeout_seconds} for the central server.</p>
     *
     * @param type The configured database type.
     * @param options The connection settings for that type.
     * @return The database backend, not yet schema-initialized.
     * @throws IllegalArgumentException When the type is unknown.
     */
    public static IMCIdentityDB createDatabase(String type, Map<String, Object> options) {
        String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "sqlite" -> new SQLiteDB(
                stringOption(options, "file", "mcidentity.db"));
            case "mysql", "mariadb" -> new MySQLDB(
                stringOption(options, "host", "localhost"),
                intOption(options, "port", 3306),
                stringOption(options, "database", "mcidentity"),
                stringOption(options, "username", "root"),
                stringOption(options, "password", ""));
            case "postgresql", "postgres" -> new PostgreSQLDB(
                stringOption(options, "host", "localhost"),
                intOption(options, "port", 5432),
                stringOption(options, "database", "mcidentity"),
                stringOption(options, "username", "postgres"),
                stringOption(options, "password", ""));
            case "mongodb", "mongo" -> new MongoDB(
                stringOption(options, "uri", "mongodb://localhost:27017"),
                stringOption(options, "database", "mcidentity"));
            case "central", "central_server" -> new MCIdentityCentralServer(
                stringOption(options, "url", "https://server.com"),
                stringOption(options, "token", ""),
                intOption(options, "timeout_seconds", 10));
            default -> throw new IllegalArgumentException("Unknown database.type: " + type);
        };
    }

    /**
     * Reads a string connection option.
     * @param options The connection settings.
     * @param key The option key.
     * @param fallback The value used when the option is absent or null.
     * @return The option value.
     */
    private static String stringOption(Map<String, Object> options, String key, String fallback) {
        Object value = options.get(key);
        return value != null ? value.toString() : fallback;
    }

    /**
     * Reads an integer connection option.
     * @param options The connection settings.
     * @param key The option key.
     * @param fallback The value used when the option is absent or not a number.
     * @return The option value.
     */
    private static int intOption(Map<String, Object> options, String key, int fallback) {
        Object value = options.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

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
        return PROFESSION_EXP_CONFIGS.getOrDefault(professionCode.toLowerCase(), Collections.emptyMap());
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
        return Collections.unmodifiableMap(PROFESSION_DISPLAY_NAMES);
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
        return PROFESSION_BASE_STATS.getOrDefault(professionCode.toLowerCase(), Collections.emptyMap());
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
        return PROFESSION_SKINS.getOrDefault(professionCode.toLowerCase(), Collections.emptyMap());
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
        ACTIVE_SESSIONS.computeIfPresent(playerUuid, (uuid, oldSession) -> oldSession.withStats(newStats));
    }

    /**
     * Updates the properties of a cached session atomically.
     * @param playerUuid The UUID of the player.
     * @param newProperties The new properties.
     */
    public static void updateProperties(UUID playerUuid, IdentityProperties newProperties) {
        ACTIVE_SESSIONS.computeIfPresent(playerUuid, (uuid, oldSession) -> oldSession.withProperties(newProperties));
    }

    /**
     * Updates the current health of a cached session.
     * @param playerUuid The UUID of the player.
     * @param health The new health.
     */
    public static void updateHealth(UUID playerUuid, double health) {
        ACTIVE_SESSIONS.computeIfPresent(playerUuid, (uuid, oldSession) -> oldSession.withCurrentHealth(health));
    }

    /**
     * Updates the current energy of a cached session.
     * @param playerUuid The UUID of the player.
     * @param energy The new energy.
     */
    public static void updateEnergy(UUID playerUuid, double energy) {
        ACTIVE_SESSIONS.computeIfPresent(playerUuid, (uuid, oldSession) -> oldSession.withCurrentEnergy(energy));
    }

    /**
     * Updates the current food of a cached session.
     * @param playerUuid The UUID of the player.
     * @param food The new food.
     */
    public static void updateFood(UUID playerUuid, double food) {
        ACTIVE_SESSIONS.computeIfPresent(playerUuid, (uuid, oldSession) -> oldSession.withCurrentFood(food));
    }

    /**
     * Updates the equipped abilities of a cached session.
     * @param playerUuid The UUID of the player.
     * @param equippedAbilities The new equipped abilities.
     */
    public static void updateEquippedAbilities(UUID playerUuid, Map<Integer, EquippedAbility> equippedAbilities) {
        ACTIVE_SESSIONS.computeIfPresent(playerUuid, (uuid, oldSession) -> oldSession.withEquippedAbilities(equippedAbilities));
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
     * Caches how many alternative base identity slots a player has unlocked, so
     * base identities can be recognised synchronously without a database round
     * trip. The primary base identity is always available and not counted here.
     * @param playerUuid The UUID of the player.
     * @param slots The unlocked alternative slot count.
     */
    public static void cacheBaseIdentitySlots(UUID playerUuid, int slots) {
        BASE_IDENTITY_SLOTS.put(playerUuid, Math.max(0, slots));
    }

    /**
     * Gets the cached number of unlocked alternative base identity slots for a
     * player, defaulting to 0 when it has not been cached yet.
     * @param playerUuid The UUID of the player.
     * @return The cached unlocked alternative slot count.
     */
    public static int getCachedBaseIdentitySlots(UUID playerUuid) {
        return BASE_IDENTITY_SLOTS.getOrDefault(playerUuid, 0);
    }

    /**
     * Drops a player's cached alternative base identity slot count.
     * @param playerUuid The UUID of the player.
     */
    public static void clearBaseIdentitySlots(UUID playerUuid) {
        BASE_IDENTITY_SLOTS.remove(playerUuid);
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
        ACTIVE_SESSIONS.clear();
        BASE_IDENTITY_SLOTS.clear();
    }
}

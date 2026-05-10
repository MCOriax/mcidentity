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
    private static final Map<String, Map<String, String>> PROFESSION_EXP_CONFIGS = new ConcurrentHashMap<>();
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
    public static void setProfessionExpConfig(String professionCode, Map<String, String> config) {
        PROFESSION_EXP_CONFIGS.put(professionCode.toLowerCase(), config);
    }

    /**
     * Gets the EXP configuration for a profession.
     * @param professionCode The system code of the profession.
     * @return The EXP configuration map.
     */
    public static Map<String, String> getProfessionExpConfig(String professionCode) {
        return PROFESSION_EXP_CONFIGS.getOrDefault(professionCode.toLowerCase(), java.util.Collections.emptyMap());
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
                oldSession.currentHealth(),
                oldSession.currentEnergy()
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
                oldSession.currentHealth(),
                oldSession.currentEnergy()
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
                health,
                oldSession.currentEnergy()
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
                oldSession.currentHealth(),
                energy
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

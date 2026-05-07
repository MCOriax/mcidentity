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
     * Updates the stats of a cached session by creating a new record instance.
     * @param playerUuid The UUID of the player.
     * @param newStats The new stats.
     */
    public static void updateStats(UUID playerUuid, IdentityStats newStats) {
        ActiveIdentitySession oldSession = ACTIVE_SESSIONS.get(playerUuid);
        if (oldSession != null) {
            ActiveIdentitySession newSession = new ActiveIdentitySession(
                oldSession.playerUuid(),
                oldSession.identityUuid(),
                oldSession.identityKey(),
                oldSession.properties(),
                newStats
            );
            ACTIVE_SESSIONS.put(playerUuid, newSession);
        }
    }

    /**
     * Updates the properties of a cached session by creating a new record instance.
     * @param playerUuid The UUID of the player.
     * @param newProperties The new properties.
     */
    public static void updateProperties(UUID playerUuid, IdentityProperties newProperties) {
        ActiveIdentitySession oldSession = ACTIVE_SESSIONS.get(playerUuid);
        if (oldSession != null) {
            ActiveIdentitySession newSession = new ActiveIdentitySession(
                oldSession.playerUuid(),
                oldSession.identityUuid(),
                oldSession.identityKey(),
                newProperties,
                oldSession.stats()
            );
            ACTIVE_SESSIONS.put(playerUuid, newSession);
        }
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

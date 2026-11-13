package io.github.mcoriax.mcidentity.common;

import io.github.mcoriax.mcidentity.api.model.ActiveProfessionSession;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/**
 * The RAM cache of the profession every online player wears, with its progress
 * and the player's stats.
 *
 * <p><strong>Internal.</strong> Package-private on purpose: gameplay code
 * reaches sessions exclusively through {@link MCIdentityManager}, which is the
 * single public entry point for RAM state. Extracted so the manager stays a
 * facade rather than also owning the maps.</p>
 *
 * <p>Sessions are immutable records, so every mutation replaces the whole entry
 * through {@link ConcurrentHashMap#computeIfPresent}. That keeps a read on the
 * main thread from ever observing a half-applied update written by an
 * asynchronous database callback.</p>
 */
final class SessionCache {

    /**
     * The session each online player currently holds, keyed by player UUID.
     */
    private static final Map<UUID, ActiveProfessionSession> ACTIVE_SESSIONS = new ConcurrentHashMap<>();

    /**
     * Prevents instantiation of this state holder.
     */
    private SessionCache() {
    }

    /**
     * Adds or replaces a player's session.
     * @param session The session to store.
     */
    static void put(ActiveProfessionSession session) {
        ACTIVE_SESSIONS.put(session.playerUuid(), session);
    }

    /**
     * Drops a player's session.
     * @param playerUuid The UUID of the player.
     */
    static void remove(UUID playerUuid) {
        ACTIVE_SESSIONS.remove(playerUuid);
    }

    /**
     * Reads a player's session.
     * @param playerUuid The UUID of the player.
     * @return The session, or empty when the player has none cached.
     */
    static Optional<ActiveProfessionSession> get(UUID playerUuid) {
        return Optional.ofNullable(ACTIVE_SESSIONS.get(playerUuid));
    }

    /**
     * Atomically derives a new session from the cached one, doing nothing when
     * the player has no session cached.
     * @param playerUuid The UUID of the player.
     * @param change Produces the replacement from the current session.
     */
    static void update(UUID playerUuid, UnaryOperator<ActiveProfessionSession> change) {
        ACTIVE_SESSIONS.computeIfPresent(playerUuid, (uuid, session) -> change.apply(session));
    }

    /**
     * Empties the cache.
     */
    static void clear() {
        ACTIVE_SESSIONS.clear();
    }
}

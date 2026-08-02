package io.github.mcoriax.mcidentity.common;

import io.github.mcoriax.mcidentity.api.model.ActiveIdentitySession;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/**
 * The RAM cache of which identity every online player is currently playing as.
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
    private static final Map<UUID, ActiveIdentitySession> ACTIVE_SESSIONS = new ConcurrentHashMap<>();

    /**
     * How many alternative base identity slots each player has unlocked, keyed
     * by player UUID. Cached so base identities can be recognised without a
     * database round trip on a hot path.
     */
    private static final Map<UUID, Integer> BASE_IDENTITY_SLOTS = new ConcurrentHashMap<>();

    /**
     * Prevents instantiation of this state holder.
     */
    private SessionCache() {
    }

    /**
     * Adds or replaces a player's session.
     * @param session The session to store.
     */
    static void put(ActiveIdentitySession session) {
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
    static Optional<ActiveIdentitySession> get(UUID playerUuid) {
        return Optional.ofNullable(ACTIVE_SESSIONS.get(playerUuid));
    }

    /**
     * Atomically derives a new session from the cached one, doing nothing when
     * the player has no session cached.
     * @param playerUuid The UUID of the player.
     * @param change Produces the replacement from the current session.
     */
    static void update(UUID playerUuid, UnaryOperator<ActiveIdentitySession> change) {
        ACTIVE_SESSIONS.computeIfPresent(playerUuid, (uuid, session) -> change.apply(session));
    }

    /**
     * Finds the session wearing a given identity.
     * @param identityUuid The UUID of the identity.
     * @return The session, or empty when nobody is wearing it.
     */
    static Optional<ActiveIdentitySession> getByIdentity(UUID identityUuid) {
        return ACTIVE_SESSIONS.values().stream()
            .filter(session -> session.identityUuid().equals(identityUuid))
            .findFirst();
    }

    /**
     * Caches a player's unlocked alternative base identity slot count.
     * @param playerUuid The UUID of the player.
     * @param slots The slot count; negative values are stored as zero.
     */
    static void putBaseIdentitySlots(UUID playerUuid, int slots) {
        BASE_IDENTITY_SLOTS.put(playerUuid, Math.max(0, slots));
    }

    /**
     * Reads a player's cached alternative base identity slot count.
     * @param playerUuid The UUID of the player.
     * @return The slot count, 0 when nothing is cached.
     */
    static int getBaseIdentitySlots(UUID playerUuid) {
        return BASE_IDENTITY_SLOTS.getOrDefault(playerUuid, 0);
    }

    /**
     * Drops a player's cached alternative base identity slot count.
     * @param playerUuid The UUID of the player.
     */
    static void removeBaseIdentitySlots(UUID playerUuid) {
        BASE_IDENTITY_SLOTS.remove(playerUuid);
    }

    /**
     * Empties both caches.
     */
    static void clear() {
        ACTIVE_SESSIONS.clear();
        BASE_IDENTITY_SLOTS.clear();
    }
}

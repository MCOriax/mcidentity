package io.gitlab.mcoriax.bukkit.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight per-key cooldown tracker used by EXP listeners to rate-limit
 * easily spammable actions (ringing a bell, re-entering a vehicle, etc.).
 *
 * <p>State is held in a {@link ConcurrentHashMap} so the tracker is safe to read
 * and update from any server thread, including Folia region threads.</p>
 */
public final class CooldownTracker {

    /**
     * Last acceptance timestamp (epoch millis) per key.
     */
    private final Map<UUID, Long> lastAccepted = new ConcurrentHashMap<>();
    /**
     * Cooldown window in milliseconds.
     */
    private final long cooldownMillis;

    /**
     * Creates a new cooldown tracker.
     * @param cooldownMillis The minimum delay between two accepted actions for a key.
     */
    public CooldownTracker(long cooldownMillis) {
        this.cooldownMillis = cooldownMillis;
    }

    /**
     * Attempts to acquire the cooldown for a key.
     * @param key The key (typically a player UUID).
     * @return true if the action is allowed now, false if it is still on cooldown.
     */
    public boolean tryAcquire(UUID key) {
        long now = System.currentTimeMillis();
        Long previous = lastAccepted.get(key);
        if (previous != null && now - previous < cooldownMillis) {
            return false;
        }
        lastAccepted.put(key, now);
        return true;
    }

    /**
     * Clears any tracked state for a key (e.g. when a player disconnects).
     * @param key The key to forget.
     */
    public void clear(UUID key) {
        lastAccepted.remove(key);
    }
}

package io.github.mcoriax.mcidentity.common;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Short-lived per-player gameplay state: identity swap cooldowns and the
 * in-flight equip guard.
 *
 * <p><strong>Internal.</strong> Package-private on purpose: gameplay code
 * reaches this state exclusively through {@link MCIdentityProvider} or the
 * matching methods on {@link MCIdentityManager}. Extracted so the manager holds
 * a facade rather than also owning the cooldown bookkeeping.</p>
 *
 * <p>Both collections are concurrent because they are read and written from the
 * main thread, from region threads on Folia, and from asynchronous callbacks.</p>
 */
final class PlayerStateStore {

    /**
     * When each player last swapped identity, in epoch milliseconds. A player
     * disappears from this map once their cooldown has elapsed.
     */
    private static final Map<UUID, Long> SWAP_COOLDOWNS = new ConcurrentHashMap<>();

    /**
     * Players with an equip operation already in flight, used to reject a
     * second one before the first has finished writing.
     */
    private static final Set<UUID> PENDING_EQUIPS = ConcurrentHashMap.newKeySet();

    /**
     * How long a player must wait between identity swaps, in minutes. Zero
     * disables the cooldown entirely.
     */
    private static volatile int swapCooldownMinutes = 0;

    /**
     * Prevents instantiation of this state holder.
     */
    private PlayerStateStore() {
    }

    /**
     * Sets how long players must wait between identity swaps.
     * @param minutes The cooldown in minutes; zero or less disables it.
     */
    static void setSwapCooldownMinutes(int minutes) {
        swapCooldownMinutes = minutes;
    }

    /**
     * Checks whether a player is still within their swap cooldown, clearing
     * the record once it has elapsed.
     * @param playerId The UUID of the player.
     * @return True while the player must still wait.
     */
    static boolean isOnSwapCooldown(UUID playerId) {
        if (swapCooldownMinutes <= 0) return false;

        Long lastSwap = SWAP_COOLDOWNS.get(playerId);
        if (lastSwap == null) return false;

        long elapsedMillis = System.currentTimeMillis() - lastSwap;
        long cooldownMillis = TimeUnit.MINUTES.toMillis(swapCooldownMinutes);

        if (elapsedMillis >= cooldownMillis) {
            SWAP_COOLDOWNS.remove(playerId);
            return false;
        }
        return true;
    }

    /**
     * Starts a player's swap cooldown from now.
     * @param playerId The UUID of the player.
     */
    static void setSwapCooldown(UUID playerId) {
        SWAP_COOLDOWNS.put(playerId, System.currentTimeMillis());
    }

    /**
     * Ends a player's swap cooldown immediately.
     * @param playerId The UUID of the player.
     */
    static void clearSwapCooldown(UUID playerId) {
        SWAP_COOLDOWNS.remove(playerId);
    }

    /**
     * Gets how long a player must still wait before swapping again.
     * @param playerId The UUID of the player.
     * @return The remaining milliseconds, never negative.
     */
    static long getRemainingSwapCooldownMillis(UUID playerId) {
        Long lastSwap = SWAP_COOLDOWNS.get(playerId);
        if (lastSwap == null) return 0;

        long elapsedMillis = System.currentTimeMillis() - lastSwap;
        long cooldownMillis = TimeUnit.MINUTES.toMillis(swapCooldownMinutes);
        long remainingMillis = cooldownMillis - elapsedMillis;

        return Math.max(0, remainingMillis);
    }

    /**
     * Checks whether a player already has an equip operation in flight.
     * @param playerId The UUID of the player.
     * @return True when one is pending.
     */
    static boolean isEquipPending(UUID playerId) {
        return PENDING_EQUIPS.contains(playerId);
    }

    /**
     * Marks whether a player has an equip operation in flight.
     * @param playerId The UUID of the player.
     * @param pending True to mark one as pending, false to clear it.
     */
    static void setEquipPending(UUID playerId, boolean pending) {
        if (pending) {
            PENDING_EQUIPS.add(playerId);
        } else {
            PENDING_EQUIPS.remove(playerId);
        }
    }

    /**
     * Renders a duration as a short "1m 30s" style string.
     * @param millis The duration in milliseconds.
     * @return The formatted duration, "0s" for anything non-positive.
     */
    static String formatTime(long millis) {
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
}

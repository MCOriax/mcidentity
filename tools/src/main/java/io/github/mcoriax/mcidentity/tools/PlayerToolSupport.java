package io.github.mcoriax.mcidentity.tools;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * The shared fail closed caller check of the identity tools.
 */
final class PlayerToolSupport {

    /**
     * The answer given to any caller that is not an online player.
     */
    static final String NOT_A_PLAYER = "Error: This tool is only available to an in-game player.";

    /**
     * The answer given when the player has no identity equipped.
     */
    static final String NO_IDENTITY = "You have no active identity equipped.";

    /**
     * The answer given when the identity data could not be read.
     */
    static final String READ_FAILED = "Error: Your identity data could not be read right now. Please try again shortly.";

    /**
     * Prevents instantiation: this class holds static helpers only.
     */
    private PlayerToolSupport() {
    }

    /**
     * Resolves the caller to an online player and formats that player's own
     * active identity. The caller's UUID is the only one ever read, so a tool
     * built on this can never report another player's data.
     *
     * @param port The server port.
     * @param caller The account the tool runs for.
     * @param format Turns the identity into the answer.
     * @return A future holding the answer; it never completes exceptionally.
     */
    static CompletableFuture<String> withOwnIdentity(IdentityToolPort port, ToolCaller caller,
                                                     Function<IdentitySnapshot, String> format) {
        Optional<UUID> playerUuid = caller == null ? Optional.empty() : caller.playerUuid();
        if (playerUuid.isEmpty()) {
            return CompletableFuture.completedFuture(NOT_A_PLAYER);
        }
        UUID uuid = playerUuid.get();
        return port.isOnlinePlayer(uuid)
            .thenCompose(online -> Boolean.TRUE.equals(online)
                ? port.activeIdentity(uuid).thenApply(identity -> identity.map(format).orElse(NO_IDENTITY))
                : CompletableFuture.completedFuture(NOT_A_PLAYER))
            .exceptionally(ex -> READ_FAILED);
    }

    /**
     * Formats a level cap for the AI.
     * @param maxLevel The level cap.
     * @return The cap, or {@code no cap} when the profession has none.
     */
    static String formatCap(int maxLevel) {
        return maxLevel <= 0 || maxLevel == Integer.MAX_VALUE ? "no cap" : String.valueOf(maxLevel);
    }

    /**
     * Formats a live value without a trailing {@code .0}.
     * @param value The value.
     * @return The formatted value.
     */
    static String formatValue(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}

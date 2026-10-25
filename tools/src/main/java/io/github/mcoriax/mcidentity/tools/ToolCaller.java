package io.github.mcoriax.mcidentity.tools;

import java.util.Optional;
import java.util.UUID;

/**
 * The account an AI tool call is made for, read off the MCAgents
 * {@code AIAccount}.
 *
 * @param accountType The account type, for example {@code player} or {@code server}; may be null.
 * @param accountUuid The account's identifier within that type; may be null.
 */
public record ToolCaller(String accountType, String accountUuid) {

    /**
     * The account type MCAgents gives an in-game player.
     */
    public static final String PLAYER_TYPE = "player";

    /**
     * A caller that could not be read, which every tool refuses.
     */
    public static final ToolCaller UNKNOWN = new ToolCaller(null, null);

    /**
     * Reports whether the caller claims to be a player.
     * @return True when the account type is {@code player}.
     */
    public boolean isPlayer() {
        return PLAYER_TYPE.equalsIgnoreCase(accountType);
    }

    /**
     * Gets the caller's player UUID.
     * @return The UUID when the caller is a player with a well-formed UUID, otherwise empty.
     */
    public Optional<UUID> playerUuid() {
        if (!isPlayer() || accountUuid == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(accountUuid.trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

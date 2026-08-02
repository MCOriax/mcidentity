package io.github.mcoriax.mcidentity.bukkit.listeners.utils;

import io.github.mcoriax.mcidentity.common.BaseProfession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import org.bukkit.entity.Player;

/**
 * Resolves the name a player is known by while wearing an identity.
 *
 * <p>The active identity's alias, when the player has claimed one, replaces the
 * Minecraft username everywhere the player is named — chat and the tab list.
 * The lookup reads the RAM session cache only, so it is safe to call from the
 * game thread and from an asynchronous chat event alike; a cold cache simply
 * yields the username.</p>
 *
 * <p>The base {@code player} profession is the one exception: it is the
 * player's own character rather than a role they put on, so it always shows the
 * Minecraft name. An alias stored against such an identity by an older release
 * is ignored here rather than deleted, so nothing is lost if the rule is ever
 * relaxed again.</p>
 */
public final class ResolveIdentityAlias {

    /**
     * Prevents instantiation of this utility holder.
     */
    private ResolveIdentityAlias() {
    }

    /**
     * Resolves the display name of a player's active identity.
     * @param player The player to name.
     * @return The active identity's alias, or the player's Minecraft name when
     *         no session is cached, the identity has no alias, or the identity
     *         is the base {@code player} profession.
     */
    public static String execute(Player player) {
        return MCIdentityManager.getSession(player.getUniqueId())
            .filter(session -> !BaseProfession.isBaseProfession(session.identityKey().getProfessionCode()))
            .map(session -> session.properties().getAlias())
            .filter(alias -> !alias.isBlank())
            .orElseGet(player::getName);
    }
}

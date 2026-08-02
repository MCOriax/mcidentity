package io.github.mcoriax.mcidentity.bukkit.listeners.utils;

import io.github.mcoriax.mcidentity.api.model.ActiveIdentitySession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Builds the name tag shown for a player in the tab list and in chat.
 *
 * <p>The tag is the active identity's profession followed by the identity's
 * alias, e.g. {@code [Miner] Rockbiter}. Keeping the layout in one place is
 * what stops the periodic visual refresh and the instant equip/unequip refresh
 * from writing two different strings and making the tab list flicker.</p>
 */
public final class BuildIdentityNameTag {

    /**
     * Prevents instantiation of this utility holder.
     */
    private BuildIdentityNameTag() {
    }

    /**
     * Builds the name tag from the player's cached session.
     * @param player The player to name.
     * @return The tagged name, or the bare alias/username when no session is
     *         cached.
     */
    public static String execute(Player player) {
        return MCIdentityManager.getSession(player.getUniqueId())
            .map(session -> execute(player, session))
            .orElseGet(() -> ResolveIdentityAlias.execute(player));
    }

    /**
     * Builds the name tag for an already resolved session.
     * @param player The player to name.
     * @param session The player's active identity session.
     * @return The tagged name.
     */
    public static String execute(Player player, ActiveIdentitySession session) {
        String professionName = MCIdentityManager.getProfessionDisplayName(session.identityKey().getProfessionCode());
        return ChatColor.GRAY + "[" + ChatColor.WHITE + professionName + ChatColor.GRAY + "] "
            + ChatColor.RESET + ResolveIdentityAlias.execute(player);
    }
}

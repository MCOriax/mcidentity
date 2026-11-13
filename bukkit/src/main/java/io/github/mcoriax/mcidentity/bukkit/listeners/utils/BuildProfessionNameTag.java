package io.github.mcoriax.mcidentity.bukkit.listeners.utils;

import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Builds the name tag shown for a player in the tab list and in chat.
 *
 * <p>The tag is the worn profession followed by the player's own name, e.g.
 * {@code [Miner] Steve}. Keeping the layout in one place is what stops the
 * periodic visual refresh and the instant wear/unwear refresh from writing two
 * different strings and making the tab list flicker.</p>
 */
public final class BuildProfessionNameTag {

    /**
     * Prevents instantiation of this utility holder.
     */
    private BuildProfessionNameTag() {
    }

    /**
     * Builds the name tag from the player's cached worn profession.
     * @param player The player to name.
     * @return The tagged name, or the bare username when no session is cached.
     */
    public static String execute(Player player) {
        return MCIdentityManager.getActiveProfession(player.getUniqueId())
            .map(code -> execute(player, code))
            .orElseGet(player::getName);
    }

    /**
     * Builds the name tag for a known profession.
     * @param player The player to name.
     * @param professionCode The worn profession's code.
     * @return The tagged name.
     */
    public static String execute(Player player, String professionCode) {
        String professionName = MCIdentityManager.getProfessionDisplayName(professionCode);
        return ChatColor.GRAY + "[" + ChatColor.WHITE + professionName + ChatColor.GRAY + "] "
            + ChatColor.RESET + player.getName();
    }
}

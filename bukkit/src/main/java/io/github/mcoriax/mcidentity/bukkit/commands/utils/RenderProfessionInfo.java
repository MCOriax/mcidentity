package io.github.mcoriax.mcidentity.bukkit.commands.utils;

import io.github.mcoriax.mcidentity.api.model.ActiveProfessionSession;
import io.github.mcoriax.mcidentity.api.model.PlayerStats;
import io.github.mcoriax.mcidentity.bukkit.util.MessageComponents;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Prints a player's worn profession, its progress, and the player's stats.
 */
public final class RenderProfessionInfo {

    private RenderProfessionInfo() {
    }

    /**
     * Renders the session to the player's chat.
     * @param player The player to display info to.
     * @param session The player's cached session.
     */
    public static void execute(Player player, ActiveProfessionSession session) {
        PlayerStats stats = session.stats();
        player.sendMessage("" + ChatColor.GOLD + ChatColor.BOLD + "--- Profession Data ---");

        player.sendMessage(infoLine("mcidentity.info.profession", "Profession",
                MCIdentityManager.getProfessionDisplayName(session.professionCode()), ChatColor.GREEN));
        player.sendMessage(infoLine("mcidentity.info.level", "Level", String.valueOf(session.progress().getLevel()), ChatColor.YELLOW));
        player.sendMessage(infoLine("mcidentity.info.exp", "Exp", String.valueOf(session.progress().getExp()), ChatColor.YELLOW));
        player.sendMessage(infoLine("mcidentity.info.stat_points", "Stat Points", String.valueOf(stats.getStatPoints()), ChatColor.YELLOW));

        player.sendMessage(infoLine("mcidentity.info.health", "Health", String.valueOf(stats.getHealth()), ChatColor.RED));
        player.sendMessage(infoLine("mcidentity.info.food", "Food", String.valueOf(stats.getFood()), ChatColor.GOLD));
        player.sendMessage(infoLine("mcidentity.info.energy", "Energy", String.valueOf(stats.getEnergy()), ChatColor.BLUE));
        player.sendMessage(infoLine("mcidentity.info.strength", "Strength", String.valueOf(stats.getStrength()), ChatColor.DARK_RED));
        player.sendMessage(infoLine("mcidentity.info.defense", "Defense", String.valueOf(stats.getDefense()), ChatColor.GRAY));
        player.sendMessage(infoLine("mcidentity.info.wisdom", "Wisdom", String.valueOf(stats.getWisdom()), ChatColor.LIGHT_PURPLE));
        player.sendMessage(infoLine("mcidentity.info.luck", "Luck", String.valueOf(stats.getLuck()), ChatColor.GOLD));
        player.sendMessage(infoLine("mcidentity.info.agility", "Agility", String.valueOf(stats.getAgility()), ChatColor.YELLOW));
        player.sendMessage(infoLine("mcidentity.info.resilience", "Resilience", String.valueOf(stats.getResilience()), ChatColor.DARK_GREEN));
    }

    /**
     * Creates a formatted info line.
     * @param key The translation key.
     * @param label The label text.
     * @param value The value text.
     * @param valueColor The color for the value text.
     * @return The formatted line.
     */
    private static String infoLine(String key, String label, String value, ChatColor valueColor) {
        return ChatColor.GRAY + label + ": " + valueColor
                + MessageComponents.value(key + ".value.prefix", "", value, key + ".value.suffix", "");
    }
}

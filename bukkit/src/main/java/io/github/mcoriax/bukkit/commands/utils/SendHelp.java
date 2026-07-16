package io.github.mcoriax.bukkit.commands.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class SendHelp {
    /**
     * Send the help message to a player.
     * @param player The player to send help to.
     */
    public static void execute(Player player) {
        player.sendMessage("" + ChatColor.GOLD + ChatColor.BOLD + "--- MCIdentity Help ---");
        player.sendMessage(helpLine("mcidentity.command.help.identity", "/identity", "Open the main Identity GUI."));
        player.sendMessage(helpLine("mcidentity.command.help.help", "/identity help", "Show this help message."));
        player.sendMessage(helpLine("mcidentity.command.help.check", "/identity check", "Check the identity key in hand or equipped identity."));
        player.sendMessage(helpLine("mcidentity.command.help.unuse", "/identity unuse", "Unequip your current active identity."));
        player.sendMessage(helpLine("mcidentity.command.help.stats", "/identity stats <get|add>", "View or upgrade your identity stats."));

        if (player.hasPermission("mcidentity.admin")) {
            player.sendMessage("" + ChatColor.RED + ChatColor.BOLD + "--- Admin Commands ---");
            player.sendMessage(helpLine("mcidentity.command.help.ability_get", "/identity ability get <system code>", "Get an ability item."));
            player.sendMessage(helpLine("mcidentity.command.help.ability_create", "/identity ability create <ability_code_id> <ability_skin_id> <head_texture_base64> [custom_model_id]", "Create an equippable MythicMobs ability item."));
            player.sendMessage(helpLine("mcidentity.command.help.rune_get", "/identity rune get <system code>", "Get a rune item."));
            player.sendMessage(helpLine("mcidentity.command.help.identity_get", "/identity get <profession|random> [texture]", "Give an identity key item."));
            player.sendMessage(helpLine("mcidentity.command.help.base", "/identity base add <player> [profession] <amount>", "Grant alternative base identity slots (random or a chosen non-player profession)."));
        }
    }

    /**
     * Creates a formatted help line.
     * @param key The translation key (unused; retained for compatibility).
     * @param command The command text.
     * @param description The description text.
     * @return The formatted line.
     */
    private static String helpLine(String key, String command, String description) {
        return ChatColor.YELLOW + command + ChatColor.GRAY + " - " + description;
    }
}

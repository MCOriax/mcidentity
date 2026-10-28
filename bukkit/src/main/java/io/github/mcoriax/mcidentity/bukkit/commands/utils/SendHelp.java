package io.github.mcoriax.mcidentity.bukkit.commands.utils;

import io.github.mcoriax.mcidentity.bukkit.permission.Permissions;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class SendHelp {
    /**
     * Send the help message to a player, listing only the commands the
     * player's permissions open.
     * @param player The player to send help to.
     */
    public static void execute(Player player) {
        player.sendMessage("" + ChatColor.GOLD + ChatColor.BOLD + "--- MCIdentity Help ---");
        sendIfPermitted(player, Permissions.USE, helpLine("mcidentity.command.help.identity", "/identity", "Open the main Identity GUI."));
        sendIfPermitted(player, Permissions.HELP, helpLine("mcidentity.command.help.help", "/identity help", "Show this help message."));
        sendIfPermitted(player, Permissions.WIKI, helpLine("mcidentity.command.help.wiki", "/identity wiki", "Get clickable links to the wiki."));
        sendIfPermitted(player, Permissions.CHECK, helpLine("mcidentity.command.help.check", "/identity check", "Check the identity key in hand or equipped identity."));
        sendIfPermitted(player, Permissions.UNEQUIP, helpLine("mcidentity.command.help.unequip", "/identity unequip", "Unequip your current active identity."));
        sendIfPermitted(player, Permissions.STATS, helpLine("mcidentity.command.help.stats", "/identity stats <get|add>", "View or upgrade your identity stats."));
        sendIfPermitted(player, Permissions.ALIAS, helpLine("mcidentity.command.help.alias", "/identity alias [name]", "Show or claim the alias of your active identity (letters and numbers only, unique network wide)."));

        if (Permissions.grantsAnyAdministrative(player)) {
            player.sendMessage("" + ChatColor.RED + ChatColor.BOLD + "--- Admin Commands ---");
            sendIfPermitted(player, Permissions.GET, helpLine("mcidentity.command.help.identity_get","/identity get <profession|random> [texture]", "Give an identity key item."));
            sendIfPermitted(player, Permissions.BASE, helpLine("mcidentity.command.help.base", "/identity base add <player> [profession] <amount>", "Grant alternative base identity slots (random or a chosen non-player profession)."));
        }
    }

    /**
     * Sends one help line when the player holds the node it documents.
     * @param player The player to send help to.
     * @param node The node the documented command requires.
     * @param line The formatted help line.
     */
    private static void sendIfPermitted(Player player, String node, String line) {
        if (Permissions.grants(player, node)) {
            player.sendMessage(line);
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

package io.gitlab.mcoriax.foliamc.commands.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

public class SendHelp {
    /**
     * Send the help message to a player.
     * @param player The player to send help to.
     */
    public static void execute(Player player) {
        player.sendMessage(Component.translatable("mcidentity.command.help.header", "--- MCIdentity Help ---").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        player.sendMessage(helpLine("mcidentity.command.help.identity", "/identity", "Open the main Identity GUI."));
        player.sendMessage(helpLine("mcidentity.command.help.help", "/identity help", "Show this help message."));
        player.sendMessage(helpLine("mcidentity.command.help.check", "/identity check", "Check the identity key in hand or equipped identity."));
        player.sendMessage(helpLine("mcidentity.command.help.unuse", "/identity unuse", "Unequip your current active identity."));
        player.sendMessage(helpLine("mcidentity.command.help.stats", "/identity stats <get|add>", "View or upgrade your identity stats."));
        
        if (player.hasPermission("mcidentity.admin")) {
            player.sendMessage(Component.translatable("mcidentity.command.help.admin_header", "--- Admin Commands ---").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
            player.sendMessage(helpLine("mcidentity.command.help.ability_get", "/identity ability get <system code>", "Get an ability item."));
            player.sendMessage(helpLine("mcidentity.command.help.rune_get", "/identity rune get <system code>", "Get a rune item."));
            player.sendMessage(helpLine("mcidentity.command.help.identity_get", "/identity get <profession|random> [texture]", "Give an identity key item."));
        }
    }

    /**
     * Creates a formatted help line component.
     * @param key The translation key.
     * @param command The command text.
     * @param description The description text.
     * @return The formatted component.
     */
    private static Component helpLine(String key, String command, String description) {
        return Component.translatable(key + ".command", command).color(NamedTextColor.YELLOW)
                .append(Component.translatable(key + ".description", " - " + description).color(NamedTextColor.GRAY));
    }
}

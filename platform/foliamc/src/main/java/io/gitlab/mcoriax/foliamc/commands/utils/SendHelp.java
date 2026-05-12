package io.gitlab.mcoriax.foliamc.commands.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

public class SendHelp {
    /**
     * Send the help message to a player.
     */
    public static void execute(Player player) {
        player.sendMessage(Component.text("--- MCIdentity Help ---", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("/identity", NamedTextColor.YELLOW).append(Component.text(" - Open the main Identity GUI.", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/identity help", NamedTextColor.YELLOW).append(Component.text(" - Show this help message.", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/identity check", NamedTextColor.YELLOW).append(Component.text(" - Check the identity key in hand or equipped identity.", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/identity unuse", NamedTextColor.YELLOW).append(Component.text(" - Unequip your current active identity.", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/identity stats <get|add>", NamedTextColor.YELLOW).append(Component.text(" - View or upgrade your identity stats.", NamedTextColor.GRAY)));
        
        if (player.hasPermission("mcidentity.admin")) {
            player.sendMessage(Component.text("--- Admin Commands ---", NamedTextColor.RED, TextDecoration.BOLD));
            player.sendMessage(Component.text("/identity ability get <system code>", NamedTextColor.YELLOW).append(Component.text(" - Get an ability item.", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/identity rune get <system code>", NamedTextColor.YELLOW).append(Component.text(" - Get a rune item.", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/identity get <profession|random> [texture]", NamedTextColor.YELLOW).append(Component.text(" - Give an identity key item.", NamedTextColor.GRAY)));
        }
    }
}

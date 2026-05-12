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
        player.sendMessage(Component.translatable("mcidentity.command.help.header", "--- MCIdentity Help ---").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("/identity", NamedTextColor.YELLOW).append(Component.translatable("mcidentity.command.help.main", " - Open the main Identity GUI.").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/identity help", NamedTextColor.YELLOW).append(Component.translatable("mcidentity.command.help.help", " - Show this help message.").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/identity check", NamedTextColor.YELLOW).append(Component.translatable("mcidentity.command.help.check", " - Check the identity key in hand or equipped identity.").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/identity unuse", NamedTextColor.YELLOW).append(Component.translatable("mcidentity.command.help.unuse", " - Unequip your current active identity.").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/identity stats <get|add>", NamedTextColor.YELLOW).append(Component.translatable("mcidentity.command.help.stats", " - View or upgrade your identity stats.").color(NamedTextColor.GRAY)));
        
        if (player.hasPermission("mcidentity.admin")) {
            player.sendMessage(Component.translatable("mcidentity.command.help.admin_header", "--- Admin Commands ---").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
            player.sendMessage(Component.text("/identity ability get <system code>", NamedTextColor.YELLOW).append(Component.translatable("mcidentity.command.help.admin_ability", " - Get an ability item.").color(NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/identity rune get <system code>", NamedTextColor.YELLOW).append(Component.translatable("mcidentity.command.help.admin_rune", " - Get a rune item.").color(NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/identity get <profession|random> [texture]", NamedTextColor.YELLOW).append(Component.translatable("mcidentity.command.help.admin_get", " - Give an identity key item.").color(NamedTextColor.GRAY)));
        }
    }
}

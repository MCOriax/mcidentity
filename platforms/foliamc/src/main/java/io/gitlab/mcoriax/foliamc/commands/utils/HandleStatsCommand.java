package io.gitlab.mcoriax.foliamc.commands.utils;

import io.gitlab.mcoriax.api.model.ActiveIdentitySession;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.Optional;

public class HandleStatsCommand {
    /**
     * Handle the /identity stats command.
     */
    public static void execute(MCIdentityPlugin plugin, Player player, String[] args) {
        Optional<ActiveIdentitySession> optSession = MCIdentityManager.getSession(player.getUniqueId());
        if (optSession.isEmpty()) {
            player.sendMessage(Component.translatable("mcidentity.command.error.active_identity_required", "You must have an active identity to use this command!").color(NamedTextColor.RED));
            return;
        }

        ActiveIdentitySession session = optSession.get();

        if (args.length == 1 || (args.length >= 2 && args[1].equalsIgnoreCase("get"))) {
            int points = session.properties().getStatPoints();
            player.sendMessage(Component.translatable("mcidentity.command.stats.header", "--- Identity Stats ---").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
            player.sendMessage(Component.translatable(
                    "mcidentity.command.stats.remaining_points",
                    "Remaining Stat Points: " + points,
                    Component.text(points)
            ).color(NamedTextColor.GRAY));
            return;
        }

        if (args.length >= 4 && args[1].equalsIgnoreCase("add")) {
            String statType = args[2].toLowerCase();
            int amount;
            try {
                amount = Integer.parseInt(args[3]);
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                player.sendMessage(Component.translatable("mcidentity.command.error.invalid_amount", "Invalid amount! Please enter a positive integer.").color(NamedTextColor.RED));
                return;
            }

            int currentPoints = session.properties().getStatPoints();
            if (currentPoints < amount) {
                player.sendMessage(Component.translatable(
                        "mcidentity.command.error.not_enough_stat_points",
                        "Not enough stat points! You only have " + currentPoints + " points.",
                        Component.text(currentPoints)
                ).color(NamedTextColor.RED));
                return;
            }

            UpdateIdentityStats.execute(plugin, player, session, statType, amount);
            return;
        }

        player.sendMessage(Component.translatable("mcidentity.command.usage.stats", "Usage: /identity stats <get|add <stat_type> <amount>>").color(NamedTextColor.RED));
    }
}

package io.github.mcoriax.mcidentity.bukkit.commands.utils;
import org.bukkit.ChatColor;

import io.github.mcoriax.mcidentity.api.model.ActiveIdentitySession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.mcidentity.bukkit.util.MessageComponents;
import org.bukkit.entity.Player;

import java.util.Optional;

public class HandleStatsCommand {
    /**
     * Handle the /identity stats command.
     * @param plugin The plugin instance.
     * @param player The player who issued the command.
     * @param args The command arguments.
     */
    public static void execute(JavaPlugin plugin, Player player, String[] args) {
        Optional<ActiveIdentitySession> optSession = MCIdentityManager.getSession(player.getUniqueId());
        if (optSession.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You must have an active identity to use this command!");
            return;
        }

        ActiveIdentitySession session = optSession.get();

        if (args.length == 1 || (args.length >= 2 && args[1].equalsIgnoreCase("get"))) {
            int points = session.properties().getStatPoints();
            player.sendMessage("" + ChatColor.GOLD + ChatColor.BOLD + "--- Identity Stats ---");
            player.sendMessage(ChatColor.GRAY + MessageComponents.value(
                    "mcidentity.command.stats.remaining_points.prefix",
                    "Remaining Stat Points: ",
                    points,
                    "mcidentity.command.stats.remaining_points.suffix",
                    ""
            ));
            return;
        }

        if (args.length >= 4 && args[1].equalsIgnoreCase("add")) {
            String statType = args[2].toLowerCase();
            int amount;
            try {
                amount = Integer.parseInt(args[3]);
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid amount! Please enter a positive integer.");
                return;
            }

            int currentPoints = session.properties().getStatPoints();
            if (currentPoints < amount) {
                player.sendMessage(ChatColor.RED + MessageComponents.value(
                        "mcidentity.command.error.not_enough_stat_points.prefix",
                        "Not enough stat points! You only have ",
                        currentPoints,
                        "mcidentity.command.error.not_enough_stat_points.suffix",
                        " points."
                ));
                return;
            }

            UpdateIdentityStats.execute(plugin, player, session, statType, amount);
            return;
        }

        player.sendMessage(ChatColor.RED + "Usage: /identity stats <get|add <stat_type> <amount>>");
    }
}

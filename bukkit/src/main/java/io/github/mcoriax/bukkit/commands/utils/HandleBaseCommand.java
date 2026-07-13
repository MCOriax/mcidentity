package io.github.mcoriax.bukkit.commands.utils;

import io.github.mcoriax.bukkit.scheduler.Schedulers;
import io.github.mcoriax.common.MCIdentityProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Handles the OP-only {@code /identity base <player> <amount>} sub-command,
 * which grants a player additional alternative base "player" identity slots.
 * The slot count is incremented in the database and the new alternative
 * identities are registered under their deterministic UUIDs.
 */
public class HandleBaseCommand {

    /**
     * Executes the base sub-command.
     * @param plugin The plugin instance.
     * @param player The admin who issued the command.
     * @param args The full command arguments ("base", player name, amount).
     */
    public static void execute(JavaPlugin plugin, Player player, String[] args) {
        if (!player.hasPermission("mcidentity.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /identity base <player name> <amount>");
            return;
        }

        String targetName = args[1];
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "The amount must be a whole number!");
            return;
        }
        if (amount < 1) {
            player.sendMessage(ChatColor.RED + "The amount must be at least 1!");
            return;
        }

        UUID targetUuid = resolvePlayerUuid(targetName);
        if (targetUuid == null) {
            player.sendMessage(ChatColor.RED + "Player '" + targetName + "' was not found!");
            return;
        }

        MCIdentityProvider.instance.addBaseProfession(targetUuid, amount)
            .whenComplete((total, ex) -> Schedulers.forEntity(player).run(plugin, st -> {
                if (ex != null) {
                    player.sendMessage(ChatColor.RED + "Failed to add base identity slots. Check the console for details.");
                    plugin.getLogger().severe("Failed to add base identity slots for " + targetName + ": " + ex.getMessage());
                    return;
                }
                player.sendMessage(ChatColor.GREEN + "Added " + amount + " alternative identity slot" + (amount == 1 ? "" : "s")
                    + " to " + targetName + ". They now own " + total + " alternative identit" + (total == 1 ? "y" : "ies") + ".");
            }, null));
    }

    /**
     * Resolves a player name to a UUID, preferring an exact online match and
     * falling back to previously seen offline players.
     * @param name The player name.
     * @return The player's UUID, or null when the player is unknown.
     */
    @SuppressWarnings("deprecation")
    private static UUID resolvePlayerUuid(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online.getUniqueId();
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline.hasPlayedBefore() ? offline.getUniqueId() : null;
    }
}

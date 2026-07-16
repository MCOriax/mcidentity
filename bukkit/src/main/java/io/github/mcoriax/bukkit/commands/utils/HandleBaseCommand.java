package io.github.mcoriax.bukkit.commands.utils;

import io.github.mcoriax.bukkit.scheduler.Schedulers;
import io.github.mcoriax.common.BaseProfession;
import io.github.mcoriax.common.MCIdentityManager;
import io.github.mcoriax.common.MCIdentityProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Handles the OP-only {@code /identity base add} sub-command, which grants a
 * player additional alternative base identity slots.
 *
 * <p>Two forms are accepted:</p>
 * <ul>
 *   <li>{@code /identity base add <player> <amount>} — each new slot is given a
 *   random registered profession.</li>
 *   <li>{@code /identity base add <player> <profession> <amount>} — each new
 *   slot is given the requested profession.</li>
 * </ul>
 *
 * <p>The {@code player} profession is reserved for the primary identity and is
 * rejected as an explicit choice; it is also excluded from the random pool.</p>
 */
public class HandleBaseCommand {

    /**
     * Executes the base sub-command.
     * @param plugin The plugin instance.
     * @param player The admin who issued the command.
     * @param args The full command arguments ("base", "add", player name,
     *             optional profession, amount).
     */
    public static void execute(JavaPlugin plugin, Player player, String[] args) {
        if (!player.hasPermission("mcidentity.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return;
        }

        // /identity base add <player> <amount>
        // /identity base add <player> <profession> <amount>
        if (args.length < 4 || !args[1].equalsIgnoreCase("add")) {
            player.sendMessage(ChatColor.RED + "Usage: /identity base add <player name> [profession] <amount>");
            return;
        }

        String targetName = args[2];
        String professionArg = args.length >= 5 ? args[3] : null;
        String amountArg = args.length >= 5 ? args[4] : args[3];

        int amount;
        try {
            amount = Integer.parseInt(amountArg);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "The amount must be a whole number!");
            return;
        }
        if (amount < 1) {
            player.sendMessage(ChatColor.RED + "The amount must be at least 1!");
            return;
        }

        // The player profession is reserved for the primary identity only.
        if (professionArg != null && BaseProfession.isBaseProfession(professionArg)) {
            player.sendMessage(ChatColor.RED + "The '" + BaseProfession.CODE + "' profession is reserved for the primary identity and cannot be added as an alternative!");
            return;
        }
        if (professionArg != null && !professionArg.equalsIgnoreCase("random")
                && !MCIdentityManager.getRegisteredProfessions().containsKey(professionArg.toLowerCase())) {
            player.sendMessage(ChatColor.RED + "Unknown profession '" + professionArg + "'.");
            return;
        }

        UUID targetUuid = resolvePlayerUuid(targetName);
        if (targetUuid == null) {
            player.sendMessage(ChatColor.RED + "Player '" + targetName + "' was not found!");
            return;
        }

        // A null/"random" profession makes each slot pick a random non-player class.
        final String professionCode = (professionArg == null || professionArg.equalsIgnoreCase("random"))
            ? null : professionArg.toLowerCase();
        final String professionLabel = professionCode == null ? "random" : professionCode;

        MCIdentityProvider.instance.addBaseProfession(targetUuid, professionCode, amount)
            .whenComplete((total, ex) -> Schedulers.forEntity(player).run(plugin, st -> {
                if (ex != null) {
                    player.sendMessage(ChatColor.RED + "Failed to add base identity slots. Check the console for details.");
                    plugin.getLogger().severe("Failed to add base identity slots for " + targetName + ": " + ex.getMessage());
                    return;
                }
                // Keep the online target's cached slot count in sync so their
                // new alternatives are recognised as base identities at once.
                Player target = Bukkit.getPlayer(targetUuid);
                if (target != null && target.isOnline()) {
                    MCIdentityManager.cacheBaseIdentitySlots(targetUuid, total);
                }
                player.sendMessage(ChatColor.GREEN + "Added " + amount + " alternative identity slot" + (amount == 1 ? "" : "s")
                    + " (" + professionLabel + ") to " + targetName + ". They now own " + total + " alternative identit" + (total == 1 ? "y" : "ies") + ".");
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

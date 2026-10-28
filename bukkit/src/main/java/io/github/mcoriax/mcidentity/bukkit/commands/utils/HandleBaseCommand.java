package io.github.mcoriax.mcidentity.bukkit.commands.utils;

import io.github.mcoriax.mcidentity.bukkit.permission.Permissions;

import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;
import io.github.mcoriax.mcidentity.common.BaseProfession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
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
     * Profession argument asking for a random non-player class per slot.
     */
    private static final String RANDOM_PROFESSION = "random";

    /**
     * Executes the base sub-command.
     * @param plugin The plugin instance.
     * @param player The admin who issued the command.
     * @param args The full command arguments ("base", "add", player name,
     *             optional profession, amount).
     */
    public static void execute(JavaPlugin plugin, Player player, String[] args) {
        if (!Permissions.grants(player, Permissions.BASE)) {
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

        int amount = parseAmount(player, amountArg);
        if (amount < 1 || !professionIsGrantable(player, professionArg)) {
            return;
        }

        UUID targetUuid = resolvePlayerUuid(targetName);
        if (targetUuid == null) {
            player.sendMessage(ChatColor.RED + "Player '" + targetName + "' was not found!");
            return;
        }

        // A null/RANDOM_PROFESSION profession makes each slot pick a random non-player class.
        final String professionCode = (professionArg == null || professionArg.equalsIgnoreCase(RANDOM_PROFESSION))
            ? null : professionArg.toLowerCase();

        grantSlots(plugin, player, targetName, targetUuid, professionCode, amount);
    }

    /**
     * Reads the slot count, telling the admin when it is not a usable number.
     * @param player The admin who issued the command.
     * @param amountArg The typed amount.
     * @return The amount, or 0 when it was rejected.
     */
    private static int parseAmount(Player player, String amountArg) {
        int amount;
        try {
            amount = Integer.parseInt(amountArg);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "The amount must be a whole number!");
            return 0;
        }
        if (amount < 1) {
            player.sendMessage(ChatColor.RED + "The amount must be at least 1!");
            return 0;
        }
        return amount;
    }

    /**
     * Reports whether the requested profession may fill an alternative slot.
     * @param player The admin who issued the command.
     * @param professionArg The typed profession, or null for random.
     * @return true when the profession is usable.
     */
    private static boolean professionIsGrantable(Player player, String professionArg) {
        if (professionArg == null || professionArg.equalsIgnoreCase(RANDOM_PROFESSION)) {
            return true;
        }
        // The player profession is reserved for the primary identity only.
        if (BaseProfession.isBaseProfession(professionArg)) {
            player.sendMessage(ChatColor.RED + "The '" + BaseProfession.CODE + "' profession is reserved for the primary identity and cannot be added as an alternative!");
            return false;
        }
        if (!MCIdentityManager.getRegisteredProfessions().containsKey(professionArg.toLowerCase())) {
            player.sendMessage(ChatColor.RED + "Unknown profession '" + professionArg + "'.");
            return false;
        }
        return true;
    }

    /**
     * Adds the slots and reports the new total back to the admin.
     * @param plugin The plugin instance.
     * @param player The admin who issued the command.
     * @param targetName The target's name, as typed.
     * @param targetUuid The target's UUID.
     * @param professionCode The profession every new slot takes, or null for random.
     * @param amount How many slots to add.
     */
    private static void grantSlots(JavaPlugin plugin, Player player, String targetName, UUID targetUuid,
                                   String professionCode, int amount) {
        final String professionLabel = professionCode == null ? RANDOM_PROFESSION : professionCode;
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

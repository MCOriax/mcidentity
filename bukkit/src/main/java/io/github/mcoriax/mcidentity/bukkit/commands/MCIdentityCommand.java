package io.github.mcoriax.mcidentity.bukkit.commands;

import io.github.mcoriax.mcidentity.api.model.ActiveProfessionSession;
import io.github.mcoriax.mcidentity.bukkit.commands.utils.HandleStatsCommand;
import io.github.mcoriax.mcidentity.bukkit.commands.utils.OpenIdentityGui;
import io.github.mcoriax.mcidentity.bukkit.commands.utils.RenderProfessionInfo;
import io.github.mcoriax.mcidentity.bukkit.commands.utils.SendHelp;
import io.github.mcoriax.mcidentity.bukkit.commands.utils.SendWiki;
import io.github.mcoriax.mcidentity.bukkit.permission.Permissions;
import io.github.mcoriax.mcidentity.bukkit.profession.ProfessionWearing;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

/**
 * The {@code /identity} command: the main GUI, help, wiki, check, stats, and
 * wearing or unwearing a profession.
 */
public class MCIdentityCommand implements CommandExecutor {

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;

    /**
     * Creates a new MCIdentityCommand.
     * @param plugin The plugin instance.
     */
    public MCIdentityCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Dispatches an {@code /identity} invocation to its sub-command handler.
     * @param sender The command sender; only players are accepted.
     * @param command The command being run.
     * @param label The alias the sender typed.
     * @param args The sub-command and its arguments.
     * @return True when the invocation was handled, false to print the usage line.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            if (permitted(player, Permissions.USE)) {
                openIdentityGui(player);
            }
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        String node = Permissions.SUB_COMMANDS.get(subCommand);
        if (node == null) {
            return false;
        }
        if (!permitted(player, node)) {
            return true;
        }

        return switch (subCommand) {
            case "help" -> handled(() -> SendHelp.execute(player));
            case "wiki" -> handled(() -> SendWiki.execute(player));
            case "check" -> handled(() -> handleCheck(player));
            case "stats" -> handled(() -> HandleStatsCommand.execute(plugin, player, args));
            case "wear" -> handled(() -> handleWear(player, args));
            case "unwear" -> handled(() -> ProfessionWearing.unwear(plugin, player, false));
            default -> false;
        };
    }

    /**
     * Checks a node and tells the player when it is missing.
     * @param player The player who issued the command.
     * @param node The node the command requires.
     * @return true when the player may run the command.
     */
    private static boolean permitted(Player player, String node) {
        if (Permissions.grants(player, node)) {
            return true;
        }
        player.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
        return false;
    }

    /**
     * Runs a sub-command that always counts as handled.
     * @param subCommand The sub-command body.
     * @return Always true, so the usage line is not printed.
     */
    private static boolean handled(Runnable subCommand) {
        subCommand.run();
        return true;
    }

    /**
     * Handles {@code /identity wear <profession>}.
     * @param player The player who issued the command.
     * @param args The command arguments.
     */
    private void handleWear(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /identity wear <profession>");
            return;
        }
        ProfessionWearing.wear(plugin, player, args[1], false);
    }

    /**
     * Handles {@code /identity check}, describing the worn profession and stats.
     * @param player The player who issued the command.
     */
    private void handleCheck(Player player) {
        Optional<ActiveProfessionSession> session = MCIdentityManager.getSession(player.getUniqueId());
        if (session.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Your profession data is not loaded yet!");
            return;
        }
        RenderProfessionInfo.execute(player, session.get());
    }

    /**
     * Opens the main identity GUI for the player.
     * @param player The player to open the GUI for.
     */
    public void openIdentityGui(Player player) {
        OpenIdentityGui.execute(plugin, player);
    }
}

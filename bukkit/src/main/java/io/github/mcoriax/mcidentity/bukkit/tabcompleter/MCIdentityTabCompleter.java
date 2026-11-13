package io.github.mcoriax.mcidentity.bukkit.tabcompleter;

import io.github.mcoriax.mcidentity.bukkit.permission.Permissions;

import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tab completion for {@code /identity}.
 */
public class MCIdentityTabCompleter implements TabCompleter {

    /**
     * The {@code stats} sub-command.
     */
    private static final String SUB_STATS = "stats";

    /**
     * Placeholder suggested where the player must type a number.
     */
    private static final String AMOUNT_PLACEHOLDER = "<amount>";

    /**
     * The {@code add} action of the stats sub-command.
     */
    private static final String ADD = "add";

    /**
     * Every stat {@code /identity stats add} accepts.
     */
    private static final List<String> STAT_NAMES = List.of(
        "health", "energy", "strength", "defense", "wisdom",
        "luck", "agility", "resilience", "food");

    /**
     * Provides tab completion for the /identity command.
     * @param sender The command sender.
     * @param command The command instance.
     * @param alias The command alias.
     * @param args The command arguments.
     * @return A list of completion suggestions.
     */
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length > 1 && !mayRun(sender, args[0])) {
            return new ArrayList<>();
        }
        return switch (args.length) {
            case 1 -> completeSubCommand(sender, args);
            case 2 -> completeSecondArgument(sender, args);
            case 3 -> completeThirdArgument(args);
            case 4 -> completeFourthArgument(args);
            default -> new ArrayList<>();
        };
    }

    /**
     * Suggests the sub-command itself.
     * @param sender The command sender.
     * @param args The command arguments.
     * @return The matching suggestions.
     */
    private static List<String> completeSubCommand(CommandSender sender, String[] args) {
        // Only the sub-commands the sender may run are offered; the command
        // re-checks the same node, so completion is a hint and never the guard.
        List<String> subCommands = Permissions.SUB_COMMANDS.entrySet().stream()
                .filter(entry -> Permissions.grants(sender, entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        return startingWith(subCommands, args[0]);
    }

    /**
     * Reports whether the sender may run the sub-command an argument list starts with.
     * @param sender The command sender.
     * @param subCommand The typed sub-command.
     * @return true when the sub-command exists and its node is granted.
     */
    private static boolean mayRun(CommandSender sender, String subCommand) {
        String node = Permissions.SUB_COMMANDS.get(subCommand.toLowerCase(Locale.ROOT));
        return node != null && Permissions.grants(sender, node);
    }

    /**
     * Suggests the argument after the sub-command.
     * @param sender The command sender.
     * @param args The command arguments.
     * @return The matching suggestions.
     */
    private static List<String> completeSecondArgument(CommandSender sender, String[] args) {
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case SUB_STATS -> startingWith(Arrays.asList("get", ADD), args[1]);
            case "wear" -> startingWith(wearableProfessions(sender), args[1]);
            default -> new ArrayList<>();
        };
    }

    /**
     * Suggests the stat for {@code /identity stats add}.
     * @param args The command arguments.
     * @return The matching suggestions.
     */
    private static List<String> completeThirdArgument(String[] args) {
        if (args[0].equalsIgnoreCase(SUB_STATS) && args[1].equalsIgnoreCase(ADD)) {
            return startingWith(STAT_NAMES, args[2]);
        }
        return new ArrayList<>();
    }

    /**
     * Suggests the amount that closes {@code /identity stats add}.
     * @param args The command arguments.
     * @return The matching suggestions.
     */
    private static List<String> completeFourthArgument(String[] args) {
        if (args[0].equalsIgnoreCase(SUB_STATS) && args[1].equalsIgnoreCase(ADD)) {
            return Arrays.asList(AMOUNT_PLACEHOLDER);
        }
        return new ArrayList<>();
    }

    /**
     * Lists the registered professions the sender may wear.
     * @param sender The command sender.
     * @return The profession codes.
     */
    private static List<String> wearableProfessions(CommandSender sender) {
        return MCIdentityManager.getRegisteredProfessions().keySet().stream()
                .filter(code -> Permissions.canWear(sender, code))
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Keeps the options the player has already started typing.
     * @param options The candidate suggestions.
     * @param typed What the player typed so far.
     * @return The matching options.
     */
    private static List<String> startingWith(Collection<String> options, String typed) {
        String prefix = typed.toLowerCase();
        return options.stream()
                .filter(s -> s.startsWith(prefix))
                .collect(Collectors.toList());
    }
}
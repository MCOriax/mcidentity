package io.github.mcoriax.mcidentity.bukkit.tabcompleter;

import io.github.mcoriax.mcidentity.bukkit.permission.Permissions;

import io.github.mcoriax.mcidentity.api.model.Profession;
import io.github.mcoriax.mcidentity.common.BaseProfession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

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
     * The {@code base} sub-command.
     */
    private static final String SUB_BASE = "base";

    /**
     * The {@code add} action shared by the stats and base sub-commands.
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
            case 2 -> completeSecondArgument(args);
            case 3 -> completeThirdArgument(args);
            case 4 -> completeFourthArgument(args);
            case 5 -> completeFifthArgument(args);
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
     * @param args The command arguments.
     * @return The matching suggestions.
     */
    private static List<String> completeSecondArgument(String[] args) {
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case SUB_BASE -> startingWith(Arrays.asList(ADD), args[1]);
            case SUB_STATS -> startingWith(Arrays.asList("get", ADD), args[1]);
            // The alias is free text, so only its shape can be hinted at.
            case "alias" -> Arrays.asList("<alias>");
            case "get" -> startingWith(itemProfessions(), args[1]);
            default -> new ArrayList<>();
        };
    }

    /**
     * Suggests the third argument: a player name, a texture, or a stat.
     * @param args The command arguments.
     * @return The matching suggestions.
     */
    private static List<String> completeThirdArgument(String[] args) {
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case SUB_BASE -> completeOnlinePlayer(args);
            case "get" -> Arrays.asList("<texture_base64>");
            case SUB_STATS -> args[1].equalsIgnoreCase(ADD)
                    ? startingWith(STAT_NAMES, args[2])
                    : new ArrayList<>();
            default -> new ArrayList<>();
        };
    }

    /**
     * Suggests an online player name for {@code /identity base add}.
     * @param args The command arguments.
     * @return The matching player names.
     */
    private static List<String> completeOnlinePlayer(String[] args) {
        if (!args[1].equalsIgnoreCase(ADD)) {
            return new ArrayList<>();
        }
        String prefix = args[2].toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix))
                .collect(Collectors.toList());
    }

    /**
     * Suggests the fourth argument: an amount, or a profession for a base slot.
     * @param args The command arguments.
     * @return The matching suggestions.
     */
    private static List<String> completeFourthArgument(String[] args) {
        if (args[0].equalsIgnoreCase(SUB_STATS) && args[1].equalsIgnoreCase(ADD)) {
            return Arrays.asList(AMOUNT_PLACEHOLDER);
        }
        if (!args[0].equalsIgnoreCase(SUB_BASE) || !args[1].equalsIgnoreCase(ADD)) {
            return new ArrayList<>();
        }
        // The 4th argument is either a profession (specific form) or the
        // amount (random form). The player profession is never offered.
        List<String> suggestions = itemProfessions();
        suggestions.add(1, AMOUNT_PLACEHOLDER);
        return startingWith(suggestions, args[3]);
    }

    /**
     * Suggests the amount that closes {@code /identity base add}.
     * @param args The command arguments.
     * @return The matching suggestions.
     */
    private static List<String> completeFifthArgument(String[] args) {
        if (args[0].equalsIgnoreCase(SUB_BASE) && args[1].equalsIgnoreCase(ADD)) {
            return Arrays.asList(AMOUNT_PLACEHOLDER);
        }
        return new ArrayList<>();
    }

    /**
     * Lists every profession an identity item may carry, random included.
     * @return A mutable list of profession codes, without the base profession.
     */
    private static List<String> itemProfessions() {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("random");
        suggestions.addAll(MCIdentityManager.getRegisteredProfessions().keySet());
        // The base profession can never become an item.
        suggestions.remove(BaseProfession.CODE);
        return suggestions;
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
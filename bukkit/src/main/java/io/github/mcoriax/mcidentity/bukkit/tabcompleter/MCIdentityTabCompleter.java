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
import java.util.stream.Collectors;

public class MCIdentityTabCompleter implements TabCompleter {

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
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList("get", "check", "unuse", "help", "stats", "alias", "wiki"));
            // The base slot grant is an admin tool, so only admins see it.
            if (sender.hasPermission(Permissions.ADMIN)) {
                subCommands.add("base");
            }
            return subCommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("base")) {
                if (!sender.hasPermission(Permissions.ADMIN)) {
                    return new ArrayList<>();
                }
                return Arrays.asList("add").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("stats")) {
                return Arrays.asList("get", "add").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args[0].equalsIgnoreCase("alias")) {
                // The alias is free text, so only its shape can be hinted at.
                return Arrays.asList("<alias>");
            }

            if (args[0].equalsIgnoreCase("get")) {
                List<String> suggestions = new ArrayList<>();
                suggestions.add("random");
                suggestions.addAll(MCIdentityManager.getRegisteredProfessions().keySet());
                // The base profession can never become an item.
                suggestions.remove(BaseProfession.CODE);
                return suggestions.stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("base")) {
                if (!sender.hasPermission(Permissions.ADMIN)) {
                    return new ArrayList<>();
                }
                if (args[1].equalsIgnoreCase("add")) {
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
                return new ArrayList<>();
            }

            if (args[0].equalsIgnoreCase("get")) {
                return Arrays.asList("<texture_base64>");
            }

            if (args[0].equalsIgnoreCase("stats") && args[1].equalsIgnoreCase("add")) {
                return Arrays.asList("health", "energy", "strength", "defense", "wisdom", "luck", "agility", "resilience", "food").stream()
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 4) {
            if (args[0].equalsIgnoreCase("stats") && args[1].equalsIgnoreCase("add")) {
                return Arrays.asList("<amount>");
            }
            if (args[0].equalsIgnoreCase("base") && args[1].equalsIgnoreCase("add")) {
                if (!sender.hasPermission(Permissions.ADMIN)) {
                    return new ArrayList<>();
                }
                // The 4th argument is either a profession (specific form) or the
                // amount (random form). The player profession is never offered.
                List<String> suggestions = new ArrayList<>();
                suggestions.add("random");
                suggestions.add("<amount>");
                suggestions.addAll(MCIdentityManager.getRegisteredProfessions().keySet());
                suggestions.remove(BaseProfession.CODE);
                return suggestions.stream()
                        .filter(s -> s.startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }
            return new ArrayList<>();
        }

        if (args.length == 5) {
            if (args[0].equalsIgnoreCase("base") && args[1].equalsIgnoreCase("add")) {
                return Arrays.asList("<amount>");
            }
            return new ArrayList<>();
        }

        return new ArrayList<>();
    }
}

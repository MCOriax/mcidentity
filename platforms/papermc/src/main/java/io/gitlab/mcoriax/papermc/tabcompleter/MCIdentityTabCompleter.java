package io.gitlab.mcoriax.papermc.tabcompleter;

import io.gitlab.mcoriax.api.model.Ability;
import io.gitlab.mcoriax.api.model.Profession;
import io.gitlab.mcoriax.api.model.Rune;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
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
            return Arrays.asList("ability", "rune", "get", "check", "unuse", "help", "stats").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("stats")) {
                return Arrays.asList("get", "add").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("ability") || args[0].equalsIgnoreCase("rune")) {
                return Arrays.asList("get").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args[0].equalsIgnoreCase("get")) {
                List<String> suggestions = new ArrayList<>();
                suggestions.add("random");
                try {
                    Collection<Profession> professions = MCIdentityProvider.instance.getProfessions().join();
                    suggestions.addAll(professions.stream()
                            .map(Profession::getSystemCode)
                            .collect(Collectors.toList()));
                } catch (Exception ignored) {}
                return suggestions.stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("ability") 
                    && args[1].equalsIgnoreCase("get")) {
                List<String> suggestions = new ArrayList<>();
                try {
                    Collection<Ability> abilities = MCIdentityProvider.instance.getAbilities().join();
                    suggestions.addAll(abilities.stream()
                            .map(Ability::getSystemCode)
                            .collect(Collectors.toList()));
                } catch (Exception ignored) {}
                return suggestions.stream()
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args[0].equalsIgnoreCase("rune") 
                    && args[1].equalsIgnoreCase("get")) {
                List<String> suggestions = new ArrayList<>();
                try {
                    Collection<Rune> runes = MCIdentityProvider.instance.getRunes().join();
                    suggestions.addAll(runes.stream()
                            .map(Rune::getSystemCode)
                            .collect(Collectors.toList()));
                } catch (Exception ignored) {}
                return suggestions.stream()
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
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
            return new ArrayList<>();
        }

        return new ArrayList<>();
    }
}

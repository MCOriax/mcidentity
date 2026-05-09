package io.gitlab.mcoriax.foliamc.tabcompleter;

import io.gitlab.mcoriax.api.model.Profession;
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

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("ability", "create", "check", "unuse", "help").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("ability")) {
                return Arrays.asList("create").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args[0].equalsIgnoreCase("create")) {
                List<String> suggestions = new ArrayList<>();
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
                    && args[1].equalsIgnoreCase("create")) {
                return Arrays.asList("<system_code>");
            }

            if (args[0].equalsIgnoreCase("create")) {
                return Arrays.asList("<texture_base64>");
            }
        }

        if (args.length == 4) {
            if (args[0].equalsIgnoreCase("ability") 
                    && args[1].equalsIgnoreCase("create")) {
                return Arrays.asList("<display_name>");
            }
        }

        return new ArrayList<>();
    }
}

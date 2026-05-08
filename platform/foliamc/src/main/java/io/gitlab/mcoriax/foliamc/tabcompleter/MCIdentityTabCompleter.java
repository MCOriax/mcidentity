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
            return Arrays.asList("create", "check", "unuse", "help").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            List<String> suggestions = new ArrayList<>(Arrays.asList("profession", "ability"));
            
            // Also suggest existing professions for /identity create <profession> [texture]
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

        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            if (args[1].equalsIgnoreCase("profession") || args[1].equalsIgnoreCase("ability")) {
                // Suggesting placeholders for system code
                return Arrays.asList("<system_code>");
            }

            // Suggesting placeholders for texture
            return Arrays.asList("<texture_base64>");
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            if (args[1].equalsIgnoreCase("profession") || args[1].equalsIgnoreCase("ability")) {
                // Suggesting placeholders for display name
                return Arrays.asList("<display_name>");
            }
        }

        return new ArrayList<>();
    }
}

package io.gitlab.mcoriax.foliamc.tabcompleter;

import io.gitlab.mcoriax.api.model.Profession;
import io.gitlab.mcoriax.api.model.Sigil;
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
            List<String> suggestions = new ArrayList<>(Arrays.asList("sigil", "profession", "ability"));
            
            // Also suggest existing professions for /identity create <profession> <sigil>
            try {
                Collection<Profession> professions = MCIdentityProvider.getProfessions().join();
                suggestions.addAll(professions.stream()
                        .map(Profession::getSystemCode)
                        .collect(Collectors.toList()));
            } catch (Exception ignored) {}

            return suggestions.stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            if (args[1].equalsIgnoreCase("sigil")) {
                // No specific suggestions for new sigil names
                return new ArrayList<>();
            }
            if (args[1].equalsIgnoreCase("profession")) {
                // No specific suggestions for new profession names
                return new ArrayList<>();
            }
            if (args[1].equalsIgnoreCase("ability")) {
                // No specific suggestions for new ability names
                return new ArrayList<>();
            }

            // Suggestions for existing sigils to link to a new profession (OLD logic compatible)
            try {
                Collection<Sigil> sigils = MCIdentityProvider.getSigils().join();
                return sigils.stream()
                        .map(Sigil::getSystemCode)
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }

        return new ArrayList<>();
    }
}

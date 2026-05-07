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
            return Arrays.asList("create", "check").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            // No specific suggestions for new sigil/profession names
            return new ArrayList<>();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            // Suggestions for existing sigils to link to a new profession
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

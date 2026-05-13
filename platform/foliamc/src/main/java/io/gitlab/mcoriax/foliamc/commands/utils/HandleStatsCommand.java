package io.gitlab.mcoriax.foliamc.commands.utils;

import io.gitlab.mcoriax.api.model.ActiveIdentitySession;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

import java.util.Optional;

public class HandleStatsCommand {
    /**
     * Handle the /identity stats command.
     */
    public static void execute(MCIdentityPlugin plugin, Player player, String[] args) {
        Optional<ActiveIdentitySession> optSession = MCIdentityManager.getSession(player.getUniqueId());
        if (optSession.isEmpty()) {
            player.sendMessage(Component.text("You must have an active identity to use this command!", NamedTextColor.RED));
            return;
        }

        ActiveIdentitySession session = optSession.get();

        if (args.length == 1 || (args.length >= 2 && args[1].equalsIgnoreCase("get"))) {
            int points = session.properties().getStatPoints();
            player.sendMessage(Component.text("--- Identity Stats ---", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<gray>Remaining Stat Points: <yellow><points></yellow></gray>",
                    Placeholder.unparsed("points", String.valueOf(points))
            ));
            return;
        }

        if (args.length >= 4 && args[1].equalsIgnoreCase("add")) {
            String statType = args[2].toLowerCase();
            int amount;
            try {
                amount = Integer.parseInt(args[3]);
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid amount! Please enter a positive integer.", NamedTextColor.RED));
                return;
            }

            int currentPoints = session.properties().getStatPoints();
            if (currentPoints < amount) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<red>Not enough stat points! You only have <points> points.</red>",
                        Placeholder.unparsed("points", String.valueOf(currentPoints))
                ));
                return;
            }

            UpdateIdentityStats.execute(plugin, player, session, statType, amount);
            return;
        }

        player.sendMessage(Component.text("Usage: /identity stats <get|add <stat_type> <amount>>", NamedTextColor.RED));
    }
}

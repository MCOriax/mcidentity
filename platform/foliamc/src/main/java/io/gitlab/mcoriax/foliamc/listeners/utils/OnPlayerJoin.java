package io.gitlab.mcoriax.foliamc.listeners.utils;

import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import io.gitlab.mcoriax.foliamc.listeners.MCIdentityStatsListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

public class OnPlayerJoin {
    /**
     * Handle the player join event.
     */
    public static void execute(MCIdentityPlugin plugin, PlayerJoinEvent event) {
        Player player = event.getPlayer();
        MCIdentityProvider.instance.getSession(player.getUniqueId()).thenAccept(optSession -> {
            optSession.ifPresent(session -> {
                MCIdentityProvider.instance.loadSessionToRam(player.getUniqueId(), session.getIdentityUniqueId(), session.getContentKey()).thenRun(() -> {
                    player.getScheduler().run(plugin, task -> {
                        if (!plugin.isEnabled() || !player.isOnline()) return;

                        MCIdentityManager.getSession(player.getUniqueId()).ifPresent(s -> {
                            ((MCIdentityStatsListener) MCIdentityManager.getStatsListener()).updateVisuals(player, s);
                        });

                        player.sendMessage(Component.text("You are currently wearing an identity.", NamedTextColor.GREEN));
                    }, null);
                });
            });
        });
    }
}

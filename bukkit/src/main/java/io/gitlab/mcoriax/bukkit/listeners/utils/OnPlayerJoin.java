package io.gitlab.mcoriax.bukkit.listeners.utils;
import org.bukkit.ChatColor;
import io.gitlab.mcoriax.bukkit.scheduler.Schedulers;

import org.bukkit.Bukkit;

import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.gitlab.mcoriax.bukkit.listeners.MCIdentityStatsListener;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

public class OnPlayerJoin {
    /**
     * Handle the player join event.
     * @param plugin The plugin instance.
     * @param event The player join event.
     */
    public static void execute(JavaPlugin plugin, PlayerJoinEvent event) {
        Player player = event.getPlayer();
        MCIdentityProvider.instance.getSession(player.getUniqueId()).thenAccept(optSession -> {
            optSession.ifPresent(session -> {
                MCIdentityProvider.instance.loadSessionToRam(player.getUniqueId(), session.getIdentityUniqueId(), session.getContentKey()).thenRun(() -> {
                    Schedulers.forEntity(player).run(plugin, st -> {
                        MCIdentityManager.getSession(player.getUniqueId()).ifPresent(s -> {
                            ((MCIdentityStatsListener) MCIdentityManager.getStatsListener()).updateVisuals(player, s);
                        });

                        player.sendMessage(ChatColor.GREEN + "You are currently wearing an identity.");
                    }, null);
                });
            });
        });
    }
}

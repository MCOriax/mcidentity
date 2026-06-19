package io.github.mcoriax.bukkit.listeners.utils;

import io.github.mcoriax.common.MCIdentityManager;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.bukkit.listeners.MCIdentityStatsListener;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;

public class OnPlayerQuit {
    /**
     * Handle the player quit event.
     * @param plugin The plugin instance.
     * @param event The player quit event.
     */
    public static void execute(JavaPlugin plugin, PlayerQuitEvent event) {
        Player player = event.getPlayer();
        MCIdentityManager.removeSession(player.getUniqueId());
        ((MCIdentityStatsListener) MCIdentityManager.getStatsListener()).restoreVisuals(player);
    }
}

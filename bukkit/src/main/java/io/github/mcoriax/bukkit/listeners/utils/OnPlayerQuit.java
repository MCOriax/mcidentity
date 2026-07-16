package io.github.mcoriax.bukkit.listeners.utils;

import io.github.mcoriax.bukkit.util.PassiveExpService;
import io.github.mcoriax.common.MCIdentityManager;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.bukkit.listeners.MCIdentityStatsListener;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;

public class OnPlayerQuit {
    /**
     * Handle the player quit event. The player's buffered passive EXP is
     * flushed to the database before their session leaves RAM.
     * @param plugin The plugin instance.
     * @param event The player quit event.
     */
    public static void execute(JavaPlugin plugin, PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PassiveExpService.flush(player.getUniqueId());
        MCIdentityManager.removeSession(player.getUniqueId());
        MCIdentityManager.clearBaseIdentitySlots(player.getUniqueId());
        ((MCIdentityStatsListener) MCIdentityManager.getStatsListener()).restoreVisuals(player);
    }
}

package io.gitlab.mcoriax.foliamc.listeners.utils;

import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;

public class OnPlayerQuit {
    /**
     * Handle the player quit event.
     */
    public static void execute(MCIdentityPlugin plugin, PlayerQuitEvent event) {
        Player player = event.getPlayer();
        MCIdentityManager.removeSession(player.getUniqueId());
        plugin.getStatsListener().restoreVisuals(player);
    }
}

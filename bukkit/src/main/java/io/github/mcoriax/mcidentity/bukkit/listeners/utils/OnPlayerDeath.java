package io.github.mcoriax.mcidentity.bukkit.listeners.utils;

import io.github.mcoriax.mcidentity.bukkit.listeners.MCIdentityStatsListener;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Handles a player's death.
 *
 * <p>The worn profession is kept: a profession is never dropped, and the
 * vanilla inventory follows the server's own keep inventory rules. The fatal
 * blow left the tracked health at 0, so the current health, food and energy are
 * restored to their maximums for the respawn.</p>
 */
public final class OnPlayerDeath {

    private OnPlayerDeath() {
    }

    /**
     * Handles the player death event.
     * @param plugin The plugin instance.
     * @param event The player death event.
     */
    public static void execute(JavaPlugin plugin, PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerUuid = player.getUniqueId();
        MCIdentityManager.getSession(playerUuid).ifPresent(session -> {
            MCIdentityManager.updateHealth(playerUuid, session.stats().getHealth());
            MCIdentityManager.updateFood(playerUuid, session.stats().getFood());
            MCIdentityManager.updateEnergy(playerUuid, session.stats().getEnergy());
            if (MCIdentityManager.getStatsListener() instanceof MCIdentityStatsListener statsListener) {
                MCIdentityManager.getSession(playerUuid).ifPresent(refreshed ->
                        statsListener.updateVisuals(player, refreshed));
            }
        });
    }
}

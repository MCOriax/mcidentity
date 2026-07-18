package io.github.mcoriax.mcidentity.bukkit.listeners;

import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Restores the active identity's current health, food, and energy to their
 * maximum capacity when a player respawns.
 *
 * <p>The fatal blow leaves the identity's tracked current health at 0, and a
 * starving death leaves current food at 0. Those current values are runtime
 * only and are never persisted, so without this listener the zeros survive the
 * death into the respawn: the player comes back at 0% health and food and is
 * instantly critical again. Restoring them here fixes every identity uniformly
 * — the base "player" identity and its alternatives, which the death handler
 * intentionally keeps (and therefore never restores), as well as any regular
 * identity kept on death.</p>
 */
public class PlayerRespawnListener implements Listener {

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;

    /**
     * Creates a new PlayerRespawnListener.
     * @param plugin The plugin instance.
     */
    public PlayerRespawnListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Resets the respawning player's current health, food, and energy to the
     * active identity's maximums and refreshes the visuals.
     * @param event The player respawn event.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        MCIdentityManager.getSession(playerUuid).ifPresent(session -> {
            MCIdentityManager.updateHealth(playerUuid, session.stats().getHealth());
            MCIdentityManager.updateFood(playerUuid, session.stats().getFood());
            MCIdentityManager.updateEnergy(playerUuid, session.stats().getEnergy());

            // Refresh the visuals on the player's thread once they are alive
            // again, so the vanilla health and food bars reflect the restored
            // values immediately instead of waiting for the periodic visual
            // task. The live session is re-read there in case it changed.
            if (MCIdentityManager.getStatsListener() instanceof MCIdentityStatsListener statsListener) {
                Schedulers.forEntity(player).run(plugin, st ->
                    MCIdentityManager.getSession(playerUuid).ifPresent(refreshed ->
                        statsListener.updateVisuals(player, refreshed)), null);
            }
        });
    }
}

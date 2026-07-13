package io.github.mcoriax.bukkit.listeners.utils;
import org.bukkit.ChatColor;
import io.github.mcoriax.bukkit.scheduler.Schedulers;

import io.github.mcoriax.api.model.ActionType;
import io.github.mcoriax.bukkit.event.MCIdentityDeathEvent;
import io.github.mcoriax.bukkit.listeners.MCIdentityStatsListener;
import io.github.mcoriax.common.BaseProfession;
import io.github.mcoriax.common.MCIdentityManager;
import io.github.mcoriax.common.MCIdentityProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.bukkit.util.ItemStackSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class OnPlayerDeath {
    /**
     * Handle the player death event.
     * @param plugin The plugin instance.
     * @param event The player death event.
     */
    public static void execute(JavaPlugin plugin, PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLocation = player.getLocation().clone();
        UUID playerUuid = player.getUniqueId();

        // Default behaviour keeps the identity on death; only drop it when the
        // server opts in via config.yml. Read on the main thread here so the
        // async callbacks below capture a stable value.
        final boolean dropOnDeath = plugin.getConfig().getBoolean("drop_on_death", false);

        MCIdentityProvider.instance.getSession(playerUuid).thenAccept(optSession -> {
            optSession.ifPresent(session -> {
                UUID identityUuid = session.getIdentityUniqueId();
                String base64 = session.getContentKey();

                // The permanent base profession never drops on death.
                if (BaseProfession.isBaseIdentity(playerUuid, identityUuid)) {
                    return;
                }

                MCIdentityManager.clearSwapCooldown(playerUuid);

                Schedulers.async().runNow(plugin, stAsync -> {
                    try {
                        ItemStack identityItem = base64 != null ? ItemStackSerializer.deserialize(base64) : null;
                        Schedulers.forEntity(player).run(plugin, st1 -> {
                            MCIdentityDeathEvent deathEvent = new MCIdentityDeathEvent(player, identityUuid, identityItem);
                            deathEvent.setDrop(dropOnDeath);
                            Bukkit.getPluginManager().callEvent(deathEvent);

                            if (!deathEvent.isDrop()) {
                                // Identity is kept. The fatal blow left the
                                // identity's tracked health at 0, so restore
                                // current health, food, and energy to full for
                                // the respawn instead of leaving the visuals at
                                // 0%.
                                restoreCurrentStats(player, playerUuid);
                                return;
                            }

                            // Dropping: fall back to the base profession so the
                            // player is never left in the "no profession" state.
                            EquipBaseProfession.execute(plugin, player, null,
                                    ChatColor.YELLOW + "You are back to the base Player profession.", false);
                            Schedulers.async().runNow(plugin, stDrop -> {
                                if (identityItem != null) {
                                    Schedulers.forEntity(player).run(plugin, st2 -> {
                                        if (deathLocation.getWorld() != null) {
                                            try {
                                                deathLocation.getWorld().dropItemNaturally(deathLocation, identityItem);
                                                Player onlinePlayer = Bukkit.getPlayer(playerUuid);
                                                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                                                    onlinePlayer.sendMessage(ChatColor.RED + "Your identity has been dropped due to death!");
                                                }
                                            } catch (Exception e) {
                                                plugin.getLogger().severe("Failed to drop identity item for " + player.getName());
                                            }
                                        }
                                    }, null);
                                    MCIdentityProvider.instance.logAction(playerUuid, identityUuid, ActionType.DEATH_DROP);
                                }
                            });
                        }, null);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Failed to process death event for " + player.getName());
                        e.printStackTrace();
                    }
                });
            });
        });
    }

    /**
     * Restores the player's active identity current health, food, and energy
     * to their maximums and refreshes the visuals. Used when the identity is
     * kept on death so the player respawns at full instead of the 0% the fatal
     * blow left behind.
     * @param player The player who died.
     * @param playerUuid The UUID of the player.
     */
    private static void restoreCurrentStats(Player player, UUID playerUuid) {
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

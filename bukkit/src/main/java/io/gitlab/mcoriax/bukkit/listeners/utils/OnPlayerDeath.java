package io.gitlab.mcoriax.bukkit.listeners.utils;
import org.bukkit.ChatColor;
import io.gitlab.mcoriax.bukkit.scheduler.Schedulers;

import io.gitlab.mcoriax.api.model.ActionType;
import io.gitlab.mcoriax.bukkit.event.MCIdentityDeathEvent;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.gitlab.mcoriax.bukkit.listeners.MCIdentityStatsListener;
import io.gitlab.mcoriax.bukkit.util.ItemStackSerializer;
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

        MCIdentityProvider.instance.getSession(playerUuid).thenAccept(optSession -> {
            optSession.ifPresent(session -> {
                UUID identityUuid = session.getIdentityUniqueId();
                String base64 = session.getContentKey();

                MCIdentityManager.clearSwapCooldown(playerUuid);

                Schedulers.async().runNow(plugin, stAsync -> {
                    try {
                        ItemStack identityItem = base64 != null ? ItemStackSerializer.deserialize(base64) : null;
                        Schedulers.forEntity(player).run(plugin, st1 -> {
                            MCIdentityDeathEvent deathEvent = new MCIdentityDeathEvent(player, identityUuid, identityItem);
                            deathEvent.setDrop(true); 
                            Bukkit.getPluginManager().callEvent(deathEvent);

                            if (deathEvent.isDrop()) {
                                MCIdentityProvider.instance.setActiveIdentity(playerUuid, null, null).thenAccept(v -> {
                                    ((MCIdentityStatsListener) MCIdentityManager.getStatsListener()).restoreVisuals(player);
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
                            }
                        }, null);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Failed to process death event for " + player.getName());
                        e.printStackTrace();
                    }
                });
            });
        });
    }
}

package io.gitlab.mcoriax.foliamc.listeners.utils;

import io.gitlab.mcoriax.api.model.ActionType;
import io.gitlab.mcoriax.bukkit.event.MCIdentityDeathEvent;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import io.gitlab.mcoriax.foliamc.listeners.MCIdentityStatsListener;
import io.gitlab.mcoriax.foliamc.util.ItemStackSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    public static void execute(MCIdentityPlugin plugin, PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLocation = player.getLocation().clone();
        UUID playerUuid = player.getUniqueId();

        MCIdentityProvider.instance.getSession(playerUuid).thenAccept(optSession -> {
            optSession.ifPresent(session -> {
                UUID identityUuid = session.getIdentityUniqueId();
                String base64 = session.getContentKey();

                MCIdentityManager.clearSwapCooldown(playerUuid);

                try {
                    ItemStack identityItem = base64 != null ? ItemStackSerializer.deserialize(base64) : null;
                    
                    Bukkit.getRegionScheduler().run(plugin, deathLocation, task -> {
                        if (!plugin.isEnabled()) return;
                        MCIdentityDeathEvent deathEvent = new MCIdentityDeathEvent(player, identityUuid, identityItem);
                        deathEvent.setDrop(true); 
                        Bukkit.getPluginManager().callEvent(deathEvent);

                        if (deathEvent.isDrop()) {
                            MCIdentityProvider.instance.setActiveIdentity(playerUuid, null, null).thenAccept(v -> {
                                ((MCIdentityStatsListener) MCIdentityManager.getStatsListener()).restoreVisuals(player);
                                if (identityItem != null) {
                                    Bukkit.getRegionScheduler().run(plugin, deathLocation, dropTask -> {
                                        if (!plugin.isEnabled()) return;
                                        if (deathLocation.getWorld() != null) {
                                            deathLocation.getChunk().addPluginChunkTicket(plugin);
                                            try {
                                                deathLocation.getWorld().dropItemNaturally(deathLocation, identityItem);
                                                Player onlinePlayer = Bukkit.getPlayer(playerUuid);
                                                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                                                    onlinePlayer.sendMessage(Component.translatable("mcidentity.status.death_drop", "Your identity has been dropped due to death!").color(NamedTextColor.RED));
                                                }
                                            } finally {
                                                deathLocation.getChunk().removePluginChunkTicket(plugin);
                                            }
                                        }
                                    });
                                    MCIdentityProvider.instance.logAction(playerUuid, identityUuid, ActionType.DEATH_DROP);
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to process death event for " + player.getName());
                    e.printStackTrace();
                }
            });
        });
    }
}

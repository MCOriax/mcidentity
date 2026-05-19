package io.gitlab.mcoriax.papermc.listeners;

import org.bukkit.Bukkit;

import io.gitlab.mcoriax.api.model.ActiveIdentitySession;
import io.gitlab.mcoriax.bukkit.event.MCIdentityEquipEvent;
import io.gitlab.mcoriax.bukkit.event.MCIdentityPreEquipEvent;
import io.gitlab.mcoriax.bukkit.event.MCIdentityUnequipEvent;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.papermc.MCIdentityPlugin;
import io.gitlab.mcoriax.papermc.util.ItemStackSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

public class MCIdentityInventoryListener implements Listener {

    /**
     * The plugin instance.
     */
    private final MCIdentityPlugin plugin;

    /**
     * Creates a new MCIdentityInventoryListener.
     * @param plugin The plugin instance.
     */
    public MCIdentityInventoryListener(MCIdentityPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks if inventory synchronization is enabled in the config.
     * @return true if enabled, false otherwise.
     */
    private boolean isInventoryEnabled() {
        return plugin.getConfig().getBoolean("features.inventory", true);
    }

    /**
     * Checks if enderchest synchronization is enabled in the config.
     * @return true if enabled, false otherwise.
     */
    private boolean isEnderchestEnabled() {
        return plugin.getConfig().getBoolean("features.enderchest", true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onIdentityPreEquipCheck(MCIdentityPreEquipEvent event) {
        if (!isInventoryEnabled() && !isEnderchestEnabled()) return;

        Player player = event.getPlayer();
        
        Optional<ActiveIdentitySession> session = MCIdentityManager.getSession(player.getUniqueId());
        if (session.isPresent()) {
            return;
        }

        boolean hasItems = false;

        if (isInventoryEnabled()) {
            int totalItems = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && !item.getType().isAir()) {
                    totalItems++;
                }
            }
            if (event.isInteract() && totalItems > 1) {
                hasItems = true;
            } else if (!event.isInteract() && totalItems > 0) {
                hasItems = true;
            }
        }

        if (isEnderchestEnabled() && !hasItems) {
            for (ItemStack item : player.getEnderChest().getContents()) {
                if (item != null && !item.getType().isAir()) {
                    hasItems = true;
                    break;
                }
            }
        }

        if (hasItems) {
            event.setCancelled(true);
            event.setCancelReason(Component.translatable(
                    "mcidentity.inventory.error.not_empty", 
                    "You must empty your inventory and enderchest before equipping an identity!"
            ).color(NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIdentityEquip(MCIdentityEquipEvent event) {
        if (!isInventoryEnabled() && !isEnderchestEnabled()) return;

        Player player = event.getPlayer();
        UUID identityUuid = event.getIdentityUuid();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;

            MCIdentityProvider.instance.getProperties(identityUuid).thenAccept(optProps -> {
                if (optProps.isEmpty()) return;
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;

                    try {
                        if (isInventoryEnabled()) {
                            String invData = optProps.get().getInventoryContents();
                            if (invData != null && !invData.isEmpty()) {
                                ItemStack[] inv = ItemStackSerializer.deserializeArray(invData);
                                player.getInventory().setContents(inv);
                            } else {
                                player.getInventory().clear();
                            }
                        }

                        if (isEnderchestEnabled()) {
                            String ecData = optProps.get().getEnderchestContents();
                            if (ecData != null && !ecData.isEmpty()) {
                                ItemStack[] ec = ItemStackSerializer.deserializeArray(ecData);
                                player.getEnderChest().setContents(ec);
                            } else {
                                player.getEnderChest().clear();
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("Failed to load inventory/enderchest for identity " + identityUuid);
                        e.printStackTrace();
                    }
                });
            });
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onIdentityUnequip(MCIdentityUnequipEvent event) {
        if (!isInventoryEnabled() && !isEnderchestEnabled()) return;

        Player player = event.getPlayer();
        UUID identityUuid = event.getIdentityUuid();

        saveData(player, identityUuid);

        if (isInventoryEnabled()) {
            player.getInventory().clear();
        }
        if (isEnderchestEnabled()) {
            player.getEnderChest().clear();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!isInventoryEnabled() && !isEnderchestEnabled()) return;

        Player player = event.getPlayer();
        Optional<ActiveIdentitySession> session = MCIdentityManager.getSession(player.getUniqueId());

        session.ifPresent(activeSession -> saveData(player, activeSession.identityUuid()));
    }

    /**
     * Handles the data saving for a player's identity.
     * @param player The player.
     * @param identityUuid The identity UUID.
     */
    private void saveData(Player player, UUID identityUuid) {
        String invData = null;
        String ecData = null;

        if (isInventoryEnabled()) {
            try {
                invData = ItemStackSerializer.serializeArray(player.getInventory().getContents());
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to serialize inventory for identity " + identityUuid);
                e.printStackTrace();
            }
        }

        if (isEnderchestEnabled()) {
            try {
                ecData = ItemStackSerializer.serializeArray(player.getEnderChest().getContents());
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to serialize enderchest for identity " + identityUuid);
                e.printStackTrace();
            }
        }

        if (invData != null || ecData != null) {
            MCIdentityProvider.instance.updateIdentityStorage(identityUuid, invData, ecData);
        }
    }
}

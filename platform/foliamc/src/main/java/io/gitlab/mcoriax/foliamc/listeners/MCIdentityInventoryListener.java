package io.gitlab.mcoriax.foliamc.listeners;

import io.gitlab.mcoriax.api.model.ActiveIdentitySession;
import io.gitlab.mcoriax.bukkit.event.IdentityEquipEvent;
import io.gitlab.mcoriax.bukkit.event.IdentityUnequipEvent;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import io.gitlab.mcoriax.foliamc.util.ItemStackSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

public class MCIdentityInventoryListener implements Listener {

    private final MCIdentityPlugin plugin;

    public MCIdentityInventoryListener(MCIdentityPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isInventoryEnabled() {
        return plugin.getConfig().getBoolean("features.inventory", false);
    }

    private boolean isEnderchestEnabled() {
        return plugin.getConfig().getBoolean("features.enderchest", false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIdentityEquip(IdentityEquipEvent event) {
        if (!isInventoryEnabled() && !isEnderchestEnabled()) return;

        Player player = event.getPlayer();
        UUID identityUuid = event.getIdentityUuid();

        player.getScheduler().run(plugin, task -> {
            if (!player.isOnline()) return;

            // Load inventory and enderchest from database safely
            MCIdentityProvider.instance.getProperties(identityUuid).thenAccept(optProps -> {
                if (optProps.isEmpty()) return;
                
                player.getScheduler().run(plugin, innerTask -> {
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
                }, null);
            });
        }, null);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onIdentityUnequip(IdentityUnequipEvent event) {
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

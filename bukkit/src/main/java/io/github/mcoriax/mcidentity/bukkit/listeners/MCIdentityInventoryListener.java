package io.github.mcoriax.mcidentity.bukkit.listeners;
import org.bukkit.ChatColor;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;

import io.github.mcoriax.mcidentity.api.model.ActiveIdentitySession;
import io.github.mcoriax.mcidentity.bukkit.event.MCIdentityEquipEvent;
import io.github.mcoriax.mcidentity.bukkit.event.MCIdentityPreEquipEvent;
import io.github.mcoriax.mcidentity.bukkit.event.MCIdentityUnequipEvent;
import io.github.mcoriax.mcidentity.common.BaseProfession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.mcidentity.bukkit.util.ItemStackSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

/**
 * Keeps every identity's inventory and enderchest isolated. Isolation is a
 * core guarantee of the identity system and is always on for all identities.
 */
public class MCIdentityInventoryListener implements Listener {

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;

    /**
     * Creates a new MCIdentityInventoryListener.
     * @param plugin The plugin instance.
     */
    public MCIdentityInventoryListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onIdentityPreEquipCheck(MCIdentityPreEquipEvent event) {
        Player player = event.getPlayer();

        Optional<ActiveIdentitySession> session = MCIdentityManager.getSession(player.getUniqueId());
        if (session.isPresent()) {
            return;
        }

        boolean hasItems = false;

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

        if (!hasItems) {
            for (ItemStack item : player.getEnderChest().getContents()) {
                if (item != null && !item.getType().isAir()) {
                    hasItems = true;
                    break;
                }
            }
        }

        if (hasItems) {
            event.setCancelled(true);
            event.setCancelReason(ChatColor.RED + "You must empty your inventory and enderchest before equipping an identity!");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIdentityEquip(MCIdentityEquipEvent event) {
        Player player = event.getPlayer();
        UUID identityUuid = event.getIdentityUuid();

        // Base identity equips restore their storage in EquipBaseProfession,
        // sequenced with the returned identity item.
        if (BaseProfession.isBaseIdentity(player.getUniqueId(), identityUuid)) {
            return;
        }

        Schedulers.forEntity(player).run(plugin, st -> {
            MCIdentityProvider.instance.getProperties(identityUuid).thenAccept(optProps -> {
                if (optProps.isEmpty()) return;

                Schedulers.forEntity(player).run(plugin, stInner -> {
                    try {
                        String invData = optProps.get().getInventoryContents();
                        if (invData != null && !invData.isEmpty()) {
                            ItemStack[] inv = ItemStackSerializer.deserializeArray(invData);
                            player.getInventory().setContents(inv);
                        } else {
                            player.getInventory().clear();
                        }

                        String ecData = optProps.get().getEnderchestContents();
                        if (ecData != null && !ecData.isEmpty()) {
                            ItemStack[] ec = ItemStackSerializer.deserializeArray(ecData);
                            player.getEnderChest().setContents(ec);
                        } else {
                            player.getEnderChest().clear();
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
    public void onIdentityUnequip(MCIdentityUnequipEvent event) {
        Player player = event.getPlayer();
        UUID identityUuid = event.getIdentityUuid();

        saveData(player, identityUuid);

        player.getInventory().clear();
        player.getEnderChest().clear();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
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
        ItemStack[] invContents = player.getInventory().getContents().clone();
        ItemStack[] ecContents = player.getEnderChest().getContents().clone();

        Schedulers.async().runNow(plugin, scheduledTask -> {
            String invData = null;
            String ecData = null;

            try {
                invData = ItemStackSerializer.serializeArray(invContents);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to serialize inventory for identity " + identityUuid);
                e.printStackTrace();
            }

            try {
                ecData = ItemStackSerializer.serializeArray(ecContents);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to serialize enderchest for identity " + identityUuid);
                e.printStackTrace();
            }

            if (invData != null || ecData != null) {
                MCIdentityProvider.instance.updateIdentityStorage(identityUuid, invData, ecData);
            }
        });
    }
}

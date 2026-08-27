package io.github.mcoriax.mcidentity.bukkit.listeners;
import org.bukkit.ChatColor;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;

import io.github.mcoriax.mcidentity.api.model.ActiveIdentitySession;
import io.github.mcoriax.mcidentity.api.model.IdentityProperties;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
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
    /**
     * Rejects an equip attempt before it starts when the player is on cooldown
     * or already has one in flight.
     * @param event The pre-equip event being handled.
     */
    public void onIdentityPreEquipCheck(MCIdentityPreEquipEvent event) {
        Player player = event.getPlayer();

        Optional<ActiveIdentitySession> session = MCIdentityManager.getSession(player.getUniqueId());
        if (session.isPresent()) {
            return;
        }

        // An interact equip is made while holding the identity key, so that one
        // item does not count as a non-empty inventory.
        int allowedItems = event.isInteract() ? 1 : 0;
        boolean hasItems = countItems(player.getInventory().getContents()) > allowedItems
                || countItems(player.getEnderChest().getContents()) > 0;

        if (hasItems) {
            event.setCancelled(true);
            event.setCancelReason(ChatColor.RED + "You must empty your inventory and enderchest before equipping an identity!");
        }
    }

    /**
     * Counts the items actually occupying a storage's slots.
     * @param contents The storage contents, empty slots included.
     * @return How many slots hold a real item.
     */
    private static int countItems(ItemStack[] contents) {
        int total = 0;
        for (ItemStack item : contents) {
            if (item != null && !item.getType().isAir()) {
                total++;
            }
        }
        return total;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    /**
     * Swaps the player's inventory and ender chest in for the newly equipped
     * identity's own storage.
     * @param event The equip event being handled.
     */
    public void onIdentityEquip(MCIdentityEquipEvent event) {
        Player player = event.getPlayer();
        UUID identityUuid = event.getIdentityUuid();

        // Base identity equips restore their storage in EquipBaseProfession,
        // sequenced with the returned identity item.
        if (BaseProfession.isBaseIdentity(player.getUniqueId(), identityUuid)) {
            return;
        }

        Schedulers.forEntity(player).run(plugin, st ->
            MCIdentityProvider.instance.getProperties(identityUuid).thenAccept(optProps ->
                optProps.ifPresent(props ->
                    Schedulers.forEntity(player).run(plugin, stInner ->
                        loadStorage(player, identityUuid, props), null))), null);
    }

    /**
     * Replaces the player's inventory and ender chest with the identity's own,
     * clearing whichever of the two the identity has not stored yet.
     * @param player The player equipping.
     * @param identityUuid The identity being equipped.
     * @param props The identity's stored properties.
     */
    private void loadStorage(Player player, UUID identityUuid, IdentityProperties props) {
        try {
            restoreContents(player.getInventory(), props.getInventoryContents());
            restoreContents(player.getEnderChest(), props.getEnderchestContents());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load inventory/enderchest for identity " + identityUuid);
            e.printStackTrace();
        }
    }

    /**
     * Fills one storage from its serialized form, or empties it when there is none.
     * @param storage The inventory to fill.
     * @param data The serialized contents, or null when the identity has none.
     * @throws IOException When the stored contents cannot be read.
     */
    private static void restoreContents(Inventory storage, String data) throws IOException {
        if (data == null || data.isEmpty()) {
            storage.clear();
            return;
        }
        storage.setContents(ItemStackSerializer.deserializeArray(data));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    /**
     * Saves the unequipped identity's storage and restores the player's own.
     * @param event The unequip event being handled.
     */
    public void onIdentityUnequip(MCIdentityUnequipEvent event) {
        Player player = event.getPlayer();
        UUID identityUuid = event.getIdentityUuid();

        saveData(player, identityUuid);

        player.getInventory().clear();
        player.getEnderChest().clear();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    /**
     * Persists the active identity's storage before the player leaves.
     * @param event The quit event being handled.
     */
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

package io.github.mcoriax.mcidentity.bukkit.listeners.utils;
import org.bukkit.ChatColor;

import io.github.mcoriax.mcidentity.api.model.ActionType;
import io.github.mcoriax.mcidentity.api.model.ActiveIdentitySession;
import io.github.mcoriax.mcidentity.bukkit.event.MCIdentityPreEquipEvent;
import io.github.mcoriax.mcidentity.bukkit.event.MCIdentityUnequipEvent;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.mcidentity.bukkit.util.MessageComponents;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;
import java.util.UUID;

public class HandleSlot4Click {
    /**
     * Handle clicks in slot 4 of the identity GUI.
     * @param plugin The plugin instance.
     * @param event The inventory click event.
     * @param player The player who clicked.
     * @param identityKey The NamespacedKey for identity UUID.
     * @param professionKey The NamespacedKey for profession code.
     * @param unidentifiedKey The NamespacedKey for identification status.
     * @param textureKey The NamespacedKey for texture.
     */
    public static void execute(JavaPlugin plugin, InventoryClickEvent event, Player player, NamespacedKey identityKey, NamespacedKey professionKey, NamespacedKey unidentifiedKey, NamespacedKey textureKey) {
        event.setCancelled(true);

        SlotKeys keys = new SlotKeys(identityKey, professionKey, unidentifiedKey, textureKey);
        InventoryAction action = event.getAction();

        if (IsPlaceAction.execute(action)) {
            handlePlace(plugin, event, player, keys);
        } else if (IsTakeAction.execute(action)) {
            handleTake(plugin, event, player, keys);
        }
    }

    /**
     * The four persistent-data keys an identity item is stamped with.
     *
     * @param identityKey The NamespacedKey for identity UUID.
     * @param professionKey The NamespacedKey for profession code.
     * @param unidentifiedKey The NamespacedKey for identification status.
     * @param textureKey The NamespacedKey for texture.
     */
    private record SlotKeys(NamespacedKey identityKey, NamespacedKey professionKey,
                            NamespacedKey unidentifiedKey, NamespacedKey textureKey) {
    }

    /**
     * Handles an identity key being placed into the slot.
     * @param plugin The plugin instance.
     * @param event The inventory click event.
     * @param player The player who clicked.
     * @param keys The identity item's persistent-data keys.
     */
    private static void handlePlace(JavaPlugin plugin, InventoryClickEvent event, Player player, SlotKeys keys) {
        ItemStack cursorItem = event.getCursor();
        if (!IsIdentityKey.execute(cursorItem, keys.identityKey(), keys.professionKey(), keys.unidentifiedKey())) {
            if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                player.sendMessage(ChatColor.RED + "Only identity keys can be placed here!");
            }
            return;
        }

        if (!mayEquip(player, cursorItem)) {
            return;
        }

        event.setCancelled(true);
        MCIdentityManager.setEquipPending(player.getUniqueId(), true);

        ItemStack itemToEquip = cursorItem.clone();
        itemToEquip.setAmount(1);

        ItemMeta meta = itemToEquip.getItemMeta();
        String uuidStr = meta.getPersistentDataContainer().get(keys.identityKey(), PersistentDataType.STRING);
        String professionCode = meta.getPersistentDataContainer().get(keys.professionKey(), PersistentDataType.STRING);

        event.getWhoClicked().setItemOnCursor(null);

        if (uuidStr == null && professionCode != null) {
            FinalizeUnboundIdentity.execute(plugin, player, itemToEquip, professionCode, true,
                    keys.textureKey(), keys.professionKey(), keys.unidentifiedKey(), keys.identityKey());
            return;
        }

        UUID identityUuid;
        try {
            identityUuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            RefundItemSafely.execute(plugin, player, itemToEquip, ChatColor.RED + "This identity item is corrupted!");
            return;
        }

        InitializeIdentity.execute(plugin, player, identityUuid, professionCode, itemToEquip, true,
                keys.textureKey(), keys.professionKey(), keys.unidentifiedKey());
    }

    /**
     * Runs every gate that can refuse the equip, telling the player why.
     * @param player The player who clicked.
     * @param cursorItem The identity key on the cursor.
     * @return true when the equip may proceed.
     */
    private static boolean mayEquip(Player player, ItemStack cursorItem) {
        if (cursorItem.getAmount() > 1) {
            player.sendMessage(ChatColor.RED + "You cannot equip stacked identity keys!");
            return false;
        }

        // A base identity — the primary or an alternative of any
        // profession — is swapped out automatically; only a real
        // identity has to be unequipped by hand first.
        Optional<ActiveIdentitySession> current = MCIdentityManager.getSession(player.getUniqueId());
        if (current.isPresent() && !MCIdentityManager.isWearingBaseIdentity(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Please unequip your current identity first!");
            return false;
        }

        MCIdentityPreEquipEvent preEquipEvent = new MCIdentityPreEquipEvent(player, cursorItem, false);
        Bukkit.getPluginManager().callEvent(preEquipEvent);
        if (preEquipEvent.isCancelled()) {
            if (preEquipEvent.getCancelReason() != null) {
                player.sendMessage(preEquipEvent.getCancelReason());
            }
            return false;
        }

        return !onSwapCooldown(player) && !MCIdentityManager.isEquipPending(player.getUniqueId());
    }

    /**
     * Handles an identity key being taken back out of the slot.
     * @param plugin The plugin instance.
     * @param event The inventory click event.
     * @param player The player who clicked.
     * @param keys The identity item's persistent-data keys.
     */
    private static void handleTake(JavaPlugin plugin, InventoryClickEvent event, Player player, SlotKeys keys) {
        ItemStack currentItem = event.getCurrentItem();
        if (!IsIdentityKey.execute(currentItem, keys.identityKey(), keys.professionKey(), keys.unidentifiedKey())
                || onSwapCooldown(player)) {
            return;
        }

        ItemStack itemToTake = currentItem.clone();

        String uuidStr = itemToTake.getItemMeta().getPersistentDataContainer().get(keys.identityKey(), PersistentDataType.STRING);
        UUID identityUuid = UUID.fromString(uuidStr);

        MCIdentityUnequipEvent unequipEvent = new MCIdentityUnequipEvent(player, identityUuid);
        Bukkit.getPluginManager().callEvent(unequipEvent);

        if (unequipEvent.isCancelled()) {
            return;
        }

        // Fall back to the base profession; the taken identity item is
        // handed back once the base storage has been restored.
        MCIdentityManager.setSwapCooldown(player.getUniqueId());
        EquipBaseProfession.execute(plugin, player, itemToTake,
                ChatColor.YELLOW + "Identity unequipped. You are back to the base Player profession.", true);
        MCIdentityProvider.instance.logAction(player.getUniqueId(), identityUuid, ActionType.UNEQUIP);
    }

    /**
     * Reports whether the player is still on the swap cooldown, telling them
     * how long is left when they are.
     * @param player The player who clicked.
     * @return true when the swap must be refused.
     */
    private static boolean onSwapCooldown(Player player) {
        if (!MCIdentityManager.isOnSwapCooldown(player.getUniqueId())) {
            return false;
        }
        String remaining = MCIdentityManager.formatTime(MCIdentityManager.getRemainingSwapCooldownMillis(player.getUniqueId()));
        player.sendMessage(swapCooldownMessage(remaining));
        return true;
    }

    private static String swapCooldownMessage(String remaining) {
        return ChatColor.RED + MessageComponents.value(
                "mcidentity.command.error.swap_cooldown.prefix",
                "You must wait ",
                remaining,
                "mcidentity.command.error.swap_cooldown.suffix",
                " before swapping identities!"
        );
    }
}

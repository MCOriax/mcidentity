package io.github.mcoriax.bukkit.listeners.utils;
import org.bukkit.ChatColor;

import io.github.mcoriax.api.model.ActionType;
import io.github.mcoriax.api.model.ActiveIdentitySession;
import io.github.mcoriax.bukkit.event.MCIdentityPreEquipEvent;
import io.github.mcoriax.bukkit.event.MCIdentityUnequipEvent;
import io.github.mcoriax.common.BaseProfession;
import io.github.mcoriax.common.MCIdentityManager;
import io.github.mcoriax.common.MCIdentityProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.bukkit.util.MessageComponents;
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

        ItemStack cursorItem = event.getCursor();
        ItemStack currentItem = event.getCurrentItem();
        InventoryAction action = event.getAction();

        if (IsPlaceAction.execute(action)) {
            if (IsIdentityKey.execute(cursorItem, identityKey, professionKey, unidentifiedKey)) {
                
                if (cursorItem.getAmount() > 1) {
                    player.sendMessage(ChatColor.RED + "You cannot equip stacked identity keys!");
                    return;
                }

                // The base profession is swapped out automatically; only a
                // real identity has to be unequipped by hand first.
                Optional<ActiveIdentitySession> current = MCIdentityManager.getSession(player.getUniqueId());
                if (current.isPresent() && !BaseProfession.isBaseProfession(current.get().identityKey().getProfessionCode())) {
                    player.sendMessage(ChatColor.RED + "Please unequip your current identity first!");
                    return;
                }

                MCIdentityPreEquipEvent preEquipEvent = new MCIdentityPreEquipEvent(player, cursorItem, false);
                Bukkit.getPluginManager().callEvent(preEquipEvent);
                if (preEquipEvent.isCancelled()) {
                    if (preEquipEvent.getCancelReason() != null) {
                        player.sendMessage(preEquipEvent.getCancelReason());
                    }
                    return;
                }

                if (MCIdentityManager.isOnSwapCooldown(player.getUniqueId())) {
                    String remaining = MCIdentityManager.formatTime(MCIdentityManager.getRemainingSwapCooldownMillis(player.getUniqueId()));
                    player.sendMessage(swapCooldownMessage(remaining));
                    return;
                }

                if (MCIdentityManager.isEquipPending(player.getUniqueId())) {
                    return;
                }

                event.setCancelled(true);
                MCIdentityManager.setEquipPending(player.getUniqueId(), true);

                ItemStack itemToEquip = cursorItem.clone();
                itemToEquip.setAmount(1);
                
                ItemMeta meta = itemToEquip.getItemMeta();
                String uuidStr = meta.getPersistentDataContainer().get(identityKey, PersistentDataType.STRING);
                String professionCode = meta.getPersistentDataContainer().get(professionKey, PersistentDataType.STRING);

                event.getWhoClicked().setItemOnCursor(null);

                if (uuidStr == null && professionCode != null) {
                    FinalizeUnboundIdentity.execute(plugin, player, itemToEquip, professionCode, true, textureKey, professionKey, unidentifiedKey, identityKey);
                    return;
                }

                UUID identityUuid;
                try {
                    identityUuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException e) {
                    RefundItemSafely.execute(plugin, player, itemToEquip, ChatColor.RED + "This identity item is corrupted!");
                    return;
                }

                InitializeIdentity.execute(plugin, player, identityUuid, professionCode, itemToEquip, true, textureKey, professionKey, unidentifiedKey);
            } else if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                player.sendMessage(ChatColor.RED + "Only identity keys can be placed here!");
            }
        } else if (IsTakeAction.execute(action)) {
            if (IsIdentityKey.execute(currentItem, identityKey, professionKey, unidentifiedKey)) {
                if (MCIdentityManager.isOnSwapCooldown(player.getUniqueId())) {
                    String remaining = MCIdentityManager.formatTime(MCIdentityManager.getRemainingSwapCooldownMillis(player.getUniqueId()));
                    player.sendMessage(swapCooldownMessage(remaining));
                    return;
                }

                ItemStack itemToTake = currentItem.clone();
                
                String uuidStr = itemToTake.getItemMeta().getPersistentDataContainer().get(identityKey, PersistentDataType.STRING);
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
        }
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

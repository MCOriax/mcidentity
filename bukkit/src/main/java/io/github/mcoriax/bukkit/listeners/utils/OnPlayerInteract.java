package io.github.mcoriax.bukkit.listeners.utils;
import org.bukkit.ChatColor;

import io.github.mcoriax.bukkit.event.MCIdentityPreEquipEvent;
import io.github.mcoriax.common.MCIdentityManager;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.bukkit.util.MessageComponents;
import io.github.mcoriax.bukkit.util.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class OnPlayerInteract {
    /**
     * Handle the player interact event.
     * @param plugin The plugin instance.
     * @param event The player interact event.
     * @param identityKey The NamespacedKey for identity UUID.
     * @param professionKey The NamespacedKey for profession code.
     * @param textureKey The NamespacedKey for texture.
     * @param unidentifiedKey The NamespacedKey for identification status.
     */
    public static void execute(JavaPlugin plugin, PlayerInteractEvent event, NamespacedKey identityKey, NamespacedKey professionKey, NamespacedKey textureKey, NamespacedKey unidentifiedKey) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        String uuidStr = meta.getPersistentDataContainer().get(identityKey, PersistentDataType.STRING);
        String professionCode = meta.getPersistentDataContainer().get(professionKey, PersistentDataType.STRING);

        if (uuidStr == null && professionCode == null) {
            return;
        }

        if (item.getAmount() > 1) {
            player.sendMessage(ChatColor.RED + "You cannot equip stacked identity keys!");
            return;
        }

        MCIdentityPreEquipEvent preEquipEvent = new MCIdentityPreEquipEvent(player, item, true);
        Bukkit.getPluginManager().callEvent(preEquipEvent);
        if (preEquipEvent.isCancelled()) {
            if (preEquipEvent.getCancelReason() != null) {
                player.sendMessage(preEquipEvent.getCancelReason());
            }
            return;
        }

        if (MCIdentityManager.getSession(player.getUniqueId()).isPresent()) {
            player.sendMessage(ChatColor.RED + "You are already wearing an identity!");
            return;
        }

        if (MCIdentityManager.isOnSwapCooldown(player.getUniqueId())) {
            String remaining = MCIdentityManager.formatTime(MCIdentityManager.getRemainingSwapCooldownMillis(player.getUniqueId()));
            player.sendMessage(ChatColor.RED + MessageComponents.value(
                    "mcidentity.command.error.swap_cooldown.prefix",
                    "You must wait ",
                    remaining,
                    "mcidentity.command.error.swap_cooldown.suffix",
                    " before swapping identities!"
            ));
            return;
        }

        if (MCIdentityManager.isEquipPending(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        MCIdentityManager.setEquipPending(player.getUniqueId(), true);

        ItemStack itemToEquip = item.clone();
        itemToEquip.setAmount(1);

        // Dual wielding (EquipmentSlot / event.getHand()) only exists on 1.9+. Calling
        // getHand() on 1.8 throws NoSuchMethodError, so gate it behind ServerVersion.
        if (ServerVersion.hasOffhand()) {
            if (event.getHand() == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(null);
            } else if (event.getHand() == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(null);
            } else {
                player.sendMessage(ChatColor.RED + "Failed to verify item in hand.");
                MCIdentityManager.setEquipPending(player.getUniqueId(), false);
                return;
            }
        } else {
            // 1.8 has no off-hand; the identity key is always in the held main-hand slot.
            // setItem(int, ItemStack) and getHeldItemSlot() exist on every supported version.
            player.getInventory().setItem(player.getInventory().getHeldItemSlot(), null);
        }

        if (uuidStr == null && professionCode != null) {
            FinalizeUnboundIdentity.execute(plugin, player, itemToEquip, professionCode, false, textureKey, professionKey, unidentifiedKey, identityKey);
            return;
        }

        UUID identityUuid;
        try {
            identityUuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            RefundItemSafely.execute(plugin, player, itemToEquip, ChatColor.RED + "This identity item is corrupted!");
            return;
        }

        InitializeIdentity.execute(plugin, player, identityUuid, professionCode, itemToEquip, false, textureKey, professionKey, unidentifiedKey);
    }
}

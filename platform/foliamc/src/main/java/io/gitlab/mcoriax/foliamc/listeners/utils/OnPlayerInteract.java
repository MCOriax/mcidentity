package io.gitlab.mcoriax.foliamc.listeners.utils;

import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
     */
    public static void execute(MCIdentityPlugin plugin, PlayerInteractEvent event, NamespacedKey identityKey, NamespacedKey professionKey, NamespacedKey textureKey, NamespacedKey unidentifiedKey) {
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

        // Check if inventory is empty (allowing 1 for the identity item being held)
        int totalItems = 0;
        for (ItemStack invItem : player.getInventory().getContents()) {
            if (invItem != null && !invItem.getType().isAir()) {
                totalItems++;
            }
        }
        
        // If there is more than 1 item type (the 1 being the identity item currently held), inventory is not empty
        if (totalItems > 1) {
            player.sendMessage(Component.translatable("mcidentity.gui.error.inventory_not_empty", "Your inventory must be empty to equip an identity!").color(NamedTextColor.RED));
            return;
        }

        if (MCIdentityManager.getSession(player.getUniqueId()).isPresent()) {
            player.sendMessage(Component.translatable("mcidentity.gui.error.already_wearing", "You are already wearing an identity!").color(NamedTextColor.RED));
            return;
        }

        if (MCIdentityManager.isOnSwapCooldown(player.getUniqueId())) {
            String remaining = MCIdentityManager.formatTime(MCIdentityManager.getRemainingSwapCooldownMillis(player.getUniqueId()));
            player.sendMessage(Component.translatable("mcidentity.command.error.swap_cooldown", "You must wait " + remaining + " before swapping identities!", Component.text(remaining)).color(NamedTextColor.RED));
            return;
        }

        if (MCIdentityManager.isEquipPending(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        MCIdentityManager.setEquipPending(player.getUniqueId(), true);

        ItemStack itemToEquip = item.clone();
        itemToEquip.setAmount(1);

        if (event.getHand() == EquipmentSlot.HAND) {
            player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
        } else if (event.getHand() == EquipmentSlot.OFF_HAND) {
            player.getInventory().getItemInOffHand().setAmount(player.getInventory().getItemInOffHand().getAmount() - 1);
        } else {
            player.sendMessage(Component.translatable("mcidentity.gui.error.hand_verify_failed", "Failed to verify item in hand.").color(NamedTextColor.RED));
            MCIdentityManager.setEquipPending(player.getUniqueId(), false);
            return;
        }

        if (uuidStr == null && professionCode != null) {
            FinalizeUnboundIdentity.execute(plugin, player, itemToEquip, professionCode, false, textureKey, professionKey, unidentifiedKey, identityKey);
            return;
        }

        UUID identityUuid;
        try {
            identityUuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            RefundItemSafely.execute(plugin, player, itemToEquip, Component.translatable("mcidentity.gui.error.item_corrupted", "This identity item is corrupted!").color(NamedTextColor.RED));
            return;
        }

        InitializeIdentity.execute(plugin, player, identityUuid, professionCode, itemToEquip, false, textureKey, professionKey, unidentifiedKey);
    }
}

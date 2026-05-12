package io.gitlab.mcoriax.foliamc.listeners.utils;

import io.gitlab.mcoriax.api.model.ActionType;
import io.gitlab.mcoriax.bukkit.event.IdentityUnequipEvent;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import io.gitlab.mcoriax.foliamc.commands.MCIdentityCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;

public class HandleSlot4Click {
    /**
     * Handle clicks in slot 4 of the identity GUI.
     */
    public static void execute(MCIdentityPlugin plugin, InventoryClickEvent event, Player player, NamespacedKey identityKey, NamespacedKey professionKey, NamespacedKey unidentifiedKey, NamespacedKey textureKey) {
        event.setCancelled(true); 

        ItemStack cursorItem = event.getCursor();
        ItemStack currentItem = event.getCurrentItem();
        InventoryAction action = event.getAction();

        if (IsPlaceAction.execute(action)) {
            if (IsIdentityKey.execute(cursorItem, identityKey, professionKey, unidentifiedKey)) {
                if (MCIdentityManager.getSession(player.getUniqueId()).isPresent()) {
                    player.sendMessage(Component.text("Please unequip your current identity first!", NamedTextColor.RED));
                    return;
                }

                if (plugin.isOnSwapCooldown(player.getUniqueId())) {
                    String remaining = plugin.formatTime(plugin.getRemainingSwapCooldownMillis(player.getUniqueId()));
                    player.sendMessage(Component.text("You must wait " + remaining + " before swapping identities!", NamedTextColor.RED));
                    return;
                }

                if (plugin.isEquipPending(player.getUniqueId())) {
                    return;
                }

                event.setCancelled(true);
                plugin.setEquipPending(player.getUniqueId(), true);

                ItemStack itemToEquip = cursorItem.clone();
                itemToEquip.setAmount(1);
                
                ItemMeta meta = itemToEquip.getItemMeta();
                String uuidStr = meta.getPersistentDataContainer().get(identityKey, PersistentDataType.STRING);
                String professionCode = meta.getPersistentDataContainer().get(professionKey, PersistentDataType.STRING);

                if (cursorItem.getAmount() > 1) {
                    cursorItem.setAmount(cursorItem.getAmount() - 1);
                } else {
                    event.getWhoClicked().setItemOnCursor(null);
                }

                if (uuidStr == null && professionCode != null) {
                    FinalizeUnboundIdentity.execute(plugin, player, itemToEquip, professionCode, true, textureKey, professionKey, unidentifiedKey, identityKey);
                    return;
                }

                UUID identityUuid;
                try {
                    identityUuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException e) {
                    RefundItemSafely.execute(plugin, player, itemToEquip, "This identity item is corrupted!");
                    return;
                }

                InitializeIdentity.execute(plugin, player, identityUuid, professionCode, itemToEquip, true, textureKey, professionKey, unidentifiedKey);
            } else if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                player.sendMessage(Component.text("Only identity keys can be placed here!", NamedTextColor.RED));
            }
        } else if (IsTakeAction.execute(action)) {
            if (IsIdentityKey.execute(currentItem, identityKey, professionKey, unidentifiedKey)) {
                if (plugin.isOnSwapCooldown(player.getUniqueId())) {
                    String remaining = plugin.formatTime(plugin.getRemainingSwapCooldownMillis(player.getUniqueId()));
                    player.sendMessage(Component.text("You must wait " + remaining + " before swapping identities!", NamedTextColor.RED));
                    return;
                }

                ItemStack itemToTake = currentItem.clone();
                
                String uuidStr = itemToTake.getItemMeta().getPersistentDataContainer().get(identityKey, PersistentDataType.STRING);
                UUID identityUuid = UUID.fromString(uuidStr);

                IdentityUnequipEvent unequipEvent = new IdentityUnequipEvent(player, identityUuid);
                Bukkit.getPluginManager().callEvent(unequipEvent);

                if (unequipEvent.isCancelled()) {
                    return;
                }
                
                Map<Integer, ItemStack> leftOver = player.getInventory().addItem(itemToTake);
                leftOver.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                
                MCIdentityProvider.instance.setActiveIdentity(player.getUniqueId(), null, null).thenAccept(v -> {
                    plugin.setSwapCooldown(player.getUniqueId());
                    plugin.getStatsListener().restoreVisuals(player);
                    player.getScheduler().run(plugin, task -> {
                        if (!plugin.isEnabled() || !player.isOnline()) return;
                        player.sendMessage(Component.text("Identity unequipped via GUI.", NamedTextColor.YELLOW));
                        MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
                        if (cmd != null) cmd.openIdentityGui(player);
                    }, null);
                    MCIdentityProvider.instance.logAction(player.getUniqueId(), identityUuid, ActionType.UNEQUIP);
                });
            }
        }
    }
}

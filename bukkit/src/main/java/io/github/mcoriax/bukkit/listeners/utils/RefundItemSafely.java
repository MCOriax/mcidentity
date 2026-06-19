package io.github.mcoriax.bukkit.listeners.utils;
import io.github.mcoriax.bukkit.scheduler.Schedulers;

import io.github.mcoriax.common.MCIdentityManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RefundItemSafely {
    /**
     * Safely refund an item to a player and send an error message.
     * @param plugin The plugin instance.
     * @param player The player receiving the refund.
     * @param itemToEquip The item stack to refund.
     * @param message The error message to send.
     */
    public static void execute(JavaPlugin plugin, Player player, ItemStack itemToEquip, String message) {
        MCIdentityManager.setEquipPending(player.getUniqueId(), false);
        Schedulers.forEntity(player).run(plugin, st -> {
            if (!plugin.isEnabled() || !player.isOnline()) return;
            player.sendMessage(message);
            player.getInventory().addItem(itemToEquip).values()
                .forEach(remaining -> player.getWorld().dropItemNaturally(player.getLocation(), remaining));
        }, null);
    }
}

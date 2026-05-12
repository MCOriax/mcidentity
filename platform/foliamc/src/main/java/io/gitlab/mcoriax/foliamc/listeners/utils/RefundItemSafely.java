package io.gitlab.mcoriax.foliamc.listeners.utils;

import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RefundItemSafely {
    /**
     * Safely refund an item to a player and send an error message.
     */
    public static void execute(MCIdentityPlugin plugin, Player player, ItemStack itemToEquip, String message) {
        plugin.setEquipPending(player.getUniqueId(), false);
        player.getScheduler().run(plugin, task -> {
            if (!plugin.isEnabled() || !player.isOnline()) return;
            player.sendMessage(Component.text(message, NamedTextColor.RED));
            player.getInventory().addItem(itemToEquip).values()
                    .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }, null);
    }
}

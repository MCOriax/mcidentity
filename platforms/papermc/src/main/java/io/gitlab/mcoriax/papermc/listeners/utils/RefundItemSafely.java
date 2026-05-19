

import org.bukkit.Bukkit;

import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.papermc.MCIdentityPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    public static void execute(MCIdentityPlugin plugin, Player player, ItemStack itemToEquip, Component message) {
        MCIdentityManager.setEquipPending(player.getUniqueId(), false);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!plugin.isEnabled() || !player.isOnline()) return;
            player.sendMessage(message);
            player.getInventory().addItem(itemToEquip).values()
                    .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        });
    }
}


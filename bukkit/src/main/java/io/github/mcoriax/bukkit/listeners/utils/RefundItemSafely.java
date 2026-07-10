package io.github.mcoriax.bukkit.listeners.utils;
import io.github.mcoriax.bukkit.scheduler.Schedulers;

import io.github.mcoriax.api.model.ActiveIdentitySession;
import io.github.mcoriax.common.BaseProfession;
import io.github.mcoriax.common.MCIdentityManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public class RefundItemSafely {
    /**
     * Safely refund an item to a player and send an error message.
     * <p>
     * The base "player" profession is never represented as an item, so a base
     * profession item is never handed back. A player left with no session at
     * all falls back to the base profession so they are never without one.
     * </p>
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

            ItemStack refund = isBaseProfessionItem(plugin, itemToEquip) ? null : itemToEquip;

            Optional<ActiveIdentitySession> session = MCIdentityManager.getSession(player.getUniqueId());
            if (session.isEmpty()) {
                EquipBaseProfession.execute(plugin, player, refund, null, false);
            } else if (refund != null) {
                player.getInventory().addItem(refund).values()
                    .forEach(remaining -> player.getWorld().dropItemNaturally(player.getLocation(), remaining));
            }
        }, null);
    }

    /**
     * Checks whether an item claims the base profession code.
     * @param plugin The plugin instance.
     * @param item The item to inspect.
     * @return True when the item carries the base profession code.
     */
    private static boolean isBaseProfessionItem(JavaPlugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String code = item.getItemMeta().getPersistentDataContainer()
            .get(new NamespacedKey(plugin, "profession_code"), PersistentDataType.STRING);
        return code != null && BaseProfession.isBaseProfession(code);
    }
}

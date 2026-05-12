package io.gitlab.mcoriax.foliamc.listeners.utils;

import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class OnBlockPlace {
    /**
     * Handle the block place event.
     */
    public static void execute(MCIdentityPlugin plugin, BlockPlaceEvent event, NamespacedKey identityKey, NamespacedKey professionKey, NamespacedKey unidentifiedKey) {
        ItemStack item = event.getItemInHand();
        if (IsIdentityKey.execute(item, identityKey, professionKey, unidentifiedKey)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("You cannot place the Identity Key as a block!", NamedTextColor.RED));
        }
    }
}

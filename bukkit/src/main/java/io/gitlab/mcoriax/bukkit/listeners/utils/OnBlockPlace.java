package io.gitlab.mcoriax.bukkit.listeners.utils;

import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class OnBlockPlace {
    /**
     * Handle the block place event.
     * @param plugin The plugin instance.
     * @param event The block place event.
     * @param identityKey The NamespacedKey for identity UUID.
     * @param professionKey The NamespacedKey for profession code.
     * @param unidentifiedKey The NamespacedKey for identification status.
     */
    public static void execute(JavaPlugin plugin, BlockPlaceEvent event, NamespacedKey identityKey, NamespacedKey professionKey, NamespacedKey unidentifiedKey) {
        ItemStack item = event.getItemInHand();
        if (IsIdentityKey.execute(item, identityKey, professionKey, unidentifiedKey)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.translatable("mcidentity.gui.error.cannot_place_key", "You cannot place the Identity Key as a block!").color(NamedTextColor.RED));
        }
    }
}

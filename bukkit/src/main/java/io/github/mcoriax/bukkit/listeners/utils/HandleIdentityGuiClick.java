package io.github.mcoriax.bukkit.listeners.utils;
import org.bukkit.ChatColor;

import io.github.mcoriax.common.MCIdentityManager;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.bukkit.commands.MCIdentityCommand;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;

public class HandleIdentityGuiClick {
    /**
     * Handle clicks in the identity GUI.
     * @param plugin The plugin instance.
     * @param event The inventory click event.
     * @param identityKey The NamespacedKey for identity UUID.
     * @param professionKey The NamespacedKey for profession code.
     * @param unidentifiedKey The NamespacedKey for identification status.
     * @param textureKey The NamespacedKey for texture.
     */
    public static void execute(JavaPlugin plugin, InventoryClickEvent event, NamespacedKey identityKey, NamespacedKey professionKey, NamespacedKey unidentifiedKey, NamespacedKey textureKey) {
        Player player = (Player) event.getWhoClicked();
        InventoryAction action = event.getAction();
        int slot = event.getRawSlot();

        if (slot >= 0 && slot < event.getView().getTopInventory().getSize()) {
            event.setCancelled(true);

            if (slot == 6) {
                if (MCIdentityManager.getSession(player.getUniqueId()).isEmpty()) {
                    player.sendMessage(ChatColor.RED + "You must be wearing an identity to open the Rune GUI!");
                    return;
                }
                MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
                if (cmd != null) {
                    cmd.openIdentityRuneGui(player);
                }
                return;
            }

            if (slot == 2) {
                if (MCIdentityManager.getSession(player.getUniqueId()).isEmpty()) {
                    player.sendMessage(ChatColor.RED + "You must be wearing an identity to open the Ability GUI!");
                    return;
                }
                MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
                if (cmd != null) {
                    cmd.openIdentityAbilityGui(player);
                }
                return;
            }

            if (slot == 4) {
                HandleSlot4Click.execute(plugin, event, player, identityKey, professionKey, unidentifiedKey, textureKey);
                return;
            }

            if (slot == 8) {
                if (MCIdentityManager.getSession(player.getUniqueId()).isEmpty()) {
                    player.sendMessage(ChatColor.RED + "You must be wearing an identity to open the Cosmetic GUI!");
                    return;
                }
                MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
                if (cmd != null) {
                    cmd.openCosmeticGui(player);
                }
                return;
            }
        } else {
            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
            }
        }
    }
}

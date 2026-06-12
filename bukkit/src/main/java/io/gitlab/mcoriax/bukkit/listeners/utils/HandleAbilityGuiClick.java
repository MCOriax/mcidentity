package io.gitlab.mcoriax.bukkit.listeners.utils;

import org.bukkit.plugin.java.JavaPlugin;
import io.gitlab.mcoriax.bukkit.commands.MCIdentityCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class HandleAbilityGuiClick {
    /**
     * Handle clicks in the ability GUI.
     * @param plugin The plugin instance.
     * @param event The inventory click event.
     */
    public static void execute(JavaPlugin plugin, InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == 45) {
            MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
            if (cmd != null) {
                cmd.openIdentityGui(player);
            }
        }
    }
}

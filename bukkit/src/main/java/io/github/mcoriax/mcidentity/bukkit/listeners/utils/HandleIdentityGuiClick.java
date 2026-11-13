package io.github.mcoriax.mcidentity.bukkit.listeners.utils;

import io.github.mcoriax.mcidentity.bukkit.commands.utils.OpenIdentityGui;
import io.github.mcoriax.mcidentity.bukkit.commands.utils.OpenProfessionGui;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles clicks in the main identity GUI.
 */
public final class HandleIdentityGuiClick {

    private HandleIdentityGuiClick() {
    }

    /**
     * Handles one click: slot 0 opens the profession list; every other slot is
     * display only.
     * @param plugin The plugin instance.
     * @param event The inventory click event.
     */
    public static void execute(JavaPlugin plugin, InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot >= 0 && slot < event.getView().getTopInventory().getSize()) {
            event.setCancelled(true);
            if (slot == OpenIdentityGui.PROFESSIONS_SLOT) {
                OpenProfessionGui.execute(plugin, player);
            }
        } else if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
        }
    }
}

package io.github.mcoriax.bukkit.listeners.utils;

import io.github.mcoriax.bukkit.commands.MCIdentityCommand;
import io.github.mcoriax.bukkit.commands.utils.OpenPlayerIdentityGui;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles clicks in the paginated "Player Identity" GUI. Every slot is
 * display-only except the pagination arrows, which re-open the GUI at the
 * neighbouring page, and the primary identity head at the control-row center,
 * which returns to the main Identity GUI. The current page is parsed from the
 * inventory title.
 */
public class HandlePlayerIdentityGuiClick {

    /**
     * Handle clicks in the Player Identity GUI.
     * @param plugin The plugin instance.
     * @param event The inventory click event.
     */
    public static void execute(JavaPlugin plugin, InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        int size = event.getView().getTopInventory().getSize();

        if (slot < 0 || slot >= size) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
            }
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) {
            return;
        }

        int controlRowStart = size - 9;

        // The primary identity head at the control-row center returns to the
        // main Identity GUI.
        if (slot == controlRowStart + 4 && clicked.getType() == Material.PLAYER_HEAD) {
            MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
            if (cmd != null) {
                cmd.openIdentityGui(player);
            }
            return;
        }

        if (clicked.getType() != Material.ARROW || !clicked.hasItemMeta()) {
            return;
        }

        int page = parsePage(event.getView().getTitle());
        if (page < 1) {
            return;
        }

        if (slot == controlRowStart + 3) {
            OpenPlayerIdentityGui.execute(plugin, player, page - 1);
        } else if (slot == controlRowStart + 5) {
            OpenPlayerIdentityGui.execute(plugin, player, page + 1);
        }
    }

    /**
     * Extracts the 1-based page number from the GUI title.
     * @param title The inventory view title.
     * @return The page number, or -1 when the title does not carry one.
     */
    private static int parsePage(String title) {
        if (!title.startsWith(OpenPlayerIdentityGui.TITLE_PREFIX)) {
            return -1;
        }
        try {
            return Integer.parseInt(title.substring(OpenPlayerIdentityGui.TITLE_PREFIX.length()).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}

package io.github.mcoriax.mcidentity.bukkit.listeners.utils;

import io.github.mcoriax.mcidentity.bukkit.commands.utils.OpenIdentityGui;
import io.github.mcoriax.mcidentity.bukkit.commands.utils.OpenProfessionGui;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Routes inventory clicks to the plugin's own GUIs.
 */
public final class OnInventoryClick {

    private OnInventoryClick() {
    }

    /**
     * Handles the inventory click event.
     * @param plugin The plugin instance.
     * @param event The inventory click event.
     */
    public static void execute(JavaPlugin plugin, InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (OpenIdentityGui.TITLE.equals(title)) {
            HandleIdentityGuiClick.execute(plugin, event);
        } else if (OpenProfessionGui.TITLE.equals(title)) {
            HandleProfessionGuiClick.execute(plugin, event);
        }
    }
}

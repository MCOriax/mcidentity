package io.github.mcoriax.bukkit.listeners.utils;

import io.github.mcoriax.bukkit.commands.utils.OpenPlayerIdentityGui;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.InventoryClickEvent;

public class OnInventoryClick {
    /**
     * Handle the inventory click event.
     * @param plugin The plugin instance.
     * @param event The inventory click event.
     * @param identityKey The NamespacedKey for identity UUID.
     * @param professionKey The NamespacedKey for profession code.
     * @param unidentifiedKey The NamespacedKey for identification status.
     * @param textureKey The NamespacedKey for texture.
     */
    public static void execute(JavaPlugin plugin, InventoryClickEvent event, NamespacedKey identityKey, NamespacedKey professionKey, NamespacedKey unidentifiedKey, NamespacedKey textureKey) {
        String title = event.getView().getTitle();
        if ("Identity GUI".equals(title)) {
            HandleIdentityGuiClick.execute(plugin, event, identityKey, professionKey, unidentifiedKey, textureKey);
        } else if (title.startsWith(OpenPlayerIdentityGui.TITLE_PREFIX)) {
            HandlePlayerIdentityGuiClick.execute(plugin, event);
        } else if ("Rune GUI".equals(title)) {
            HandleRuneGuiClick.execute(plugin, event);
        } else if ("Ability GUI".equals(title)) {
            HandleAbilityGuiClick.execute(plugin, event);
        } else if ("Cosmetic GUI".equals(title)) {
            HandleCosmeticGuiClick.execute(plugin, event);
        }
    }
}

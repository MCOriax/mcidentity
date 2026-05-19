package io.gitlab.mcoriax.papermc.listeners.utils;

import io.gitlab.mcoriax.papermc.MCIdentityPlugin;
import net.kyori.adventure.text.Component;
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
    public static void execute(MCIdentityPlugin plugin, InventoryClickEvent event, NamespacedKey identityKey, NamespacedKey professionKey, NamespacedKey unidentifiedKey, NamespacedKey textureKey) {
        Component title = event.getView().title();
        if (title.equals(Component.translatable("mcidentity.gui.title", "Identity GUI"))) {
            HandleIdentityGuiClick.execute(plugin, event, identityKey, professionKey, unidentifiedKey, textureKey);
        } else if (title.equals(Component.translatable("mcidentity.gui.rune.title", "Rune GUI"))) {
            HandleRuneGuiClick.execute(plugin, event);
        } else if (title.equals(Component.translatable("mcidentity.gui.ability.title", "Ability GUI"))) {
            HandleAbilityGuiClick.execute(plugin, event);
        } else if (title.equals(Component.translatable("mcidentity.gui.cosmetic.title", "Cosmetic GUI"))) {
            HandleCosmeticGuiClick.execute(plugin, event);
        }
    }
}

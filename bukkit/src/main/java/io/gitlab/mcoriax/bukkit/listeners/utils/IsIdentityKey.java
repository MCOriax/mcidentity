package io.gitlab.mcoriax.bukkit.listeners.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class IsIdentityKey {
    /**
     * Check if an item is an identity key.
     * @param item The item stack to check.
     * @param identityKey The NamespacedKey for identity UUID.
     * @param professionKey The NamespacedKey for profession code.
     * @param unidentifiedKey The NamespacedKey for identification status.
     * @return true if it is an identity key, false otherwise.
     */
    public static boolean execute(ItemStack item, NamespacedKey identityKey, NamespacedKey professionKey, NamespacedKey unidentifiedKey) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(identityKey, PersistentDataType.STRING) || 
               meta.getPersistentDataContainer().has(professionKey, PersistentDataType.STRING) ||
               meta.getPersistentDataContainer().has(unidentifiedKey, PersistentDataType.BYTE);
    }
}

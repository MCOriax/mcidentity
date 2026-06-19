package io.github.mcoriax.bukkit.commands.utils;

import com.cryptomorin.xseries.XMaterial;
import io.github.mcoriax.bukkit.util.SkullTextures;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class CreateCustomHead {
    /**
     * Create a custom player head item stack.
     * @param texture The texture string (Base64).
     * @param translationKey The translation key for the item name (unused; retained for compatibility).
     * @param fallback The fallback item name.
     * @return The created ItemStack.
     */
    public static ItemStack execute(String texture, String translationKey, String fallback) {
        ItemStack head = XMaterial.PLAYER_HEAD.parseItem();
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(fallback);
            SkullTextures.apply(meta, texture);
            head.setItemMeta(meta);
        }
        return head;
    }
}

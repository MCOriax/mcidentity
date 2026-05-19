package io.gitlab.mcoriax.foliamc.commands.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class CreateCustomHead {
    /**
     * Create a custom player head item stack.
     * @param texture The texture string (Base64).
     * @param translationKey The translation key for the item name.
     * @param fallback The fallback item name.
     * @return The created ItemStack.
     */
    public static ItemStack execute(String texture, String translationKey, String fallback) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.translatable(translationKey, fallback).decoration(TextDecoration.ITALIC, false));
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", texture));
            meta.setPlayerProfile(profile);
            head.setItemMeta(meta);
        }
        return head;
    }
}

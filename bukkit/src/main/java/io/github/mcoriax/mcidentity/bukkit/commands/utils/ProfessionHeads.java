package io.github.mcoriax.mcidentity.bukkit.commands.utils;

import io.github.mcoriax.mcidentity.bukkit.util.SkullTextures;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Builds the head item that stands for a profession in the GUIs.
 */
public final class ProfessionHeads {

    private ProfessionHeads() {
    }

    /**
     * Builds a head for a profession: its first configured skin, or the
     * player's own head when the profession declares none.
     * @param player The player viewing the GUI.
     * @param professionCode The profession's code.
     * @param name The display name to give the item.
     * @param lore The lore lines.
     * @return The head item.
     */
    public static ItemStack build(Player player, String professionCode, String name, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (head.getItemMeta() instanceof SkullMeta meta) {
            String texture = firstSkin(professionCode);
            if (texture != null) {
                SkullTextures.apply(meta, texture);
            } else {
                meta.setOwningPlayer(player);
            }
            meta.setDisplayName(name);
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    /**
     * Picks the skin configured for the lowest level of a profession.
     * @param professionCode The profession's code.
     * @return The Base64 texture, or null when none is configured.
     */
    private static String firstSkin(String professionCode) {
        Map<Integer, String> skins = MCIdentityManager.getProfessionSkins(professionCode);
        if (skins == null || skins.isEmpty()) {
            return null;
        }
        return skins.entrySet().stream()
            .min(Map.Entry.comparingByKey(Comparator.naturalOrder()))
            .map(Map.Entry::getValue)
            .orElse(null);
    }
}

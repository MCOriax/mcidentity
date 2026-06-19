package io.github.mcoriax.bukkit.commands.utils;
import io.github.mcoriax.bukkit.scheduler.Schedulers;

import io.github.mcoriax.api.model.Profession;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.bukkit.util.HeadTexture;
import io.github.mcoriax.bukkit.util.SkullTextures;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

public class CreateIdentityItem {
    /**
     * Create an unidentified identity item for a specific profession and give it to the player.
     * @param plugin The plugin instance.
     * @param player The player to give the item to.
     * @param profession The profession for the identity.
     * @param customTexture The custom texture (optional).
     * @param textureKey The NamespacedKey for texture.
     * @param professionKey The NamespacedKey for profession code.
     * @param unidentifiedKey The NamespacedKey for identification status.
     */
    public static void execute(JavaPlugin plugin, Player player, Profession profession, String customTexture, NamespacedKey textureKey, NamespacedKey professionKey, NamespacedKey unidentifiedKey) {
        String professionCode = profession.getSystemCode();

        String activeTexture = customTexture;
        if (activeTexture == null) {
            activeTexture = HeadTexture.getUnidentified(plugin);
        }
        final String resolvedTexture = activeTexture;

        Schedulers.forEntity(player).run(plugin, st -> {
            ItemStack item = XMaterial.PLAYER_HEAD.parseItem();
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta != null) {
                if (resolvedTexture != null) {
                    SkullTextures.apply(meta, resolvedTexture);
                    meta.getPersistentDataContainer().set(textureKey, PersistentDataType.STRING, resolvedTexture);
                }

                meta.setDisplayName(ChatColor.GRAY + "[" + ChatColor.WHITE + ChatColor.MAGIC + "Unidentified Identity" + ChatColor.GRAY + "]");

                meta.getPersistentDataContainer().set(professionKey, PersistentDataType.STRING, professionCode);
                meta.getPersistentDataContainer().set(unidentifiedKey, PersistentDataType.BYTE, (byte) 1);
                item.setItemMeta(meta);
            }

            player.getInventory().addItem(item);
            player.sendMessage(ChatColor.GREEN + "Unbound Identity item given!");
        }, null);
    }
}

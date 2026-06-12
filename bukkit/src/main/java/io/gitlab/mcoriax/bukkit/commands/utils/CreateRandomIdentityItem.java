package io.gitlab.mcoriax.bukkit.commands.utils;
import io.gitlab.mcoriax.bukkit.scheduler.Schedulers;

import org.bukkit.plugin.java.JavaPlugin;
import io.gitlab.mcoriax.bukkit.util.HeadTexture;
import io.gitlab.mcoriax.bukkit.util.SkullTextures;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

public class CreateRandomIdentityItem {
    /**
     * Create a random unidentified identity item and give it to the player.
     * @param plugin The plugin instance.
     * @param player The player to give the item to.
     * @param customTexture The custom texture (optional).
     * @param professionKey The NamespacedKey for profession code.
     * @param unidentifiedKey The NamespacedKey for identification status.
     */
    public static void execute(JavaPlugin plugin, Player player, String customTexture, NamespacedKey professionKey, NamespacedKey unidentifiedKey) {
        Schedulers.forEntity(player).run(plugin, st -> {
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta != null) {
                String activeTexture = customTexture != null ? customTexture : HeadTexture.getUnidentified(plugin);

                SkullTextures.apply(meta, activeTexture);

                meta.setDisplayName(ChatColor.GRAY + "[" + ChatColor.WHITE + ChatColor.MAGIC + "Unidentified Identity" + ChatColor.GRAY + "]");
                
                meta.getPersistentDataContainer().set(professionKey, PersistentDataType.STRING, "random");
                meta.getPersistentDataContainer().set(unidentifiedKey, PersistentDataType.BYTE, (byte) 1);
                item.setItemMeta(meta);
            }

            player.getInventory().addItem(item);
            player.sendMessage(ChatColor.GREEN + "Random Identity item given!");
        }, null);
    }
}

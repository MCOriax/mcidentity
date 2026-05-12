package io.gitlab.mcoriax.foliamc.commands.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import io.gitlab.mcoriax.foliamc.util.HeadTexture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class CreateRandomIdentityItem {
    /**
     * Create a random unidentified identity item and give it to the player.
     */
    public static void execute(MCIdentityPlugin plugin, Player player, String customTexture, NamespacedKey professionKey, NamespacedKey unidentifiedKey) {
        player.getScheduler().run(plugin, task -> {
            if (!plugin.isEnabled() || !player.isOnline()) return;

            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta != null) {
                String activeTexture = customTexture != null ? customTexture : HeadTexture.getUnidentified(plugin);
                
                PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
                profile.setProperty(new ProfileProperty("textures", activeTexture));
                meta.setPlayerProfile(profile);

                meta.displayName(
                    Component.text("[", NamedTextColor.GRAY)
                        .append(Component.text("Unidentified Identity", NamedTextColor.WHITE).decoration(TextDecoration.OBFUSCATED, true))
                        .append(Component.text("]", NamedTextColor.GRAY))
                        .decoration(TextDecoration.ITALIC, false)
                );
                
                meta.getPersistentDataContainer().set(professionKey, PersistentDataType.STRING, "random");
                meta.getPersistentDataContainer().set(unidentifiedKey, PersistentDataType.BYTE, (byte) 1);
                item.setItemMeta(meta);
            }

            player.getInventory().addItem(item);
            player.sendMessage(Component.text("Random Identity item given!", NamedTextColor.GREEN));
        }, null);
    }
}

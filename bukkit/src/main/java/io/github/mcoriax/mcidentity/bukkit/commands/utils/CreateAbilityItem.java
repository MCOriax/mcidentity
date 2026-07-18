package io.github.mcoriax.mcidentity.bukkit.commands.utils;

import io.github.mcoriax.mcidentity.bukkit.util.SkullTextures;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Builds ability items for {@code /identity ability create}.
 *
 * <p>An ability item is a player head carrying the ability code (the
 * MythicMobs ability ID) and the ability skin (the model ID) in its
 * persistent data container, so equipping it through the ability GUI knows
 * exactly which MythicMobs ability and skin to store.</p>
 */
public class CreateAbilityItem {

    /**
     * Persistent data key holding the ability code.
     */
    public static final String ABILITY_CODE_KEY = "ability_code";

    /**
     * Persistent data key holding the ability skin.
     */
    public static final String ABILITY_SKIN_KEY = "ability_skin";

    /**
     * Sentinel argument meaning "no custom model data".
     */
    private static final String NO_CUSTOM_MODEL = "null";

    /**
     * Creates the ability item and gives it to the player.
     * @param plugin The plugin instance owning the persistent data keys.
     * @param player The player receiving the item.
     * @param abilityCode The ability code, mapping to the MythicMobs ability ID.
     * @param abilitySkin The ability skin, mapping to the model ID.
     * @param headTextureBase64 The Base64 profile-property texture applied to the head.
     * @param customModelId The CustomModelData to set, or null/"null" to keep the default.
     */
    public static void execute(JavaPlugin plugin, Player player, String abilityCode, String abilitySkin,
                               String headTextureBase64, String customModelId) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            player.sendMessage(ChatColor.RED + "Failed to create the ability item!");
            return;
        }

        if (meta instanceof SkullMeta skullMeta) {
            SkullTextures.apply(skullMeta, headTextureBase64);
        }

        meta.setDisplayName(ChatColor.AQUA + "Ability: " + ChatColor.WHITE + abilityCode);

        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, ABILITY_CODE_KEY), PersistentDataType.STRING, abilityCode);
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, ABILITY_SKIN_KEY), PersistentDataType.STRING, abilitySkin);

        if (customModelId != null && !customModelId.equalsIgnoreCase(NO_CUSTOM_MODEL)) {
            try {
                meta.setCustomModelData(Integer.parseInt(customModelId.trim()));
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Custom model id must be a number or \"null\": " + customModelId);
                return;
            }
        }

        item.setItemMeta(meta);
        player.getInventory().addItem(item);
        player.sendMessage(ChatColor.GREEN + "Created ability item " + ChatColor.WHITE + abilityCode
            + ChatColor.GREEN + " with skin " + ChatColor.WHITE + abilitySkin + ChatColor.GREEN + ".");
    }
}

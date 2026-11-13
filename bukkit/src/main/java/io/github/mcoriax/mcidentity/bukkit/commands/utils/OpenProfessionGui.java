package io.github.mcoriax.mcidentity.bukkit.commands.utils;

import io.github.mcoriax.mcidentity.bukkit.permission.Permissions;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;
import io.github.mcoriax.mcidentity.bukkit.util.HeadTexture;
import io.github.mcoriax.mcidentity.common.BaseProfession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Opens the profession list, where a click wears the profession.
 *
 * <p>Every registered profession is shown, the base profession first, each
 * marked as worn, wearable, or locked by permission. The profession code rides
 * on the item itself, so a click never depends on the list's order.</p>
 */
public final class OpenProfessionGui {

    /**
     * The GUI title, which the click listener routes on.
     */
    public static final String TITLE = "Professions";

    /**
     * The inventory size: five rows of professions and a bottom row of controls.
     */
    public static final int SIZE = 54;

    /**
     * The slot of the back button.
     */
    public static final int BACK_SLOT = 49;

    /**
     * The most professions the list shows.
     */
    private static final int MAX_PROFESSIONS = 45;

    private OpenProfessionGui() {
    }

    /**
     * The key the profession code is stored under on each list item.
     * @param plugin The plugin instance.
     * @return The key.
     */
    public static NamespacedKey professionKey(JavaPlugin plugin) {
        return new NamespacedKey(plugin, "profession_code");
    }

    /**
     * Opens the profession list for a player.
     * @param plugin The plugin instance.
     * @param player The player to open it for.
     */
    public static void execute(JavaPlugin plugin, Player player) {
        Schedulers.forEntity(player).run(plugin, st -> {
            Inventory gui = Bukkit.createInventory(player, SIZE, TITLE);
            String worn = MCIdentityManager.getActiveProfession(player.getUniqueId()).orElse(BaseProfession.CODE);
            NamespacedKey key = professionKey(plugin);

            int slot = 0;
            for (String code : orderedCodes()) {
                if (slot >= MAX_PROFESSIONS) {
                    break;
                }
                gui.setItem(slot++, professionItem(player, key, code, worn));
            }

            gui.setItem(BACK_SLOT, CreateCustomHead.execute(HeadTexture.getBackButton(plugin),
                    "mcidentity.gui.back", ChatColor.YELLOW + "Back"));
            player.openInventory(gui);
        }, null);
    }

    /**
     * Lists the registered professions, the base profession first and the rest
     * by code.
     * @return The codes in display order.
     */
    private static List<String> orderedCodes() {
        Map<String, String> registered = MCIdentityManager.getRegisteredProfessions();
        List<String> codes = new ArrayList<>(registered.keySet());
        codes.remove(BaseProfession.CODE);
        codes.sort(String::compareTo);
        codes.addFirst(BaseProfession.CODE);
        return codes;
    }

    /**
     * Builds one profession entry.
     * @param player The viewing player.
     * @param key The key the code is stored under.
     * @param code The profession's code.
     * @param worn The code of the profession the player wears.
     * @return The entry item.
     */
    private static ItemStack professionItem(Player player, NamespacedKey key, String code, String worn) {
        List<String> lore = new ArrayList<>();
        ChatColor nameColor;
        if (code.equalsIgnoreCase(worn)) {
            nameColor = ChatColor.GREEN;
            lore.add(ChatColor.GREEN + "Currently worn");
            if (!BaseProfession.isBaseProfession(code)) {
                lore.add(ChatColor.GRAY + "Click to take it off.");
            }
        } else if (Permissions.canWear(player, code)) {
            nameColor = ChatColor.AQUA;
            lore.add(ChatColor.GRAY + "Click to wear.");
        } else {
            nameColor = ChatColor.DARK_GRAY;
            lore.add(ChatColor.RED + "Locked");
        }

        ItemStack item = ProfessionHeads.build(player, code,
                nameColor + MCIdentityManager.getProfessionDisplayName(code), lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, code);
            item.setItemMeta(meta);
        }
        return item;
    }
}

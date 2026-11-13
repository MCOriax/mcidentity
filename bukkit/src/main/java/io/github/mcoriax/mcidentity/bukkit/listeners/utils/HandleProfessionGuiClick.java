package io.github.mcoriax.mcidentity.bukkit.listeners.utils;

import io.github.mcoriax.mcidentity.bukkit.commands.utils.OpenIdentityGui;
import io.github.mcoriax.mcidentity.bukkit.commands.utils.OpenProfessionGui;
import io.github.mcoriax.mcidentity.bukkit.permission.Permissions;
import io.github.mcoriax.mcidentity.bukkit.profession.ProfessionWearing;
import io.github.mcoriax.mcidentity.common.BaseProfession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles clicks in the profession list: a click wears the profession, or takes
 * off the one already worn.
 */
public final class HandleProfessionGuiClick {

    private HandleProfessionGuiClick() {
    }

    /**
     * Handles one click.
     * @param plugin The plugin instance.
     * @param event The inventory click event.
     */
    public static void execute(JavaPlugin plugin, InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);

        if (slot == OpenProfessionGui.BACK_SLOT) {
            OpenIdentityGui.execute(plugin, player);
            return;
        }

        String code = professionOf(plugin, event.getCurrentItem());
        if (code == null) {
            return;
        }

        String worn = MCIdentityManager.getActiveProfession(player.getUniqueId()).orElse(BaseProfession.CODE);
        if (code.equalsIgnoreCase(worn)) {
            if (!BaseProfession.isBaseProfession(code) && Permissions.grants(player, Permissions.UNWEAR)) {
                player.closeInventory();
                ProfessionWearing.unwear(plugin, player, true);
            }
            return;
        }
        if (!Permissions.grants(player, Permissions.WEAR)) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return;
        }
        player.closeInventory();
        ProfessionWearing.wear(plugin, player, code, true);
    }

    /**
     * Reads the profession code stored on a list item.
     * @param plugin The plugin instance.
     * @param item The clicked item.
     * @return The code, or null for an empty slot or a control.
     */
    private static String professionOf(JavaPlugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(OpenProfessionGui.professionKey(plugin), PersistentDataType.STRING);
    }
}

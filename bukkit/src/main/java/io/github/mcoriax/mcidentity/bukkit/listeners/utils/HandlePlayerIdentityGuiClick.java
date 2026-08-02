package io.github.mcoriax.mcidentity.bukkit.listeners.utils;

import io.github.mcoriax.mcidentity.bukkit.commands.MCIdentityCommand;
import io.github.mcoriax.mcidentity.bukkit.commands.utils.OpenPlayerIdentityGui;
import io.github.mcoriax.mcidentity.common.BaseProfession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Handles clicks in the paginated "Player Identity" GUI. Identity heads are
 * clickable: an alternative identity head in the content rows equips that
 * identity, and the primary identity head at the control-row center equips
 * the primary identity. The Back button at the control-row start returns to
 * the main Identity GUI, and the pagination arrows re-open the GUI at the
 * neighbouring page. The current page is parsed from the inventory title.
 */
public class HandlePlayerIdentityGuiClick {

    /**
     * Handle clicks in the Player Identity GUI.
     * @param plugin The plugin instance.
     * @param event The inventory click event.
     */
    public static void execute(JavaPlugin plugin, InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        int size = event.getView().getTopInventory().getSize();

        if (slot < 0 || slot >= size) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
            }
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) {
            return;
        }

        int controlRowStart = size - 9;
        int page = parsePage(event.getView().getTitle());
        if (page < 1) {
            return;
        }

        // An alternative identity head in the content rows equips that identity.
        // Which identity that is comes from the head's persistent data, not from
        // its name, its lore, or its position, so the click keeps working
        // whatever the head is labelled.
        if (slot < controlRowStart && clicked.getType() == Material.PLAYER_HEAD) {
            UUID identityUuid = stampedIdentity(plugin, clicked);
            int index = identityUuid != null
                ? BaseProfession.identityIndex(player.getUniqueId(), identityUuid,
                    MCIdentityManager.getCachedBaseIdentitySlots(player.getUniqueId()))
                : -1;
            if (index < 0) {
                // No usable stamp: fall back to the slot the head sits in.
                index = (page - 1) * OpenPlayerIdentityGui.ALTERNATIVES_PER_PAGE + slot + 1;
            }
            EquipBaseIdentity.execute(plugin, player, index);
            return;
        }

        // The Back button at the control-row start returns to the main GUI.
        if (slot == controlRowStart && clicked.getType() == Material.PLAYER_HEAD) {
            MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
            if (cmd != null) {
                cmd.openIdentityGui(player);
            }
            return;
        }

        // The primary identity head at the control-row center equips the
        // primary identity.
        if (slot == controlRowStart + 4 && clicked.getType() == Material.PLAYER_HEAD) {
            EquipBaseIdentity.execute(plugin, player, BaseProfession.PRIMARY_INDEX);
            return;
        }

        if (clicked.getType() != Material.ARROW || !clicked.hasItemMeta()) {
            return;
        }

        if (slot == controlRowStart + 3) {
            OpenPlayerIdentityGui.execute(plugin, player, page - 1);
        } else if (slot == controlRowStart + 5) {
            OpenPlayerIdentityGui.execute(plugin, player, page + 1);
        }
    }

    /**
     * Reads the identity a GUI head stands for out of its persistent data
     * container.
     * @param plugin The plugin instance, owning the key.
     * @param item The clicked head.
     * @return The identity UUID, or null when the head carries no usable stamp.
     */
    private static UUID stampedIdentity(JavaPlugin plugin, ItemStack item) {
        if (!item.hasItemMeta()) {
            return null;
        }
        String stored = item.getItemMeta().getPersistentDataContainer()
            .get(new NamespacedKey(plugin, "identity_uuid"), PersistentDataType.STRING);
        if (stored == null) {
            return null;
        }
        try {
            return UUID.fromString(stored);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Extracts the 1-based page number from the GUI title.
     * @param title The inventory view title.
     * @return The page number, or -1 when the title does not carry one.
     */
    private static int parsePage(String title) {
        if (!title.startsWith(OpenPlayerIdentityGui.TITLE_PREFIX)) {
            return -1;
        }
        try {
            return Integer.parseInt(title.substring(OpenPlayerIdentityGui.TITLE_PREFIX.length()).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}

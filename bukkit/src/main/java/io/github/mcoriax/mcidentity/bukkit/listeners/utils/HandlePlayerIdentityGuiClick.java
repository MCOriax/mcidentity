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
        int page = parsePage(event.getView().getTitle());
        if (clicked == null || page < 1) {
            return;
        }

        int controlRowStart = size - 9;
        if (clicked.getType() == Material.PLAYER_HEAD) {
            handleHeadClick(plugin, player, clicked, slot, controlRowStart, page);
        } else if (clicked.getType() == Material.ARROW && clicked.hasItemMeta()) {
            handlePageArrow(plugin, player, slot, controlRowStart, page);
        }
    }

    /**
     * Turns the page when one of the control-row arrows is clicked.
     * @param plugin The plugin instance.
     * @param player The player who clicked.
     * @param slot The raw slot clicked.
     * @param controlRowStart The first slot of the control row.
     * @param page The 1-based page being shown.
     */
    private static void handlePageArrow(JavaPlugin plugin, Player player, int slot, int controlRowStart, int page) {
        if (slot == controlRowStart + 3) {
            OpenPlayerIdentityGui.execute(plugin, player, page - 1);
        } else if (slot == controlRowStart + 5) {
            OpenPlayerIdentityGui.execute(plugin, player, page + 1);
        }
    }

    /**
     * Routes a click on a head: an alternative, the Back button, or the primary.
     * @param plugin The plugin instance.
     * @param player The player who clicked.
     * @param clicked The clicked head.
     * @param slot The raw slot clicked.
     * @param controlRowStart The first slot of the control row.
     * @param page The 1-based page being shown.
     */
    private static void handleHeadClick(JavaPlugin plugin, Player player, ItemStack clicked,
                                        int slot, int controlRowStart, int page) {
        // An alternative identity head in the content rows equips that identity.
        // Which identity that is comes from the head's persistent data, not from
        // its name, its lore, or its position, so the click keeps working
        // whatever the head is labelled.
        if (slot < controlRowStart) {
            EquipBaseIdentity.execute(plugin, player, alternativeIndex(plugin, player, clicked, slot, page));
            return;
        }

        // The Back button at the control-row start returns to the main GUI.
        if (slot == controlRowStart) {
            MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
            if (cmd != null) {
                cmd.openIdentityGui(player);
            }
            return;
        }

        // The primary identity head at the control-row center equips the
        // primary identity.
        if (slot == controlRowStart + 4) {
            EquipBaseIdentity.execute(plugin, player, BaseProfession.PRIMARY_INDEX);
        }
    }

    /**
     * Resolves which alternative a content-row head stands for, preferring its
     * persistent stamp and falling back to the slot it sits in.
     * @param plugin The plugin instance.
     * @param player The player who clicked.
     * @param clicked The clicked head.
     * @param slot The raw slot clicked.
     * @param page The 1-based page being shown.
     * @return The 1-based alternative index.
     */
    private static int alternativeIndex(JavaPlugin plugin, Player player, ItemStack clicked, int slot, int page) {
        UUID identityUuid = stampedIdentity(plugin, clicked);
        int index = identityUuid != null
            ? BaseProfession.identityIndex(player.getUniqueId(), identityUuid,
                MCIdentityManager.getCachedBaseIdentitySlots(player.getUniqueId()))
            : -1;
        if (index < 0) {
            // No usable stamp: fall back to the slot the head sits in.
            return (page - 1) * OpenPlayerIdentityGui.ALTERNATIVES_PER_PAGE + slot + 1;
        }
        return index;
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

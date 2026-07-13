package io.github.mcoriax.bukkit.commands.utils;

import io.github.mcoriax.bukkit.scheduler.Schedulers;
import io.github.mcoriax.common.BaseProfession;
import io.github.mcoriax.common.MCIdentityProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.UUID;

/**
 * Opens the paginated "Player Identity" GUI listing a player's base "player"
 * identities: the always-available primary identity plus every unlocked
 * alternative identity.
 *
 * <p>Alternative identities fill the content rows from slot 0, up to
 * {@link #ALTERNATIVES_PER_PAGE} per page. The GUI scales dynamically: a
 * single-page view only shows as many content rows as needed, while any
 * multi-page view uses the full six rows so the controls keep their fixed
 * places — the primary identity at slot 49, "Previous Page" at slot 48, and
 * "Next Page" at slot 50. Pagination buttons appear only when the
 * corresponding page exists.</p>
 */
public class OpenPlayerIdentityGui {

    /**
     * The GUI title prefix; the page number is appended after it.
     */
    public static final String TITLE_PREFIX = "Player Identity - Page ";

    /**
     * Alternative identities shown per page (the five content rows).
     */
    public static final int ALTERNATIVES_PER_PAGE = 45;

    /**
     * Opens the GUI at the given page.
     * @param plugin The plugin instance.
     * @param player The player to open the GUI for.
     * @param page The 1-based page number; out-of-range values are clamped.
     */
    public static void execute(JavaPlugin plugin, Player player, int page) {
        UUID playerUuid = player.getUniqueId();
        MCIdentityProvider.instance.getBaseIdentitySlots(playerUuid).thenAccept(slots ->
            Schedulers.forEntity(player).run(plugin, st -> {
                int totalPages = Math.max(1, (slots + ALTERNATIVES_PER_PAGE - 1) / ALTERNATIVES_PER_PAGE);
                int currentPage = Math.min(Math.max(page, 1), totalPages);

                int firstIndex = (currentPage - 1) * ALTERNATIVES_PER_PAGE + 1;
                int alternativesOnPage = Math.min(slots - (firstIndex - 1), ALTERNATIVES_PER_PAGE);

                // Content rows scale with the page; multi-page views keep the
                // full six rows so the controls stay at slots 48/49/50.
                int contentRows = (Math.max(alternativesOnPage, 0) + 8) / 9;
                int rows = totalPages > 1 ? 6 : contentRows + 1;
                int controlRowStart = (rows - 1) * 9;

                Inventory gui = Bukkit.createInventory(player, rows * 9, TITLE_PREFIX + currentPage);

                for (int i = 0; i < alternativesOnPage; i++) {
                    int index = firstIndex + i;
                    gui.setItem(i, alternativeIdentityItem(playerUuid, index));
                }

                ItemStack filler = fillerPane();
                for (int slot = controlRowStart; slot < controlRowStart + 9; slot++) {
                    gui.setItem(slot, filler);
                }

                gui.setItem(controlRowStart + 4, primaryIdentityItem(player));
                if (currentPage > 1) {
                    gui.setItem(controlRowStart + 3, pageButton(ChatColor.YELLOW + "Previous Page",
                        "Go to page " + (currentPage - 1)));
                }
                if (currentPage < totalPages) {
                    gui.setItem(controlRowStart + 5, pageButton(ChatColor.YELLOW + "Next Page",
                        "Go to page " + (currentPage + 1)));
                }

                player.openInventory(gui);
            }, null));
    }

    /**
     * Builds the primary identity button: the player's own head, since the
     * primary identity shares the player's UUID.
     * @param player The owning player.
     * @return The button item.
     */
    private static ItemStack primaryIdentityItem(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (head.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(ChatColor.GOLD + "Primary Identity");
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Your default " + BaseProfession.DISPLAY_NAME + " identity.",
                ChatColor.DARK_GRAY + "UUID: " + player.getUniqueId()));
            head.setItemMeta(meta);
        }
        return head;
    }

    /**
     * Builds the button for one alternative identity.
     * @param playerUuid The owning player's UUID.
     * @param index The alternative identity index, 1-based.
     * @return The button item.
     */
    private static ItemStack alternativeIdentityItem(UUID playerUuid, int index) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Alternative Identity #" + index);
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "An independent " + BaseProfession.DISPLAY_NAME + " identity.",
                ChatColor.DARK_GRAY + "UUID: " + BaseProfession.identityUuid(playerUuid, index)));
            head.setItemMeta(meta);
        }
        return head;
    }

    /**
     * Builds a pagination arrow button.
     * @param name The button display name.
     * @param loreLine The single lore line describing the target page.
     * @return The button item.
     */
    private static ItemStack pageButton(String name, String loreLine) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(ChatColor.GRAY + loreLine));
            arrow.setItemMeta(meta);
        }
        return arrow;
    }

    /**
     * Builds the unnamed filler pane for the control row.
     * @return The filler item.
     */
    private static ItemStack fillerPane() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("");
            pane.setItemMeta(meta);
        }
        return pane;
    }
}

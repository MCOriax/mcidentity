package io.github.mcoriax.bukkit.commands.utils;

import io.github.mcoriax.api.model.ActiveIdentitySession;
import io.github.mcoriax.bukkit.scheduler.Schedulers;
import io.github.mcoriax.bukkit.util.HeadTexture;
import io.github.mcoriax.common.BaseProfession;
import io.github.mcoriax.common.MCIdentityManager;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Opens the paginated "Player Identity" GUI listing a player's base
 * identities: the always-available primary "player" identity plus every
 * unlocked alternative identity (each of its own non-player profession).
 * Clicking an identity head equips it; the currently equipped base identity is
 * marked in its lore.
 *
 * <p>Alternative identities fill the content rows from slot 0, up to
 * {@link #ALTERNATIVES_PER_PAGE} per page. The GUI scales dynamically: a
 * single-page view only shows as many content rows as needed, while any
 * multi-page view uses the full six rows so the controls keep their fixed
 * places — the Back button at slot 45, the primary identity at slot 49, and
 * "Previous Page"/"Next Page" at slots 48 and 50. Pagination buttons appear
 * only when the corresponding page exists.</p>
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
                // full six rows so the controls stay at slots 45/48/49/50.
                int contentRows = (Math.max(alternativesOnPage, 0) + 8) / 9;
                int rows = totalPages > 1 ? 6 : contentRows + 1;
                int controlRowStart = (rows - 1) * 9;

                // The equipped base identity is marked in its lore.
                UUID equippedUuid = MCIdentityManager.getSession(playerUuid)
                    .map(ActiveIdentitySession::identityUuid)
                    .orElse(null);

                Inventory gui = Bukkit.createInventory(player, rows * 9, TITLE_PREFIX + currentPage);

                for (int i = 0; i < alternativesOnPage; i++) {
                    int index = firstIndex + i;
                    gui.setItem(i, alternativeIdentityItem(playerUuid, index, equippedUuid));
                }

                ItemStack filler = fillerPane();
                for (int slot = controlRowStart; slot < controlRowStart + 9; slot++) {
                    gui.setItem(slot, filler);
                }

                gui.setItem(controlRowStart, CreateCustomHead.execute(
                    HeadTexture.getBackButton(plugin), "mcidentity.gui.button.back", ChatColor.YELLOW + "Back"));
                gui.setItem(controlRowStart + 4, primaryIdentityItem(player, equippedUuid));
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
     * primary identity shares the player's UUID. Clicking it equips the
     * primary identity.
     * @param player The owning player.
     * @param equippedUuid The currently equipped identity UUID, or null.
     * @return The button item.
     */
    private static ItemStack primaryIdentityItem(Player player, UUID equippedUuid) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (head.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(ChatColor.GOLD + "Primary Identity");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Your default " + BaseProfession.DISPLAY_NAME + " identity.");
            lore.add(ChatColor.DARK_GRAY + "UUID: " + player.getUniqueId());
            lore.add(equippedLoreLine(player.getUniqueId(), equippedUuid));
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    /**
     * Builds the button for one alternative identity. Clicking it equips that
     * identity.
     * @param playerUuid The owning player's UUID.
     * @param index The alternative identity index, 1-based.
     * @param equippedUuid The currently equipped identity UUID, or null.
     * @return The button item.
     */
    private static ItemStack alternativeIdentityItem(UUID playerUuid, int index, UUID equippedUuid) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        if (meta != null) {
            UUID identityUuid = BaseProfession.identityUuid(playerUuid, index);
            meta.setDisplayName(ChatColor.AQUA + "Alternative Identity #" + index);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "A permanently bound alternative identity.");
            lore.add(ChatColor.DARK_GRAY + "UUID: " + identityUuid);
            lore.add(equippedLoreLine(identityUuid, equippedUuid));
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    /**
     * Builds the lore line telling whether this identity is the equipped one.
     * @param identityUuid The identity the button represents.
     * @param equippedUuid The currently equipped identity UUID, or null.
     * @return The lore line.
     */
    private static String equippedLoreLine(UUID identityUuid, UUID equippedUuid) {
        return identityUuid.equals(equippedUuid)
            ? ChatColor.GREEN + "Currently equipped."
            : ChatColor.YELLOW + "Click to equip.";
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
            meta.setLore(List.of(ChatColor.GRAY + loreLine));
            arrow.setItemMeta(meta);
        }
        return arrow;
    }

    /**
     * Builds the tooltip-less filler pane for the control row.
     * @return The filler item.
     */
    private static ItemStack fillerPane() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setHideTooltip(true);
            pane.setItemMeta(meta);
        }
        return pane;
    }
}

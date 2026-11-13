package io.github.mcoriax.mcidentity.bukkit.commands.utils;

import io.github.mcoriax.mcidentity.api.model.ActiveProfessionSession;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Opens the main {@code /identity} GUI: a button to the profession list in slot
 * 0 and the worn profession, with its level, in slot 4.
 */
public final class OpenIdentityGui {

    /**
     * The GUI title, which the click listener routes on.
     */
    public static final String TITLE = "Identity GUI";

    /**
     * The slot of the button opening the profession list.
     */
    public static final int PROFESSIONS_SLOT = 0;

    /**
     * The slot showing the worn profession.
     */
    public static final int WORN_SLOT = 4;

    private OpenIdentityGui() {
    }

    /**
     * Opens the main identity GUI for a player.
     * @param plugin The plugin instance.
     * @param player The player to open the GUI for.
     */
    public static void execute(JavaPlugin plugin, Player player) {
        Schedulers.forEntity(player).run(plugin, st -> {
            Inventory gui = Bukkit.createInventory(player, 9, TITLE);

            ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta meta = blackGlass.getItemMeta();
            if (meta != null) {
                meta.setHideTooltip(true);
                blackGlass.setItemMeta(meta);
            }
            for (int i = 0; i < 9; i++) {
                if (i != PROFESSIONS_SLOT && i != WORN_SLOT) {
                    gui.setItem(i, blackGlass);
                }
            }

            gui.setItem(PROFESSIONS_SLOT, professionsButton());
            gui.setItem(WORN_SLOT, wornProfession(player));
            player.openInventory(gui);
        }, null);
    }

    /**
     * Builds the slot 0 button opening the profession list.
     * @return The button item.
     */
    private static ItemStack professionsButton() {
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Professions");
            meta.setLore(List.of(ChatColor.GRAY + "Choose the profession you wear."));
            book.setItemMeta(meta);
        }
        return book;
    }

    /**
     * Builds the slot 4 item showing the worn profession.
     * @param player The player the GUI belongs to.
     * @return The item.
     */
    private static ItemStack wornProfession(Player player) {
        ActiveProfessionSession session = MCIdentityManager.getSession(player.getUniqueId()).orElse(null);
        if (session == null) {
            ItemStack redGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            ItemMeta meta = redGlass.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RED + "No profession loaded");
                redGlass.setItemMeta(meta);
            }
            return redGlass;
        }
        String code = session.professionCode();
        return ProfessionHeads.build(player, code,
            ChatColor.GREEN + MCIdentityManager.getProfessionDisplayName(code),
            List.of(
                ChatColor.GRAY + "Level: " + ChatColor.YELLOW + session.progress().getLevel(),
                ChatColor.GRAY + "Exp: " + ChatColor.YELLOW + session.progress().getExp(),
                ChatColor.GRAY + "Stat Points: " + ChatColor.YELLOW + session.stats().getStatPoints()));
    }
}

package io.github.mcoriax.mcidentity.bukkit.commands.utils;
import org.bukkit.ChatColor;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;

import io.github.mcoriax.mcidentity.api.model.ActiveIdentitySession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.mcidentity.bukkit.util.HeadTexture;
import io.github.mcoriax.mcidentity.bukkit.util.ItemStackSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class OpenIdentityGui {
    /**
     * Open the main identity GUI for a player.
     * @param plugin The plugin instance.
     * @param player The player to open the GUI for.
     */
    public static void execute(JavaPlugin plugin, Player player) {
        Schedulers.forEntity(player).run(plugin, st -> {
            Inventory gui = Bukkit.createInventory(player, 9, "Identity GUI");

            ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta meta = blackGlass.getItemMeta();
            if (meta != null) {
                meta.setHideTooltip(true);
                blackGlass.setItemMeta(meta);
            }

            // Only slots 0 and 4 carry a real item; everything else is filler.
            // Slots 2, 6, and 8 used to hold the ability, rune, and cosmetic
            // buttons, and were still skipped here after those were removed, so
            // they rendered as empty holes rather than glass.
            for (int i = 0; i < 9; i++) {
                if (i == 0 || i == 4) continue;
                gui.setItem(i, blackGlass);
            }

            gui.setItem(0, playerIdentityButton(player));

            String base64 = MCIdentityManager.getSession(player.getUniqueId())
                .map(ActiveIdentitySession::contentKey)
                .orElse(null);
            if (base64 == null) {
                // No session, or a base profession session: it has no item, so
                // slot 4 stays an open place target for a new identity key.
                gui.setItem(4, identitySlotPlaceholder());
                player.openInventory(gui);
                return;
            }

            openWithIdentityItem(plugin, player, gui, base64);
        }, null);
    }

    /**
     * Deserializes the worn identity item off the game thread, then opens the
     * GUI with it in slot 4 — or without it when it cannot be read.
     * @param plugin The plugin instance.
     * @param player The player the GUI belongs to.
     * @param gui The inventory being opened.
     * @param base64 The serialized identity item.
     */
    private static void openWithIdentityItem(JavaPlugin plugin, Player player, Inventory gui, String base64) {
        Schedulers.async().runNow(plugin, stAsync -> {
            ItemStack identityItem;
            try {
                identityItem = ItemStackSerializer.deserialize(base64);
            } catch (Exception e) {
                e.printStackTrace();
                identityItem = null;
            }

            final ItemStack resolved = identityItem;
            Schedulers.forEntity(player).run(plugin, stSync -> {
                if (resolved != null) {
                    gui.setItem(4, resolved);
                }
                player.openInventory(gui);
            }, null);
        });
    }

    /**
     * Builds the slot 0 button opening the Player Identity GUI, rendered as
     * the clicking player's own head.
     * @param player The player the GUI belongs to.
     * @return The button item.
     */
    private static ItemStack playerIdentityButton(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (head.getItemMeta() instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(ChatColor.AQUA + "Player Identity");
            head.setItemMeta(skullMeta);
        }
        return head;
    }

    /**
     * Builds the red placeholder shown in slot 4 when no identity item is
     * equipped there.
     * @return The placeholder item.
     */
    private static ItemStack identitySlotPlaceholder() {
        ItemStack redGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta redMeta = redGlass.getItemMeta();
        if (redMeta != null) {
            redMeta.setDisplayName(ChatColor.RED + "Identity Slot");
            redGlass.setItemMeta(redMeta);
        }
        return redGlass;
    }
}

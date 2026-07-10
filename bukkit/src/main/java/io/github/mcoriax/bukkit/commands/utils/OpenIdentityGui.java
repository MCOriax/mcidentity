package io.github.mcoriax.bukkit.commands.utils;
import org.bukkit.ChatColor;
import io.github.mcoriax.bukkit.scheduler.Schedulers;

import io.github.mcoriax.common.MCIdentityManager;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.bukkit.util.HeadTexture;
import io.github.mcoriax.bukkit.util.ItemStackSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
                meta.setDisplayName("");
                blackGlass.setItemMeta(meta);
            }

            for (int i = 0; i < 9; i++) {
                if (i == 4 || i == 6 || i == 2 || i == 8) continue;
                gui.setItem(i, blackGlass);
            }

            gui.setItem(6, CreateCustomHead.execute(HeadTexture.getRuneGui(plugin), "mcidentity.gui.button.rune", "Rune GUI"));
            gui.setItem(2, CreateCustomHead.execute(HeadTexture.getAbilityGui(plugin), "mcidentity.gui.button.ability", "Ability GUI"));
            gui.setItem(8, CreateCustomHead.execute(HeadTexture.getCosmeticGui(plugin), "mcidentity.gui.button.cosmetic", "Cosmetic GUI"));

            MCIdentityManager.getSession(player.getUniqueId()).ifPresentOrElse(session -> {
                String base64 = session.contentKey();
                if (base64 != null) {
                    Schedulers.async().runNow(plugin, stAsync -> {
                        try {
                            ItemStack identityItem = ItemStackSerializer.deserialize(base64);
                            Schedulers.forEntity(player).run(plugin, stSync -> {
                                gui.setItem(4, identityItem);
                                player.openInventory(gui);
                            }, null);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Schedulers.forEntity(player).run(plugin, stSync -> {
                                player.openInventory(gui);
                            }, null);
                        }
                    });
                } else {
                    // Base profession session: it has no item, so slot 4 stays
                    // an open place target for a new identity key.
                    gui.setItem(4, identitySlotPlaceholder());
                    player.openInventory(gui);
                }
            }, () -> {
                gui.setItem(4, identitySlotPlaceholder());
                player.openInventory(gui);
            });
        }, null);
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

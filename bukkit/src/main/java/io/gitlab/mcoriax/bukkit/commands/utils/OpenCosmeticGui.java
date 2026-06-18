package io.gitlab.mcoriax.bukkit.commands.utils;
import org.bukkit.ChatColor;
import io.gitlab.mcoriax.bukkit.scheduler.Schedulers;

import io.gitlab.mcoriax.api.model.IdentityProperties;
import io.gitlab.mcoriax.common.MCIdentityManager;
import org.bukkit.plugin.java.JavaPlugin;
import io.gitlab.mcoriax.bukkit.util.HeadTexture;
import io.gitlab.mcoriax.bukkit.util.MessageComponents;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class OpenCosmeticGui {
    /**
     * Open the cosmetic GUI for a player.
     * @param plugin The plugin instance.
     * @param player The player to open the GUI for.
     */
    public static void execute(JavaPlugin plugin, Player player) {
        MCIdentityManager.getSession(player.getUniqueId()).ifPresentOrElse(session -> {
            IdentityProperties props = session.properties();

            Schedulers.forEntity(player).run(plugin, st -> {
                Inventory gui = Bukkit.createInventory(player, 54, "Cosmetic GUI");

                ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                ItemMeta blackMeta = blackGlass.getItemMeta();
                if (blackMeta != null) {
                    blackMeta.setDisplayName("");
                    blackGlass.setItemMeta(blackMeta);
                }

                for (int i = 0; i < 54; i++) {
                    if (i == 0 || i == 49 || i == 51) continue;
                    gui.setItem(i, blackGlass);
                }

                gui.setItem(0, CreateCustomHead.execute(HeadTexture.getBackButton(plugin), "mcidentity.gui.button.back", "Back"));

                // Slot 49: Skin
                String skin = props.getSkin();
                if (skin != null && !skin.isEmpty()) {
                    gui.setItem(49, CreateCustomHead.execute(skin, "mcidentity.gui.cosmetic.skin", "Current Skin"));
                } else {
                    ItemStack redGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                    ItemMeta redMeta = redGlass.getItemMeta();
                    if (redMeta != null) {
                        redMeta.setDisplayName("No Skin Equipped");
                        redGlass.setItemMeta(redMeta);
                    }
                    gui.setItem(49, redGlass);
                }

                // Slot 51: Aura
                String aura = props.getAura();
                if (aura != null && !aura.isEmpty()) {
                    ItemStack limeGlass = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                    ItemMeta limeMeta = limeGlass.getItemMeta();
                    if (limeMeta != null) {
                        limeMeta.setDisplayName(MessageComponents.value(
                                "mcidentity.gui.cosmetic.aura_name.prefix",
                                "Aura: ",
                                aura,
                                "mcidentity.gui.cosmetic.aura_name.suffix",
                                ""
                        ));
                        limeGlass.setItemMeta(limeMeta);
                    }
                    gui.setItem(51, limeGlass);
                } else {
                    ItemStack redGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                    ItemMeta redMeta = redGlass.getItemMeta();
                    if (redMeta != null) {
                        redMeta.setDisplayName("No Aura Equipped");
                        redGlass.setItemMeta(redMeta);
                    }
                    gui.setItem(51, redGlass);
                }

                player.openInventory(gui);
            }, null);
        }, () -> {
            Schedulers.forEntity(player).run(plugin, st -> {
                player.sendMessage(ChatColor.RED + "You must have an active identity session!");
            }, null);
        });
    }
}

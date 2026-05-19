package io.gitlab.mcoriax.papermc.commands.utils;

import io.gitlab.mcoriax.api.model.IdentityProperties;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.papermc.MCIdentityPlugin;
import io.gitlab.mcoriax.papermc.util.HeadTexture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
    public static void execute(MCIdentityPlugin plugin, Player player) {
        MCIdentityManager.getSession(player.getUniqueId()).ifPresentOrElse(session -> {
            IdentityProperties props = session.properties();

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!plugin.isEnabled() || !player.isOnline()) return;

                Inventory gui = Bukkit.createInventory(player, 9, Component.translatable("mcidentity.gui.cosmetic.title", "Cosmetic GUI"));

                ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                ItemMeta blackMeta = blackGlass.getItemMeta();
                if (blackMeta != null) {
                    blackMeta.displayName(Component.empty());
                    blackGlass.setItemMeta(blackMeta);
                }

                for (int i = 0; i < 9; i++) {
                    if (i == 0 || i == 4 || i == 7) continue;
                    gui.setItem(i, blackGlass);
                }

                gui.setItem(0, CreateCustomHead.execute(HeadTexture.getBackButton(plugin), "mcidentity.gui.button.back", "Back"));

                // Slot 4: Skin
                String skin = props.getSkin();
                if (skin != null && !skin.isEmpty()) {
                    gui.setItem(4, CreateCustomHead.execute(skin, "mcidentity.gui.cosmetic.skin", "Current Skin"));
                } else {
                    ItemStack redGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                    ItemMeta redMeta = redGlass.getItemMeta();
                    if (redMeta != null) {
                        redMeta.displayName(Component.translatable("mcidentity.gui.cosmetic.no_skin", "No Skin Equipped").decoration(TextDecoration.ITALIC, false));
                        redGlass.setItemMeta(redMeta);
                    }
                    gui.setItem(4, redGlass);
                }

                // Slot 7: Aura
                String aura = props.getAura();
                if (aura != null && !aura.isEmpty()) {
                    ItemStack limeGlass = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                    ItemMeta limeMeta = limeGlass.getItemMeta();
                    if (limeMeta != null) {
                        limeMeta.displayName(Component.translatable("mcidentity.gui.cosmetic.aura_name", "Aura: " + aura, Component.text(aura)).decoration(TextDecoration.ITALIC, false));
                        limeGlass.setItemMeta(limeMeta);
                    }
                    gui.setItem(7, limeGlass);
                } else {
                    ItemStack redGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                    ItemMeta redMeta = redGlass.getItemMeta();
                    if (redMeta != null) {
                        redMeta.displayName(Component.translatable("mcidentity.gui.cosmetic.no_aura", "No Aura Equipped").decoration(TextDecoration.ITALIC, false));
                        redGlass.setItemMeta(redMeta);
                    }
                    gui.setItem(7, redGlass);
                }

                player.openInventory(gui);
            });
        }, () -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!plugin.isEnabled() || !player.isOnline()) return;
                player.sendMessage(Component.translatable("mcidentity.command.error.active_identity_required", "You must have an active identity session!").color(NamedTextColor.RED));
            });
        });
    }
}


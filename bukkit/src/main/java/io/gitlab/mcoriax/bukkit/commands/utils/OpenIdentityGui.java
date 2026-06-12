package io.gitlab.mcoriax.bukkit.commands.utils;

import io.gitlab.mcoriax.common.MCIdentityManager;
import org.bukkit.plugin.java.JavaPlugin;
import io.gitlab.mcoriax.bukkit.util.HeadTexture;
import io.gitlab.mcoriax.bukkit.util.ItemStackSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
        player.getScheduler().run(plugin, st -> {
            Inventory gui = Bukkit.createInventory(player, 9, Component.translatable("mcidentity.gui.title", "Identity GUI"));

            ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta meta = blackGlass.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.empty());
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
                    plugin.getServer().getAsyncScheduler().runNow(plugin, stAsync -> {
                        try {
                            ItemStack identityItem = ItemStackSerializer.deserialize(base64);
                            player.getScheduler().run(plugin, stSync -> {
                                gui.setItem(4, identityItem);
                                player.openInventory(gui);
                            }, null);
                        } catch (Exception e) {
                            e.printStackTrace();
                            player.getScheduler().run(plugin, stSync -> {
                                player.openInventory(gui);
                            }, null);
                        }
                    });
                } else {
                    player.openInventory(gui);
                }
            }, () -> {
                ItemStack redGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                ItemMeta redMeta = redGlass.getItemMeta();
                if (redMeta != null) {
                    redMeta.displayName(Component.translatable("mcidentity.gui.identity_slot", "Identity Slot").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
                    redGlass.setItemMeta(redMeta);
                }
                gui.setItem(4, redGlass);
                player.openInventory(gui);
            });
        }, null);
    }
}

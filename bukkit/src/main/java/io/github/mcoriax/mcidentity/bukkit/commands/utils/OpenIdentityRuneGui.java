package io.github.mcoriax.mcidentity.bukkit.commands.utils;
import org.bukkit.ChatColor;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;

import io.github.mcoriax.mcidentity.api.model.Rune;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.mcidentity.bukkit.util.HeadTexture;
import io.github.mcoriax.mcidentity.bukkit.util.MessageComponents;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class OpenIdentityRuneGui {
    /**
     * Open the identity rune GUI for a player.
     * @param plugin The plugin instance.
     * @param player The player to open the GUI for.
     */
    public static void execute(JavaPlugin plugin, Player player) {
        MCIdentityProvider.instance.getSession(player.getUniqueId()).thenAccept(optSession -> {
            if (optSession.isEmpty()) {
                Schedulers.forEntity(player).run(plugin, st -> {
                    player.sendMessage(ChatColor.RED + "You must have an active identity session!");
                }, null);
                return;
            }

            UUID identityUuid = optSession.get().getIdentityUniqueId();
            
            CompletableFuture<Map<Integer, String>> equippedFuture = MCIdentityProvider.instance.getRunes(identityUuid);
            CompletableFuture<Collection<Rune>> runesFuture = MCIdentityProvider.instance.getRunes();
            
            CompletableFuture.allOf(equippedFuture, runesFuture).thenAccept(v -> {
                Schedulers.forEntity(player).run(plugin, st -> {
                    Map<Integer, String> equipped = equippedFuture.join();
                    Collection<Rune> allRunes = runesFuture.join();
                    
                    Map<String, String> runeNameMap = allRunes.stream()
                            .collect(Collectors.toMap(Rune::getSystemCode, Rune::getDisplayName));

                    Inventory gui = Bukkit.createInventory(player, 54, "Rune GUI");
                    
                    ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                    ItemMeta blackMeta = blackGlass.getItemMeta();
                    if (blackMeta != null) {
                        blackMeta.setDisplayName("");
                        blackGlass.setItemMeta(blackMeta);
                    }

                    Map<Integer, String> runeSlots = Map.of(
                        10, "Rune slot 1", 12, "Rune slot 2", 14, "Rune slot 3",
                        16, "Rune slot 4", 29, "Rune slot 5", 31, "Rune slot 6",
                        33, "Rune slot 7", 48, "Rune slot 8", 50, "Rune slot 9",
                        53, "Rune slot ultimate"
                    );

                    int[] guiSlotOrder = {10, 12, 14, 16, 29, 31, 33, 48, 50, 53};

                    for (int i = 0; i < 54; i++) {
                        if (i == 45) {
                            gui.setItem(45, CreateCustomHead.execute(HeadTexture.getBackButton(plugin), "mcidentity.gui.button.back", "Back to Identity GUI"));
                            continue;
                        }

                        if (runeSlots.containsKey(i)) {
                            int logicSlot = -1;
                            for (int j = 0; j < guiSlotOrder.length; j++) {
                                if (guiSlotOrder[j] == i) {
                                    logicSlot = j + 1;
                                    break;
                                }
                            }

                            String runeCode = equipped.get(logicSlot);
                            if (runeCode != null && runeNameMap.containsKey(runeCode)) {
                                ItemStack limeGlass = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                                ItemMeta limeMeta = limeGlass.getItemMeta();
                                if (limeMeta != null) {
                                    String runeName = runeNameMap.get(runeCode);
                                    limeMeta.setDisplayName(ChatColor.GREEN + MessageComponents.value(
                                            "mcidentity.gui.rune.equipped.prefix",
                                            "",
                                            runeName,
                                            "mcidentity.gui.rune.equipped.suffix",
                                            ""
                                    ));
                                    limeGlass.setItemMeta(limeMeta);
                                }
                                gui.setItem(i, limeGlass);
                            } else {
                                String name = runeSlots.get(i);
                                String key = "mcidentity.gui.rune.slot." + (i == 53 ? "ultimate" : name.substring(name.lastIndexOf(" ") + 1));
                                
                                ItemStack redGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                                ItemMeta redMeta = redGlass.getItemMeta();
                                if (redMeta != null) {
                                    redMeta.setDisplayName(ChatColor.RED + name);
                                    redGlass.setItemMeta(redMeta);
                                }
                                gui.setItem(i, redGlass);
                            }
                        } else {
                            gui.setItem(i, blackGlass);
                        }
                    }

                    player.openInventory(gui);
                }, null);
            });
        });
    }
}

package io.github.mcoriax.bukkit.commands.utils;
import org.bukkit.ChatColor;
import io.github.mcoriax.bukkit.scheduler.Schedulers;

import io.github.mcoriax.api.model.Ability;
import io.github.mcoriax.api.model.EquippedAbility;
import io.github.mcoriax.common.MCIdentityProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.bukkit.util.HeadTexture;
import io.github.mcoriax.bukkit.util.MessageComponents;
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

public class OpenIdentityAbilityGui {
    /**
     * Open the identity ability GUI for a player.
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
            
            CompletableFuture<Map<Integer, EquippedAbility>> equippedFuture = MCIdentityProvider.instance.getEquippedAbilities(identityUuid);
            CompletableFuture<Collection<Ability>> abilitiesFuture = MCIdentityProvider.instance.getAbilities();
            
            CompletableFuture.allOf(equippedFuture, abilitiesFuture).thenAccept(v -> {
                Schedulers.forEntity(player).run(plugin, st -> {
                    Map<Integer, EquippedAbility> equipped = equippedFuture.join();
                    Collection<Ability> allAbilities = abilitiesFuture.join();
                    
                    Map<String, String> abilityNameMap = allAbilities.stream()
                            .collect(Collectors.toMap(Ability::getSystemCode, Ability::getDisplayName));

                    Inventory gui = Bukkit.createInventory(player, 54, "Ability GUI");
                    
                    ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                    ItemMeta blackMeta = blackGlass.getItemMeta();
                    if (blackMeta != null) {
                        blackMeta.setDisplayName("");
                        blackGlass.setItemMeta(blackMeta);
                    }

                    Map<Integer, String> abilitySlots = Map.of(
                        10, "Ability slot 1", 12, "Ability slot 2", 14, "Ability slot 3",
                        16, "Ability slot 4", 29, "Ability slot 5", 31, "Ability slot 6",
                        33, "Ability slot 7", 48, "Ability slot 8", 50, "Ability slot 9",
                        53, "Ability slot ultimate"
                    );

                    int[] guiSlotOrder = {10, 12, 14, 16, 29, 31, 33, 48, 50, 53};

                    for (int i = 0; i < 54; i++) {
                        if (i == 45) {
                            gui.setItem(45, CreateCustomHead.execute(HeadTexture.getBackButton(plugin), "mcidentity.gui.button.back", "Back to Identity GUI"));
                            continue;
                        }

                        if (abilitySlots.containsKey(i)) {
                            int logicSlot = -1;
                            for (int j = 0; j < guiSlotOrder.length; j++) {
                                if (guiSlotOrder[j] == i) {
                                    logicSlot = j + 1;
                                    break;
                                }
                            }

                            EquippedAbility equippedAbility = equipped.get(logicSlot);
                            if (equippedAbility != null && abilityNameMap.containsKey(equippedAbility.systemCode())) {
                                ItemStack limeGlass = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                                ItemMeta limeMeta = limeGlass.getItemMeta();
                                if (limeMeta != null) {
                                    String abilityName = abilityNameMap.get(equippedAbility.systemCode());
                                    limeMeta.setDisplayName(ChatColor.GREEN + MessageComponents.value(
                                            "mcidentity.gui.ability.equipped.prefix",
                                            "",
                                            abilityName,
                                            "mcidentity.gui.ability.equipped.suffix",
                                            ""
                                    ));
                                    limeGlass.setItemMeta(limeMeta);
                                }
                                gui.setItem(i, limeGlass);
                            } else {
                                String name = abilitySlots.get(i);
                                String key = "mcidentity.gui.ability.slot." + (i == 53 ? "ultimate" : name.substring(name.lastIndexOf(" ") + 1));
                                
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

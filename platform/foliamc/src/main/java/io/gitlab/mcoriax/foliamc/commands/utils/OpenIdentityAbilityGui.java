package io.gitlab.mcoriax.foliamc.commands.utils;

import io.gitlab.mcoriax.api.model.Ability;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import io.gitlab.mcoriax.foliamc.util.HeadTexture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
     */
    public static void execute(MCIdentityPlugin plugin, Player player) {
        MCIdentityProvider.instance.getSession(player.getUniqueId()).thenAccept(optSession -> {
            if (optSession.isEmpty()) {
                player.getScheduler().run(plugin, t -> {
                    if (!plugin.isEnabled() || !player.isOnline()) return;
                    player.sendMessage(Component.translatable("mcidentity.command.error.active_identity_required", "You must have an active identity session!").color(NamedTextColor.RED));
                }, null);
                return;
            }

            UUID identityUuid = optSession.get().getIdentityUniqueId();
            
            CompletableFuture<Map<Integer, String>> equippedFuture = MCIdentityProvider.instance.getEquippedAbilities(identityUuid);
            CompletableFuture<Collection<Ability>> abilitiesFuture = MCIdentityProvider.instance.getAbilities();
            
            CompletableFuture.allOf(equippedFuture, abilitiesFuture).thenAccept(v -> {
                player.getScheduler().run(plugin, task -> {
                    if (!plugin.isEnabled() || !player.isOnline()) return;

                    Map<Integer, String> equipped = equippedFuture.join();
                    Collection<Ability> allAbilities = abilitiesFuture.join();
                    
                    Map<String, String> abilityNameMap = allAbilities.stream()
                            .collect(Collectors.toMap(Ability::getSystemCode, Ability::getDisplayName));

                    Inventory gui = Bukkit.createInventory(player, 54, Component.translatable("mcidentity.gui.ability.title", "Ability GUI"));
                    
                    ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                    ItemMeta blackMeta = blackGlass.getItemMeta();
                    if (blackMeta != null) {
                        blackMeta.displayName(Component.empty());
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

                            String abilityCode = equipped.get(logicSlot);
                            if (abilityCode != null && abilityNameMap.containsKey(abilityCode)) {
                                ItemStack limeGlass = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                                ItemMeta limeMeta = limeGlass.getItemMeta();
                                if (limeMeta != null) {
                                    String abilityName = abilityNameMap.get(abilityCode);
                                    limeMeta.displayName(Component.translatable("mcidentity.gui.ability.equipped", abilityName, Component.text(abilityName)).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                                    limeGlass.setItemMeta(limeMeta);
                                }
                                gui.setItem(i, limeGlass);
                            } else {
                                String name = abilitySlots.get(i);
                                String key = "mcidentity.gui.ability.slot." + (i == 53 ? "ultimate" : name.substring(name.lastIndexOf(" ") + 1));
                                
                                ItemStack redGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                                ItemMeta redMeta = redGlass.getItemMeta();
                                if (redMeta != null) {
                                    redMeta.displayName(Component.translatable(key, name).color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
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

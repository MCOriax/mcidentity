package io.gitlab.mcoriax.foliamc.commands;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.gitlab.mcoriax.api.db.IMCIdentityDB;
import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import io.gitlab.mcoriax.foliamc.util.HeadTexture;
import io.gitlab.mcoriax.foliamc.util.ItemStackSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MCIdentityCommand implements CommandExecutor {

    private final MCIdentityPlugin plugin;
    private final NamespacedKey identityKey;
    private final NamespacedKey sigilKey;
    private final NamespacedKey professionKey;

    public MCIdentityCommand(MCIdentityPlugin plugin) {
        this.plugin = plugin;
        this.identityKey = new NamespacedKey(plugin, "identity_uuid");
        this.sigilKey = new NamespacedKey(plugin, "sigil_code");
        this.professionKey = new NamespacedKey(plugin, "profession_code");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            openIdentityGui(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            // Step 1: Fix Security Vulnerability (Command Permissions)
            if (!player.hasPermission("mcidentity.admin")) {
                player.sendMessage(Component.text("You do not have permission to use this command!", NamedTextColor.RED));
                return true;
            }

            if (args.length == 3 && args[1].equalsIgnoreCase("sigil")) {
                // /identity create sigil <name>
                String sigilCode = args[2];
                MCIdentityProvider.getSigils().thenAccept(sigils -> {
                    boolean exists = sigils.stream().anyMatch(s -> s.getSystemCode().equalsIgnoreCase(sigilCode));
                    if (exists) {
                        player.getScheduler().run(plugin, task -> {
                            if (player.isOnline()) {
                                player.sendMessage(Component.text("Sigil '" + sigilCode + "' already exists!", NamedTextColor.RED));
                            }
                        }, null);
                        return;
                    }
                    MCIdentityProvider.createSigil(sigilCode, sigilCode).thenAccept(v -> {
                        player.getScheduler().run(plugin, task -> {
                            if (player.isOnline()) {
                                player.sendMessage(Component.text("Sigil '" + sigilCode + "' created successfully!", NamedTextColor.GREEN));
                            }
                        }, null);
                    }).exceptionally(ex -> {
                        player.getScheduler().run(plugin, task -> {
                            if (player.isOnline()) {
                                player.sendMessage(Component.text("Failed to create sigil: " + ex.getMessage(), NamedTextColor.RED));
                            }
                        }, null);
                        return null;
                    });
                });
                return true;
            }

            if (args.length == 3 && args[1].equalsIgnoreCase("profession")) {
                // /identity create profession <name>
                String professionCode = args[2];
                MCIdentityProvider.getProfessions().thenAccept(professions -> {
                    boolean exists = professions.stream().anyMatch(p -> p.getSystemCode().equalsIgnoreCase(professionCode));
                    if (exists) {
                        player.getScheduler().run(plugin, task -> {
                            if (player.isOnline()) {
                                player.sendMessage(Component.text("Profession '" + professionCode + "' already exists!", NamedTextColor.RED));
                            }
                        }, null);
                        return;
                    }
                    MCIdentityProvider.createProfession(professionCode, professionCode).thenAccept(v -> {
                        player.getScheduler().run(plugin, task -> {
                            if (player.isOnline()) {
                                player.sendMessage(Component.text("Profession '" + professionCode + "' created successfully!", NamedTextColor.GREEN));
                            }
                        }, null);
                    });
                });
                return true;
            }

            if (args.length == 3 && args[1].equalsIgnoreCase("ability")) {
                // /identity create ability <name>
                String abilityCode = args[2];
                MCIdentityProvider.getAbilities().thenAccept(abilities -> {
                    boolean exists = abilities.stream().anyMatch(a -> a.getSystemCode().equalsIgnoreCase(abilityCode));
                    if (exists) {
                        player.getScheduler().run(plugin, task -> {
                            if (player.isOnline()) {
                                player.sendMessage(Component.text("Ability '" + abilityCode + "' already exists!", NamedTextColor.RED));
                            }
                        }, null);
                        return;
                    }
                    // Default to ACTIVE and not global for now
                    MCIdentityProvider.createAbility(abilityCode, abilityCode, AbilityType.ACTIVE, false).thenAccept(v -> {
                        player.getScheduler().run(plugin, task -> {
                            if (player.isOnline()) {
                                player.sendMessage(Component.text("Ability '" + abilityCode + "' created successfully!", NamedTextColor.GREEN));
                            }
                        }, null);
                    });
                });
                return true;
            }

            if (args.length == 3) {
                // OLD logic: /identity create <profession> <sigil>
                String professionCode = args[1];
                String sigilCode = args[2];

                MCIdentityProvider.getSigils().thenAccept(sigils -> {
                    boolean sigilExists = sigils.stream().anyMatch(s -> s.getSystemCode().equalsIgnoreCase(sigilCode));
                    if (!sigilExists) {
                        player.getScheduler().run(plugin, task -> {
                            if (player.isOnline()) {
                                player.sendMessage(Component.text("Error: Sigil '" + sigilCode + "' does not exist!", NamedTextColor.RED));
                            }
                        }, null);
                        return;
                    }

                    MCIdentityProvider.getProfessions().thenAccept(professions -> {
                        boolean professionExists = professions.stream().anyMatch(p -> p.getSystemCode().equalsIgnoreCase(professionCode));
                        if (!professionExists) {
                            player.getScheduler().run(plugin, task -> {
                                if (player.isOnline()) {
                                    player.sendMessage(Component.text("Error: Profession '" + professionCode + "' does not exist!", NamedTextColor.RED));
                                }
                            }, null);
                            return;
                        }

                        // Also create an identity key item for convenience
                        createIdentityItem(player, sigilCode, professionCode);
                    });
                });
                return true;
            }

            player.sendMessage(Component.text("Usage: /identity create <sigil|profession|ability> <name> OR /identity create <profession> <sigil>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("unuse")) {
            MCIdentityProvider.getSession(player.getUniqueId()).thenAccept(optSession -> {
                if (optSession.isEmpty()) {
                    player.getScheduler().run(plugin, task -> {
                        if (player.isOnline()) {
                            player.sendMessage(Component.text("You are not currently wearing an identity!", NamedTextColor.RED));
                        }
                    }, null);
                    return;
                }

                IdentitySession session = optSession.get();
                UUID identityUuid = session.getIdentityUniqueId();
                String base64 = session.getContentKey();

                MCIdentityProvider.setActiveIdentity(player.getUniqueId(), null, null).thenAccept(v -> {
                    if (base64 != null) {
                        try {
                            ItemStack item = ItemStackSerializer.deserialize(base64);
                            player.getScheduler().run(plugin, task -> {
                                if (player.isOnline()) {
                                    player.getInventory().addItem(item);
                                    player.sendMessage(Component.text("Identity unequipped and returned to inventory.", NamedTextColor.GREEN));
                                }
                            }, null);
                            MCIdentityProvider.logAction(player.getUniqueId(), identityUuid, ActionType.UNEQUIP);
                        } catch (Exception e) {
                            player.getScheduler().run(plugin, task -> {
                                if (player.isOnline()) {
                                    player.sendMessage(Component.text("Failed to recover identity item!", NamedTextColor.RED));
                                }
                            }, null);
                            e.printStackTrace();
                        }
                    }
                });
            });
            return true;
        }

        if (args[0].equalsIgnoreCase("check")) {
            ItemStack item = player.getInventory().getItemInMainHand();
            boolean checkedHand = false;

            if (item.getType() != Material.AIR && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                String uuidStr = meta.getPersistentDataContainer().get(identityKey, PersistentDataType.STRING);
                if (uuidStr != null) {
                    UUID uuid = UUID.fromString(uuidStr);
                    fetchAndDisplayIdentityInfo(player, uuid);
                    checkedHand = true;
                }
            }

            if (!checkedHand) {
                Optional<ActiveIdentitySession> optRamSession = MCIdentityManager.getSession(player.getUniqueId());
                if (optRamSession.isPresent()) {
                    ActiveIdentitySession session = optRamSession.get();
                    renderIdentityInfo(player, session.identityUuid(), session.identityKey(), session.properties(), session.stats());
                } else {
                    player.sendMessage(Component.text("You are not wearing an identity and not holding an identity key!", NamedTextColor.RED));
                }
            }
            return true;
        }

        return false;
    }

    public void openIdentityGui(Player player) {
        MCIdentityProvider.getSession(player.getUniqueId()).thenAccept(optSession -> {
            Inventory gui = Bukkit.createInventory(player, 9, Component.translatable("mcidentity.gui.title", "Identity GUI"));
            
            ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta meta = blackGlass.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.empty());
                blackGlass.setItemMeta(meta);
            }

            for (int i = 0; i < 9; i++) {
                if (i == 4 || i == 6 || i == 2) continue; // Skip identity, rune, and ability slots
                gui.setItem(i, blackGlass);
            }

            // Slot 6: Rune GUI Button
            gui.setItem(6, createCustomHead(HeadTexture.RUNE_GUI, "mcidentity.gui.button.rune", "Rune GUI"));

            // Slot 2: Ability GUI Button
            gui.setItem(2, createCustomHead(HeadTexture.ABILITY_GUI, "mcidentity.gui.button.ability", "Ability GUI"));

            if (optSession.isPresent()) {
                String base64 = optSession.get().getContentKey();
                if (base64 != null) {
                    try {
                        ItemStack identityItem = ItemStackSerializer.deserialize(base64);
                        gui.setItem(4, identityItem);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                ItemStack redGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                ItemMeta redMeta = redGlass.getItemMeta();
                if (redMeta != null) {
                    redMeta.displayName(Component.text("Identity Slot", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
                    redGlass.setItemMeta(redMeta);
                }
                gui.setItem(4, redGlass);
            }

            player.getScheduler().run(plugin, task -> {
                if (player.isOnline()) {
                    player.openInventory(gui);
                }
            }, null);
        });
    }

    public void openIdentityAbilityGui(Player player) {
        MCIdentityProvider.getSession(player.getUniqueId()).thenAccept(optSession -> {
            if (optSession.isEmpty()) {
                player.sendMessage(Component.text("You must have an active identity session!", NamedTextColor.RED));
                return;
            }

            UUID identityUuid = optSession.get().getIdentityUniqueId();
            
            CompletableFuture.allOf(
                MCIdentityProvider.getEquippedAbilities(identityUuid),
                MCIdentityProvider.getAbilities()
            ).thenAccept(v -> {
                Map<Integer, String> equipped = MCIdentityProvider.getEquippedAbilities(identityUuid).join();
                Collection<Ability> allAbilities = MCIdentityProvider.getAbilities().join();
                
                Map<String, String> abilityNameMap = allAbilities.stream()
                        .collect(java.util.stream.Collectors.toMap(Ability::getSystemCode, Ability::getDisplayName));

                Inventory gui = Bukkit.createInventory(player, 54, Component.translatable("mcidentity.gui.ability.title", "Ability GUI"));
                
                ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                ItemMeta blackMeta = blackGlass.getItemMeta();
                if (blackMeta != null) {
                    blackMeta.displayName(Component.empty());
                    blackGlass.setItemMeta(blackMeta);
                }

                Map<Integer, String> abilitySlots = Map.of(
                    10, "Ability slot 1",
                    12, "Ability slot 2",
                    14, "Ability slot 3",
                    16, "Ability slot 4",
                    29, "Ability slot 5",
                    31, "Ability slot 6",
                    33, "Ability slot 7",
                    48, "Ability slot 8",
                    50, "Ability slot 9",
                    53, "Ability slot ultimate"
                );

                // Map GUI slot index (1-10) to actual GUI slots
                int[] guiSlotOrder = {10, 12, 14, 16, 29, 31, 33, 48, 50, 53};

                for (int i = 0; i < 54; i++) {
                    if (i == 45) {
                        // Back button
                        gui.setItem(45, createCustomHead(HeadTexture.BACK_BUTTON, "mcidentity.gui.button.back", "Back to Identity GUI"));
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
                            // Equipped
                            ItemStack limeGlass = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                            ItemMeta limeMeta = limeGlass.getItemMeta();
                            if (limeMeta != null) {
                                limeMeta.displayName(Component.text(abilityNameMap.get(abilityCode), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                                limeGlass.setItemMeta(limeMeta);
                            }
                            gui.setItem(i, limeGlass);
                        } else {
                            // Not equipped - use red glass
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

                player.getScheduler().run(plugin, task -> {
                    if (player.isOnline()) {
                        player.openInventory(gui);
                    }
                }, null);
            });
        });
    }

    public void openIdentityRuneGui(Player player) {
        MCIdentityProvider.getSession(player.getUniqueId()).thenAccept(optSession -> {
            if (optSession.isEmpty()) {
                player.sendMessage(Component.text("You must have an active identity session!", NamedTextColor.RED));
                return;
            }

            UUID identityUuid = optSession.get().getIdentityUniqueId();
            
            CompletableFuture.allOf(
                MCIdentityProvider.getRunes(identityUuid),
                MCIdentityProvider.getRunes()
            ).thenAccept(v -> {
                Map<Integer, String> equipped = MCIdentityProvider.getRunes(identityUuid).join();
                Collection<Rune> allRunes = MCIdentityProvider.getRunes().join();
                
                Map<String, String> runeNameMap = allRunes.stream()
                        .collect(java.util.stream.Collectors.toMap(Rune::getSystemCode, Rune::getDisplayName));

                Inventory gui = Bukkit.createInventory(player, 54, Component.translatable("mcidentity.gui.rune.title", "Rune GUI"));
                
                ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                ItemMeta blackMeta = blackGlass.getItemMeta();
                if (blackMeta != null) {
                    blackMeta.displayName(Component.empty());
                    blackGlass.setItemMeta(blackMeta);
                }

                Map<Integer, String> runeSlots = Map.of(
                    10, "Rune slot 1",
                    12, "Rune slot 2",
                    14, "Rune slot 3",
                    16, "Rune slot 4",
                    29, "Rune slot 5",
                    31, "Rune slot 6",
                    33, "Rune slot 7",
                    48, "Rune slot 8",
                    50, "Rune slot 9",
                    53, "Rune slot ultimate"
                );

                // Map GUI slot index (1-10) to actual GUI slots
                int[] guiSlotOrder = {10, 12, 14, 16, 29, 31, 33, 48, 50, 53};

                for (int i = 0; i < 54; i++) {
                    if (i == 45) {
                        // Back button
                        gui.setItem(45, createCustomHead(HeadTexture.BACK_BUTTON, "mcidentity.gui.button.back", "Back to Identity GUI"));
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
                            // Equipped
                            ItemStack limeGlass = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                            ItemMeta limeMeta = limeGlass.getItemMeta();
                            if (limeMeta != null) {
                                limeMeta.displayName(Component.text(runeNameMap.get(runeCode), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                                limeGlass.setItemMeta(limeMeta);
                            }
                            gui.setItem(i, limeGlass);
                        } else {
                            // Not equipped - use red glass
                            String name = runeSlots.get(i);
                            String key = "mcidentity.gui.rune.slot." + (i == 53 ? "ultimate" : name.substring(name.lastIndexOf(" ") + 1));
                            
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

                player.getScheduler().run(plugin, task -> {
                    if (player.isOnline()) {
                        player.openInventory(gui);
                    }
                }, null);
            });
        });
    }

    private ItemStack createCustomHead(String texture, String translationKey, String fallback) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.translatable(translationKey, fallback).decoration(TextDecoration.ITALIC, false));
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", texture));
            meta.setPlayerProfile(profile);
            head.setItemMeta(meta);
        }
        return head;
    }

    private void fetchAndDisplayIdentityInfo(Player player, UUID uuid) {
        MCIdentityProvider.getIdentityKey(uuid).thenAccept(optKey -> {
            if (optKey.isEmpty()) {
                player.getScheduler().run(plugin, task -> {
                    if (player.isOnline()) {
                        player.sendMessage(Component.text("Identity data not found in database.", NamedTextColor.RED));
                    }
                }, null);
                return;
            }

            IdentityKey key = optKey.get();
            MCIdentityProvider.getProperties(uuid).thenAccept(optProps -> {
                MCIdentityProvider.getStats(uuid).thenAccept(optStats -> {
                    player.getScheduler().run(plugin, task -> {
                        if (player.isOnline()) {
                            if (optProps.isPresent() && optStats.isPresent()) {
                                renderIdentityInfo(player, uuid, key, optProps.get(), optStats.get());
                            } else {
                                player.sendMessage(Component.text("Identity data is incomplete in database.", NamedTextColor.RED));
                            }
                        }
                    }, null);
                });
            });
        });
    }

    private void renderIdentityInfo(Player player, UUID uuid, IdentityKey key, IdentityProperties props, IdentityStats stats) {
        player.sendMessage(Component.text("--- Identity Data ---", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("UUID: ", NamedTextColor.GRAY).append(Component.text(uuid.toString(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Sigil: ", NamedTextColor.GRAY).append(Component.text(key.getSigilCode(), NamedTextColor.AQUA)));
        player.sendMessage(Component.text("Profession: ", NamedTextColor.GRAY).append(Component.text(key.getProfessionCode(), NamedTextColor.GREEN)));

        player.sendMessage(Component.text("Level: ", NamedTextColor.GRAY).append(Component.text(props.getLevel(), NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("Exp: ", NamedTextColor.GRAY).append(Component.text(props.getExp(), NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("Stat Points: ", NamedTextColor.GRAY).append(Component.text(props.getStatPoints(), NamedTextColor.YELLOW)));

        player.sendMessage(Component.text("Health: ", NamedTextColor.GRAY).append(Component.text(stats.getHealth(), NamedTextColor.RED)));
        player.sendMessage(Component.text("Energy: ", NamedTextColor.GRAY).append(Component.text(stats.getEnergy(), NamedTextColor.BLUE)));
        player.sendMessage(Component.text("Strength: ", NamedTextColor.GRAY).append(Component.text(stats.getStrength(), NamedTextColor.DARK_RED)));
        player.sendMessage(Component.text("Defense: ", NamedTextColor.GRAY).append(Component.text(stats.getDefense(), NamedTextColor.GRAY)));
        player.sendMessage(Component.text("Wisdom: ", NamedTextColor.GRAY).append(Component.text(stats.getWisdom(), NamedTextColor.LIGHT_PURPLE)));
        player.sendMessage(Component.text("Luck: ", NamedTextColor.GRAY).append(Component.text(stats.getLuck(), NamedTextColor.GOLD)));
        player.sendMessage(Component.text("Agility: ", NamedTextColor.GRAY).append(Component.text(stats.getAgility(), NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("Resilience: ", NamedTextColor.GRAY).append(Component.text(stats.getResilience(), NamedTextColor.DARK_GREEN)));
    }

    private void createIdentityItem(Player player, String sigilCode, String professionCode) {
        UUID identityUuid = UUID.randomUUID();
        
        IdentityKey key = new IdentityKey() {
            @Override public UUID getUniqueId() { return identityUuid; }
            @Override public String getSigilCode() { return sigilCode; }
            @Override public String getProfessionCode() { return professionCode; }
        };

        IdentityProperties props = new IdentityProperties() {
            @Override public int getLevel() { return 1; }
            @Override public int getExp() { return 0; }
            @Override public int getStatPoints() { return 0; }
            @Override public String getHeadText() { return null; }
            @Override public String getModel() { return null; }
        };

        IdentityStats stats = new IdentityStats() {
            @Override public int getEnergy() { return 100; }
            @Override public int getHealth() { return 20; }
            @Override public int getStrength() { return 0; }
            @Override public int getDefense() { return 0; }
            @Override public int getWisdom() { return 0; }
            @Override public int getLuck() { return 0; }
            @Override public int getAgility() { return 0; }
            @Override public int getResilience() { return 0; }
        };

        MCIdentityProvider.createIdentity(key, props, stats).thenAccept(v -> {
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta != null) {
                // Set custom texture based on profession
                if (professionCode.equalsIgnoreCase("tanker")) {
                    List<String> textures = HeadTexture.TANKER_DEFAULTS;
                    if (!textures.isEmpty()) {
                        String randomTexture = textures.get(new java.util.Random().nextInt(textures.size()));
                        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
                        profile.setProperty(new ProfileProperty("textures", randomTexture));
                        meta.setPlayerProfile(profile);
                    }
                }

                meta.displayName(
                    Component.text("[", NamedTextColor.GRAY)
                        .append(Component.text(sigilCode, NamedTextColor.AQUA))
                        .append(Component.text("] [", NamedTextColor.GRAY))
                        .append(Component.text(professionCode, NamedTextColor.GREEN))
                        .append(Component.text("] [", NamedTextColor.GRAY))
                        .append(Component.text(props.getLevel(), NamedTextColor.YELLOW))
                        .append(Component.text("]", NamedTextColor.GRAY))
                        .decoration(TextDecoration.ITALIC, false)
                );
                meta.getPersistentDataContainer().set(identityKey, PersistentDataType.STRING, identityUuid.toString());
                meta.getPersistentDataContainer().set(sigilKey, PersistentDataType.STRING, sigilCode);
                meta.getPersistentDataContainer().set(professionKey, PersistentDataType.STRING, professionCode);
                item.setItemMeta(meta);
            }

            player.getScheduler().run(plugin, task -> {
                if (player.isOnline()) {
                    player.getInventory().addItem(item);
                    player.sendMessage(Component.text("Identity item given!", NamedTextColor.GREEN));
                }
            }, null);
        });
    }
}

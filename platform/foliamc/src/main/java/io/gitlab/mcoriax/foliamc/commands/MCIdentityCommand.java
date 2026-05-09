package io.gitlab.mcoriax.foliamc.commands;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MCIdentityCommand implements CommandExecutor {

    private final MCIdentityPlugin plugin;
    private final NamespacedKey identityKey;
    private final NamespacedKey professionKey;

    public MCIdentityCommand(MCIdentityPlugin plugin) {
        this.plugin = plugin;
        this.identityKey = new NamespacedKey(plugin, "identity_uuid");
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

        if (args[0].equalsIgnoreCase("help")) {
            sendHelp(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("ability")) {
            if (args.length >= 3 && args[1].equalsIgnoreCase("get")) {
                // Check command permissions securely
                if (!player.hasPermission("mcidentity.admin")) {
                    player.sendMessage(Component.text("You do not have permission to use this command!", NamedTextColor.RED));
                    return true;
                }

                String abilityCode = args[2].toLowerCase();
                MCIdentityProvider.instance.getAbilities().thenAccept(abilities -> {
                    Optional<Ability> optAbility = abilities.stream()
                            .filter(a -> a.getSystemCode().equalsIgnoreCase(abilityCode))
                            .findFirst();

                    if (optAbility.isEmpty()) {
                        player.getScheduler().run(plugin, task -> {
                            if (player.isOnline()) {
                                player.sendMessage(Component.text("Ability '" + abilityCode + "' not found!", NamedTextColor.RED));
                            }
                        }, null);
                        return;
                    }

                    Ability ability = optAbility.get();
                    player.getScheduler().run(plugin, task -> {
                        if (!player.isOnline()) return;

                        ItemStack item = new ItemStack(Material.PAPER);
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            String typeStr = ability.getAbilityType() == AbilityType.ACTIVE ? "Active" : "Passive";
                            StringBuilder nameBuilder = new StringBuilder();
                            nameBuilder.append(ability.getDisplayName()).append(" <").append(typeStr).append(">");
                            if (ability.isGlobal()) {
                                nameBuilder.append(" <Global>");
                            }

                            meta.displayName(Component.text(nameBuilder.toString()).decoration(TextDecoration.ITALIC, false));
                            item.setItemMeta(meta);
                        }

                        player.getInventory().addItem(item);
                        player.sendMessage(Component.text("You received the ability item for: " + ability.getDisplayName(), NamedTextColor.GREEN));
                    }, null);
                });
                return true;
            }
            sendHelp(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            // Check command permissions securely
            if (!player.hasPermission("mcidentity.admin")) {
                player.sendMessage(Component.text("You do not have permission to use this command!", NamedTextColor.RED));
                return true;
            }

            if (args.length >= 2) {
                String professionCode = args[1].toLowerCase();
                String customTexture = args.length >= 3 ? args[2] : null;

                // Combine future requests to check dependencies at once
                MCIdentityProvider.instance.getProfessions().thenAccept(professions -> {
                    Optional<Profession> optProfession = professions.stream().filter(p -> p.getSystemCode().equalsIgnoreCase(professionCode)).findFirst();
                    if (optProfession.isEmpty()) {
                        player.getScheduler().run(plugin, task -> {
                            if (player.isOnline()) player.sendMessage(Component.text("Error: Profession '" + professionCode + "' does not exist!", NamedTextColor.RED));
                        }, null);
                        return;
                    }

                    createIdentityItem(player, optProfession.get(), customTexture);
                });
                return true;
            }

            player.sendMessage(Component.text("Usage: /identity create <profession> [head texture]", NamedTextColor.RED));
            return true;
        }

        if (args[0].equalsIgnoreCase("unuse")) {
            MCIdentityProvider.instance.getSession(player.getUniqueId()).thenAccept(optSession -> {
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

                MCIdentityProvider.instance.setActiveIdentity(player.getUniqueId(), null, null).thenAccept(v -> {
                    if (base64 != null) {
                        try {
                            ItemStack item = ItemStackSerializer.deserialize(base64);
                            player.getScheduler().run(plugin, task -> {
                                if (player.isOnline()) {
                                    player.getInventory().addItem(item);
                                    player.sendMessage(Component.text("Identity unequipped and returned to inventory.", NamedTextColor.GREEN));
                                }
                            }, null);
                            MCIdentityProvider.instance.logAction(player.getUniqueId(), identityUuid, ActionType.UNEQUIP);
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

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("--- MCIdentity Help ---", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("/identity", NamedTextColor.YELLOW).append(Component.text(" - Open the main Identity GUI.", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/identity help", NamedTextColor.YELLOW).append(Component.text(" - Show this help message.", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/identity check", NamedTextColor.YELLOW).append(Component.text(" - Check the identity key in hand or equipped identity.", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/identity unuse", NamedTextColor.YELLOW).append(Component.text(" - Unequip your current active identity.", NamedTextColor.GRAY)));
        
        if (player.hasPermission("mcidentity.admin")) {
            player.sendMessage(Component.text("--- Admin Commands ---", NamedTextColor.RED, TextDecoration.BOLD));
            player.sendMessage(Component.text("/identity ability get <system code>", NamedTextColor.YELLOW).append(Component.text(" - Get an ability item.", NamedTextColor.GRAY)));
            player.sendMessage(Component.text("/identity create <profession> [texture]", NamedTextColor.YELLOW).append(Component.text(" - Create an identity key item.", NamedTextColor.GRAY)));
        }
    }

    public void openIdentityGui(Player player) {
        MCIdentityProvider.instance.getSession(player.getUniqueId()).thenAccept(optSession -> {
            player.getScheduler().run(plugin, task -> {
                if (!player.isOnline()) return;

                Inventory gui = Bukkit.createInventory(player, 9, Component.translatable("mcidentity.gui.title", "Identity GUI"));
                
                ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                ItemMeta meta = blackGlass.getItemMeta();
                if (meta != null) {
                    meta.displayName(Component.empty());
                    blackGlass.setItemMeta(meta);
                }

                for (int i = 0; i < 9; i++) {
                    if (i == 4 || i == 6 || i == 2) continue;
                    gui.setItem(i, blackGlass);
                }

                gui.setItem(6, createCustomHead(HeadTexture.RUNE_GUI, "mcidentity.gui.button.rune", "Rune GUI"));
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

                player.openInventory(gui);
            }, null);
        });
    }

    public void openIdentityAbilityGui(Player player) {
        MCIdentityProvider.instance.getSession(player.getUniqueId()).thenAccept(optSession -> {
            if (optSession.isEmpty()) {
                player.getScheduler().run(plugin, t -> {
                    if (player.isOnline()) player.sendMessage(Component.text("You must have an active identity session!", NamedTextColor.RED));
                }, null);
                return;
            }

            UUID identityUuid = optSession.get().getIdentityUniqueId();
            
            // Fix: Store future references to prevent re-querying the database on .join()
            CompletableFuture<Map<Integer, String>> equippedFuture = MCIdentityProvider.instance.getEquippedAbilities(identityUuid);
            CompletableFuture<Collection<Ability>> abilitiesFuture = MCIdentityProvider.instance.getAbilities();
            
            CompletableFuture.allOf(equippedFuture, abilitiesFuture).thenAccept(v -> {
                player.getScheduler().run(plugin, task -> {
                    if (!player.isOnline()) return;

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
                                ItemStack limeGlass = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                                ItemMeta limeMeta = limeGlass.getItemMeta();
                                if (limeMeta != null) {
                                    limeMeta.displayName(Component.text(abilityNameMap.get(abilityCode), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
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

    public void openIdentityRuneGui(Player player) {
        MCIdentityProvider.instance.getSession(player.getUniqueId()).thenAccept(optSession -> {
            if (optSession.isEmpty()) {
                player.getScheduler().run(plugin, t -> {
                    if (player.isOnline()) player.sendMessage(Component.text("You must have an active identity session!", NamedTextColor.RED));
                }, null);
                return;
            }

            UUID identityUuid = optSession.get().getIdentityUniqueId();
            
            // Fix: Prevent double database querying
            CompletableFuture<Map<Integer, String>> equippedFuture = MCIdentityProvider.instance.getRunes(identityUuid);
            CompletableFuture<Collection<Rune>> runesFuture = MCIdentityProvider.instance.getRunes();
            
            CompletableFuture.allOf(equippedFuture, runesFuture).thenAccept(v -> {
                player.getScheduler().run(plugin, task -> {
                    if (!player.isOnline()) return;

                    Map<Integer, String> equipped = equippedFuture.join();
                    Collection<Rune> allRunes = runesFuture.join();
                    
                    Map<String, String> runeNameMap = allRunes.stream()
                            .collect(Collectors.toMap(Rune::getSystemCode, Rune::getDisplayName));

                    Inventory gui = Bukkit.createInventory(player, 54, Component.translatable("mcidentity.gui.rune.title", "Rune GUI"));
                    
                    ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                    ItemMeta blackMeta = blackGlass.getItemMeta();
                    if (blackMeta != null) {
                        blackMeta.displayName(Component.empty());
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
                                ItemStack limeGlass = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                                ItemMeta limeMeta = limeGlass.getItemMeta();
                                if (limeMeta != null) {
                                    limeMeta.displayName(Component.text(runeNameMap.get(runeCode), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                                    limeGlass.setItemMeta(limeMeta);
                                }
                                gui.setItem(i, limeGlass);
                            } else {
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

                    player.openInventory(gui);
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
        MCIdentityProvider.instance.getIdentityKey(uuid).thenAccept(optKey -> {
            if (optKey.isEmpty()) {
                player.getScheduler().run(plugin, task -> {
                    if (player.isOnline()) player.sendMessage(Component.text("Identity data not found in database.", NamedTextColor.RED));
                }, null);
                return;
            }

            IdentityKey key = optKey.get();
            
            // Fix: Optimize by performing stats and props queries in parallel
            CompletableFuture<Optional<IdentityProperties>> propsFuture = MCIdentityProvider.instance.getProperties(uuid);
            CompletableFuture<Optional<IdentityStats>> statsFuture = MCIdentityProvider.instance.getStats(uuid);
            
            CompletableFuture.allOf(propsFuture, statsFuture).thenAccept(v -> {
                Optional<IdentityProperties> optProps = propsFuture.join();
                Optional<IdentityStats> optStats = statsFuture.join();
                
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
    }

    private void renderIdentityInfo(Player player, UUID uuid, IdentityKey key, IdentityProperties props, IdentityStats stats) {
        player.sendMessage(Component.text("--- Identity Data ---", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("UUID: ", NamedTextColor.GRAY).append(Component.text(uuid.toString(), NamedTextColor.WHITE)));
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

    private void createIdentityItem(Player player, Profession profession, String customTexture) {
        UUID identityUuid = UUID.randomUUID();
        String professionCode = profession.getSystemCode();
        String displayName = profession.getDisplayName();
        
        IdentityKey key = new IdentityKey() {
            @Override public UUID getUniqueId() { return identityUuid; }
            @Override public String getProfessionCode() { return professionCode; }
        };

        IdentityProperties props = new IdentityProperties() {
            @Override public int getLevel() { return 1; }
            @Override public int getExp() { return 0; }
            @Override public int getStatPoints() { return 0; }
            @Override public String getHeadText() { return null; }
            @Override public String getModel() { return null; }
        };

        // Distribute 100 points randomly among 6 stats using stars and bars approach
        java.util.Random random = new java.util.Random();
        int[] points = new int[5];
        for (int i = 0; i < 5; i++) {
            points[i] = random.nextInt(101);
        }
        java.util.Arrays.sort(points);

        final int s1 = points[0];
        final int s2 = points[1] - points[0];
        final int s3 = points[2] - points[1];
        final int s4 = points[3] - points[2];
        final int s5 = points[4] - points[3];
        final int s6 = 100 - points[4];

        IdentityStats stats = new IdentityStats() {
            @Override public int getEnergy() { return 100; }
            @Override public int getHealth() { return 20; }
            @Override public int getStrength() { return s1; }
            @Override public int getDefense() { return s2; }
            @Override public int getWisdom() { return s3; }
            @Override public int getLuck() { return s4; }
            @Override public int getAgility() { return s5; }
            @Override public int getResilience() { return s6; }
        };

        MCIdentityProvider.instance.createIdentity(key, props, stats).thenAccept(v -> {
            player.getScheduler().run(plugin, task -> {
                if (!player.isOnline()) return;

                ItemStack item = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                if (meta != null) {
                    String activeTexture = customTexture;
                    
                    if (activeTexture == null && professionCode.equalsIgnoreCase("tanker")) {
                        List<String> textures = HeadTexture.TANKER_DEFAULTS;
                        if (!textures.isEmpty()) {
                            activeTexture = textures.get(new java.util.Random().nextInt(textures.size()));
                        }
                    }

                    if (activeTexture != null) {
                        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
                        profile.setProperty(new ProfileProperty("textures", activeTexture));
                        meta.setPlayerProfile(profile);
                    }

                    NamedTextColor[] suitableColors = {
                        NamedTextColor.AQUA, NamedTextColor.BLUE, NamedTextColor.DARK_AQUA,
                        NamedTextColor.DARK_BLUE, NamedTextColor.DARK_GREEN, NamedTextColor.DARK_PURPLE,
                        NamedTextColor.DARK_RED, NamedTextColor.GOLD, NamedTextColor.GREEN,
                        NamedTextColor.LIGHT_PURPLE, NamedTextColor.RED, NamedTextColor.WHITE,
                        NamedTextColor.YELLOW
                    };
                    NamedTextColor randomNameColor = suitableColors[random.nextInt(suitableColors.length)];
                    NamedTextColor randomLevelColor = suitableColors[random.nextInt(suitableColors.length)];

                    meta.displayName(
                        Component.text("[", NamedTextColor.GRAY)
                            .append(Component.text(displayName, randomNameColor))
                            .append(Component.text("] [", NamedTextColor.GRAY))
                            .append(Component.text(props.getLevel(), randomLevelColor))
                            .append(Component.text("]", NamedTextColor.GRAY))
                            .decoration(TextDecoration.ITALIC, false)
                    );
                    meta.getPersistentDataContainer().set(identityKey, PersistentDataType.STRING, identityUuid.toString());
                    meta.getPersistentDataContainer().set(professionKey, PersistentDataType.STRING, professionCode);
                    item.setItemMeta(meta);
                }

                player.getInventory().addItem(item);
                player.sendMessage(Component.text("Identity item given!", NamedTextColor.GREEN));
            }, null);
        });
    }
}

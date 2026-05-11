package io.gitlab.mcoriax.foliamc.listeners;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.bukkit.event.IdentityCreationEvent;
import io.gitlab.mcoriax.bukkit.event.IdentityDeathEvent;
import io.gitlab.mcoriax.bukkit.event.IdentityEquipEvent;
import io.gitlab.mcoriax.bukkit.event.IdentityUnequipEvent;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import io.gitlab.mcoriax.foliamc.commands.MCIdentityCommand;
import io.gitlab.mcoriax.foliamc.util.HeadTexture;
import io.gitlab.mcoriax.foliamc.util.ItemStackSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MCIdentityListener implements Listener {

    private final MCIdentityPlugin plugin;
    private final NamespacedKey identityKey;
    private final NamespacedKey professionKey;
    private final NamespacedKey textureKey;
    private final NamespacedKey unidentifiedKey;

    public MCIdentityListener(MCIdentityPlugin plugin) {
        this.plugin = plugin;
        this.identityKey = new NamespacedKey(plugin, "identity_uuid");
        this.professionKey = new NamespacedKey(plugin, "profession_code");
        this.textureKey = new NamespacedKey(plugin, "identity_texture");
        this.unidentifiedKey = new NamespacedKey(plugin, "is_unidentified");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        MCIdentityProvider.instance.getSession(player.getUniqueId()).thenAccept(optSession -> {
            optSession.ifPresent(session -> {
                MCIdentityProvider.instance.loadSessionToRam(player.getUniqueId(), session.getIdentityUniqueId(), session.getContentKey()).thenRun(() -> {
                    // Fix: Run player operations safely on Folia's region thread to avoid AsyncTrackerUpdate error
                    player.getScheduler().run(plugin, task -> {
                        if (!plugin.isEnabled() || !player.isOnline()) return;
                        player.sendMessage(Component.text("You are currently wearing an identity.", NamedTextColor.GREEN));
                    }, null);
                });
            });
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        MCIdentityManager.removeSession(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        String uuidStr = meta.getPersistentDataContainer().get(identityKey, PersistentDataType.STRING);
        String professionCode = meta.getPersistentDataContainer().get(professionKey, PersistentDataType.STRING);

        if (uuidStr == null && professionCode == null) {
            return;
        }

        if (MCIdentityManager.getSession(player.getUniqueId()).isPresent()) {
            player.sendMessage(Component.text("You are already wearing an identity!", NamedTextColor.RED));
            return;
        }

        if (plugin.isOnSwapCooldown(player.getUniqueId())) {
            String remaining = plugin.formatTime(plugin.getRemainingSwapCooldownMillis(player.getUniqueId()));
            player.sendMessage(Component.text("You must wait " + remaining + " before swapping identities!", NamedTextColor.RED));
            return;
        }

        if (plugin.isEquipPending(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        plugin.setEquipPending(player.getUniqueId(), true);

        ItemStack itemToEquip = item.clone();
        itemToEquip.setAmount(1);

        if (player.getInventory().getItemInMainHand().isSimilar(itemToEquip)) {
            player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
        } else {
            player.sendMessage(Component.text("Failed to verify item in hand.", NamedTextColor.RED));
            return;
        }

        // Handle Unbound State (Profession present but no UUID)
        if (uuidStr == null && professionCode != null) {
            finalizeUnboundIdentity(player, itemToEquip, professionCode, false);
            return;
        }

        UUID identityUuid;
        try {
            identityUuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            refundItemSafely(player, itemToEquip, "This identity item is corrupted!");
            return;
        }

        initializeIdentity(player, identityUuid, professionCode, itemToEquip, false);
    }

    private void initializeIdentity(Player player, UUID identityUuid, String professionCode, ItemStack itemToEquip, boolean reopenGui) {
        MCIdentityProvider.instance.getIdentityKey(identityUuid).thenAccept(optKey -> {
            if (optKey.isEmpty()) {
                // Not in DB yet, need to initialize
                MCIdentityProvider.instance.getProfessions().thenAccept(professions -> {
                    if (professions.isEmpty()) {
                        refundItemSafely(player, itemToEquip, "No professions available!");
                        return;
                    }

                    Profession profession;
                    if (professionCode == null || professionCode.equalsIgnoreCase("random")) {
                        // Random item or "random" code: pick one
                        List<Profession> profList = new ArrayList<>(professions);
                        profession = profList.get(new Random().nextInt(profList.size()));
                    } else {
                        // Profession is pre-assigned, use it
                        Optional<Profession> optProf = professions.stream()
                                .filter(p -> p.getSystemCode().equalsIgnoreCase(professionCode))
                                .findFirst();
                        if (optProf.isEmpty()) {
                            refundItemSafely(player, itemToEquip, "Profession '" + professionCode + "' not found!");
                            return;
                        }
                        profession = optProf.get();
                    }

                    String actualProfCode = profession.getSystemCode();
                    String displayName = profession.getDisplayName();

                    // Update item meta if it's unidentified
                    player.getScheduler().run(plugin, task -> {
                        if (!plugin.isEnabled() || !player.isOnline()) return;

                        SkullMeta skullMeta = (SkullMeta) itemToEquip.getItemMeta();
                        String finalTexture = null;
                        if (skullMeta != null) {
                            // Robust check for unidentified using the PDC flag
                            boolean isUnidentified = skullMeta.getPersistentDataContainer().has(unidentifiedKey, PersistentDataType.BYTE);
                            
                            // Check if current texture is the UNIDENTIFIED placeholder as a fallback
                            PlayerProfile profile = skullMeta.getPlayerProfile();
                            boolean isUnidentifiedTexture = false;
                            if (profile != null) {
                                isUnidentifiedTexture = profile.getProperties().stream()
                                        .anyMatch(p -> p.getName().equals("textures") && p.getValue().equals(HeadTexture.getUnidentified(plugin)));
                            }

                            if (isUnidentified || isUnidentifiedTexture) {
                                List<String> textures = new ArrayList<>(profession.getSkins().values());
                                if (!textures.isEmpty()) {
                                    String activeTexture = textures.get(new Random().nextInt(textures.size()));
                                    PlayerProfile newProfile = Bukkit.createProfile(UUID.randomUUID());
                                    newProfile.setProperty(new ProfileProperty("textures", activeTexture));
                                    skullMeta.setPlayerProfile(newProfile);
                                    finalTexture = activeTexture;
                                }

                                Random random = new Random();
                                NamedTextColor[] suitableColors = {
                                    NamedTextColor.AQUA, NamedTextColor.BLUE, NamedTextColor.DARK_AQUA,
                                    NamedTextColor.DARK_BLUE, NamedTextColor.DARK_GREEN, NamedTextColor.DARK_PURPLE,
                                    NamedTextColor.DARK_RED, NamedTextColor.GOLD, NamedTextColor.GREEN,
                                    NamedTextColor.LIGHT_PURPLE, NamedTextColor.RED, NamedTextColor.WHITE,
                                    NamedTextColor.YELLOW
                                };
                                NamedTextColor randomNameColor = suitableColors[random.nextInt(suitableColors.length)];
                                NamedTextColor randomLevelColor = suitableColors[random.nextInt(suitableColors.length)];

                                skullMeta.displayName(
                                    Component.text("[", NamedTextColor.GRAY)
                                        .append(Component.text(displayName, randomNameColor))
                                        .append(Component.text("] [", NamedTextColor.GRAY))
                                        .append(Component.text(1, randomLevelColor))
                                        .append(Component.text("]", NamedTextColor.GRAY))
                                        .decoration(TextDecoration.ITALIC, false)
                                );
                                skullMeta.getPersistentDataContainer().set(professionKey, PersistentDataType.STRING, actualProfCode);
                                // Remove the unidentified flag
                                skullMeta.getPersistentDataContainer().remove(unidentifiedKey);
                                itemToEquip.setItemMeta(skullMeta);
                            } else if (profile != null) {
                                finalTexture = profile.getProperties().stream()
                                        .filter(p -> p.getName().equals("textures"))
                                        .map(ProfileProperty::getValue)
                                        .findFirst().orElse(null);
                            }
                        }

                        // Re-serialize the updated item
                        String updatedBase64 = ItemStackSerializer.serialize(itemToEquip);

                        createIdentityInDB(identityUuid, actualProfCode, finalTexture).thenAccept(v -> {
                            // Fire IdentityCreationEvent
                            player.getScheduler().run(plugin, t -> {
                                if (!plugin.isEnabled()) return;
                                
                                MCIdentityProvider.instance.getStats(identityUuid).thenAccept(optStats -> {
                                    MCIdentityProvider.instance.getProperties(identityUuid).thenAccept(optProps -> {
                                        if (optStats.isPresent() && optProps.isPresent()) {
                                            IdentityKey key = new IdentityKey() {
                                                @Override public UUID getUniqueId() { return identityUuid; }
                                                @Override public String getProfessionCode() { return actualProfCode; }
                                            };
                                            IdentityCreationEvent event = new IdentityCreationEvent(key, optProps.get(), optStats.get());
                                            Bukkit.getPluginManager().callEvent(event);
                                        }
                                    });
                                });
                            }, null);
                            
                            processEquip(player, identityUuid, itemToEquip, updatedBase64, reopenGui);
                        }).exceptionally(ex -> {
                            refundItemSafely(player, itemToEquip, "Failed to register identity: " + ex.getMessage());
                            return null;
                        });
                    }, null);
                });
            } else {
                // Identity already in DB, ensure the item meta is updated if it was random
                IdentityKey key = optKey.get();
                String dbProfCode = key.getProfessionCode();
                
                player.getScheduler().run(plugin, task -> {
                    if (!plugin.isEnabled() || !player.isOnline()) return;

                    SkullMeta skullMeta = (SkullMeta) itemToEquip.getItemMeta();
                    if (skullMeta != null) {
                        boolean isUnidentified = skullMeta.getPersistentDataContainer().has(unidentifiedKey, PersistentDataType.BYTE);
                        String currentProf = skullMeta.getPersistentDataContainer().get(professionKey, PersistentDataType.STRING);
                        
                        if (isUnidentified || "random".equalsIgnoreCase(currentProf)) {
                            // Update item to match DB
                            MCIdentityProvider.instance.getProfessions().thenAccept(professions -> {
                                Optional<Profession> optProf = professions.stream()
                                        .filter(p -> p.getSystemCode().equalsIgnoreCase(dbProfCode))
                                        .findFirst();
                                
                                if (optProf.isPresent()) {
                                    Profession profession = optProf.get();
                                    player.getScheduler().run(plugin, innerTask -> {
                                        if (!plugin.isEnabled() || !player.isOnline()) return;
                                        
                                        // Update display name and remove unidentified flag
                                        Random random = new Random();
                                        NamedTextColor[] suitableColors = {
                                            NamedTextColor.AQUA, NamedTextColor.BLUE, NamedTextColor.DARK_AQUA,
                                            NamedTextColor.DARK_BLUE, NamedTextColor.DARK_GREEN, NamedTextColor.DARK_PURPLE,
                                            NamedTextColor.DARK_RED, NamedTextColor.GOLD, NamedTextColor.GREEN,
                                            NamedTextColor.LIGHT_PURPLE, NamedTextColor.RED, NamedTextColor.WHITE,
                                            NamedTextColor.YELLOW
                                        };
                                        NamedTextColor randomNameColor = suitableColors[random.nextInt(suitableColors.length)];
                                        NamedTextColor randomLevelColor = suitableColors[random.nextInt(suitableColors.length)];

                                        skullMeta.displayName(
                                            Component.text("[", NamedTextColor.GRAY)
                                                .append(Component.text(profession.getDisplayName(), randomNameColor))
                                                .append(Component.text("] [", NamedTextColor.GRAY))
                                                .append(Component.text(1, randomLevelColor))
                                                .append(Component.text("]", NamedTextColor.GRAY))
                                                .decoration(TextDecoration.ITALIC, false)
                                        );
                                        
                                        // Update texture if unidentified
                                        List<String> textures = new ArrayList<>(profession.getSkins().values());
                                        if (!textures.isEmpty()) {
                                            String activeTexture = textures.get(new Random().nextInt(textures.size()));
                                            PlayerProfile newProfile = Bukkit.createProfile(UUID.randomUUID());
                                            newProfile.setProperty(new ProfileProperty("textures", activeTexture));
                                            skullMeta.setPlayerProfile(newProfile);
                                        }

                                        skullMeta.getPersistentDataContainer().set(professionKey, PersistentDataType.STRING, dbProfCode);
                                        skullMeta.getPersistentDataContainer().remove(unidentifiedKey);
                                        itemToEquip.setItemMeta(skullMeta);
                                        
                                        String base64 = ItemStackSerializer.serialize(itemToEquip);
                                        processEquip(player, identityUuid, itemToEquip, base64, reopenGui);
                                    }, null);
                                } else {
                                    // Just equip with current if profession missing
                                    String base64 = ItemStackSerializer.serialize(itemToEquip);
                                    processEquip(player, identityUuid, itemToEquip, base64, reopenGui);
                                }
                            });
                            return;
                        }
                    }
                    
                    String base64 = ItemStackSerializer.serialize(itemToEquip);
                    processEquip(player, identityUuid, itemToEquip, base64, reopenGui);
                }, null);
            }
        }).exceptionally(ex -> {
            refundItemSafely(player, itemToEquip, "Failed to fetch identity data: " + ex.getMessage());
            return null;
        });
    }

    private void finalizeUnboundIdentity(Player player, ItemStack itemToEquip, String professionCode, boolean reopenGui) {
        MCIdentityProvider.instance.getProfessions().thenAccept(professions -> {
            Optional<Profession> optProf = professions.stream().filter(p -> p.getSystemCode().equalsIgnoreCase(professionCode)).findFirst();
            if (optProf.isEmpty()) {
                refundItemSafely(player, itemToEquip, "Error: Profession '" + professionCode + "' no longer exists!");
                return;
            }

            Profession profession = optProf.get();
            UUID identityUuid = UUID.randomUUID();
            ItemMeta meta = itemToEquip.getItemMeta();
            final String texture = meta.getPersistentDataContainer().get(textureKey, PersistentDataType.STRING);

            // Calculate stats now (at first use)
            Map<String, Integer> baseStats = profession.getBaseStats();
            String statsRandomConfig = profession.getStatsRandom();
            Random random = new Random();

            int pointsToDistribute = 0;
            try {
                if (statsRandomConfig.contains("~")) {
                    String[] parts = statsRandomConfig.split("~");
                    if (parts.length == 2) {
                        int min = Integer.parseInt(parts[0].trim());
                        int max = Integer.parseInt(parts[1].trim());
                        pointsToDistribute = random.nextInt((max - min) + 1) + min;
                    }
                } else {
                    pointsToDistribute = Integer.parseInt(statsRandomConfig.trim());
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid stats_random configuration for " + professionCode + ": " + statsRandomConfig);
            }

            int[] points = new int[5];
            if (pointsToDistribute > 0) {
                for (int i = 0; i < 5; i++) {
                    points[i] = random.nextInt(pointsToDistribute + 1);
                }
                Arrays.sort(points);
            }

            final int s1 = points[0];
            final int s2 = points[1] - points[0];
            final int s3 = points[2] - points[1];
            final int s4 = points[3] - points[2];
            final int s5 = points[4] - points[3];
            final int s6 = pointsToDistribute - points[4];

            final int energyVal = baseStats.getOrDefault("energy", 100);
            final int healthVal = baseStats.getOrDefault("health", 20);
            final int foodVal = baseStats.getOrDefault("food", 20);
            final int strengthVal = baseStats.getOrDefault("strength", 0) + s1;
            final int defenseVal = baseStats.getOrDefault("defense", 0) + s2;
            final int wisdomVal = baseStats.getOrDefault("wisdom", 0) + s3;
            final int luckVal = baseStats.getOrDefault("luck", 0) + s4;
            final int agilityVal = baseStats.getOrDefault("agility", 0) + s5;
            final int resilienceVal = baseStats.getOrDefault("resilience", 0) + s6;

            IdentityKey key = new IdentityKey() {
                @Override public UUID getUniqueId() { return identityUuid; }
                @Override public String getProfessionCode() { return professionCode; }
            };

            IdentityProperties props = new IdentityProperties() {
                private String skin;
                private String aura;
                @Override public int getLevel() { return 1; }
                @Override public int getExp() { return 0; }
                @Override public int getStatPoints() { return 0; }
                @Override public String getHeadText() { return texture; }
                @Override public String getSkin() { return skin; }
                @Override public void setSkin(String skin) { this.skin = skin; }
                @Override public String getAura() { return aura; }
                @Override public void setAura(String aura) { this.aura = aura; }
            };

            IdentityStats stats = new IdentityStats() {
                @Override public int getEnergy() { return energyVal; }
                @Override public int getHealth() { return healthVal; }
                @Override public int getFood() { return foodVal; }
                @Override public int getStrength() { return strengthVal; }
                @Override public int getDefense() { return defenseVal; }
                @Override public int getWisdom() { return wisdomVal; }
                @Override public int getLuck() { return luckVal; }
                @Override public int getAgility() { return agilityVal; }
                @Override public int getResilience() { return resilienceVal; }
            };

            MCIdentityProvider.instance.createIdentity(key, props, stats).thenAccept(v -> {
                // Fire IdentityCreationEvent
                player.getScheduler().run(plugin, t -> {
                    if (!plugin.isEnabled()) return;
                    IdentityCreationEvent event = new IdentityCreationEvent(key, props, stats);
                    Bukkit.getPluginManager().callEvent(event);
                }, null);

                player.getScheduler().run(plugin, task -> {
                    if (!plugin.isEnabled() || !player.isOnline()) return;

                    // Update item with the new UUID
                    ItemMeta updatedMeta = itemToEquip.getItemMeta();
                    updatedMeta.getPersistentDataContainer().set(identityKey, PersistentDataType.STRING, identityUuid.toString());
                    updatedMeta.getPersistentDataContainer().remove(textureKey);
                    itemToEquip.setItemMeta(updatedMeta);
                    
                    String updatedBase64 = ItemStackSerializer.serialize(itemToEquip);
                    processEquip(player, identityUuid, itemToEquip, updatedBase64, reopenGui);
                }, null);
            }).exceptionally(ex -> {
                refundItemSafely(player, itemToEquip, "Failed to register identity: " + ex.getMessage());
                return null;
            });
        });
    }

    private void refundItemSafely(Player player, ItemStack itemToEquip, String message) {
        plugin.setEquipPending(player.getUniqueId(), false);
        player.getScheduler().run(plugin, task -> {
            if (!plugin.isEnabled() || !player.isOnline()) return;
            player.sendMessage(Component.text(message, NamedTextColor.RED));
            player.getInventory().addItem(itemToEquip).values()
                    .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }, null);
    }

    private void processEquip(Player player, UUID identityUuid, ItemStack itemToEquip, String base64, boolean reopenGui) {
        player.getScheduler().run(plugin, task -> {
            if (!plugin.isEnabled() || !player.isOnline()) {
                plugin.setEquipPending(player.getUniqueId(), false);
                return;
            }
            
            IdentityEquipEvent equipEvent = new IdentityEquipEvent(player, identityUuid, itemToEquip);
            Bukkit.getPluginManager().callEvent(equipEvent);
            
            if (equipEvent.isCancelled()) {
                refundItemSafely(player, itemToEquip, "Equipping was cancelled.");
                return;
            }

            MCIdentityProvider.instance.setActiveIdentity(player.getUniqueId(), identityUuid, base64).thenAccept(v -> {
                plugin.setEquipPending(player.getUniqueId(), false);
                plugin.setSwapCooldown(player.getUniqueId());
                player.getScheduler().run(plugin, innerTask -> {
                    if (!plugin.isEnabled() || !player.isOnline()) return;
                    player.sendMessage(Component.text("Identity equipped!", NamedTextColor.GREEN));
                    MCIdentityProvider.instance.logAction(player.getUniqueId(), identityUuid, ActionType.EQUIP);
                    
                    if (reopenGui) {
                        MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
                        if (cmd != null) cmd.openIdentityGui(player);
                    }
                }, null);
            }).exceptionally(ex -> {
                plugin.setEquipPending(player.getUniqueId(), false);
                refundItemSafely(player, itemToEquip, "Failed to equip identity: " + ex.getMessage());
                return null;
            });
        }, null);
    }

    private CompletableFuture<Void> createIdentityInDB(UUID identityUuid, String professionCode, String headTexture) {
        IdentityKey key = new IdentityKey() {
            @Override public UUID getUniqueId() { return identityUuid; }
            @Override public String getProfessionCode() { return professionCode; }
        };

        IdentityProperties props = new IdentityProperties() {
            private String skin;
            private String aura;
            @Override public int getLevel() { return 1; }
            @Override public int getExp() { return 0; }
            @Override public int getStatPoints() { return 0; }
            @Override public String getHeadText() { return headTexture; }
            @Override public String getSkin() { return skin; }
            @Override public void setSkin(String skin) { this.skin = skin; }
            @Override public String getAura() { return aura; }
            @Override public void setAura(String aura) { this.aura = aura; }
        };

        // Distribute 100 points randomly among 6 stats using stars and bars approach
        Random random = new Random();
        int[] points = new int[5];
        for (int i = 0; i < 5; i++) {
            points[i] = random.nextInt(101);
        }
        Arrays.sort(points);

        final int s1 = points[0];
        final int s2 = points[1] - points[0];
        final int s3 = points[2] - points[1];
        final int s4 = points[3] - points[2];
        final int s5 = points[4] - points[3];
        final int s6 = 100 - points[4];

        IdentityStats stats = new IdentityStats() {
            @Override public int getEnergy() { return 100; }
            @Override public int getHealth() { return 20; }
            @Override public int getFood() { return 20; }
            @Override public int getStrength() { return s1; }
            @Override public int getDefense() { return s2; }
            @Override public int getWisdom() { return s3; }
            @Override public int getLuck() { return s4; }
            @Override public int getAgility() { return s5; }
            @Override public int getResilience() { return s6; }
        };

        return MCIdentityProvider.instance.createIdentity(key, props, stats);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Component title = event.getView().title();
        if (title.equals(Component.translatable("mcidentity.gui.title", "Identity GUI"))) {
            handleIdentityGuiClick(event);
        } else if (title.equals(Component.translatable("mcidentity.gui.rune.title", "Rune GUI"))) {
            handleRuneGuiClick(event);
        } else if (title.equals(Component.translatable("mcidentity.gui.ability.title", "Ability GUI"))) {
            handleAbilityGuiClick(event);
        } else if (title.equals(Component.translatable("mcidentity.gui.cosmetic.title", "Cosmetic GUI"))) {
            handleCosmeticGuiClick(event);
        }
    }

    private void handleCosmeticGuiClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == 0) {
            MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
            if (cmd != null) {
                cmd.openIdentityGui(player);
            }
        }
    }

    private void handleAbilityGuiClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == 45) {
            MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
            if (cmd != null) {
                cmd.openIdentityGui(player);
            }
        }
    }

    private void handleRuneGuiClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == 45) {
            MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
            if (cmd != null) {
                cmd.openIdentityGui(player);
            }
        }
    }

    private void handleIdentityGuiClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        InventoryAction action = event.getAction();
        int slot = event.getRawSlot();

        if (slot >= 0 && slot < event.getView().getTopInventory().getSize()) {
            event.setCancelled(true);

            if (slot == 6) {
                if (MCIdentityManager.getSession(player.getUniqueId()).isEmpty()) {
                    player.sendMessage(Component.text("You must be wearing an identity to open the Rune GUI!", NamedTextColor.RED));
                    return;
                }
                MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
                if (cmd != null) {
                    cmd.openIdentityRuneGui(player);
                }
                return;
            }

            if (slot == 2) {
                if (MCIdentityManager.getSession(player.getUniqueId()).isEmpty()) {
                    player.sendMessage(Component.text("You must be wearing an identity to open the Ability GUI!", NamedTextColor.RED));
                    return;
                }
                MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
                if (cmd != null) {
                    cmd.openIdentityAbilityGui(player);
                }
                return;
            }

            if (slot == 4) {
                handleSlot4Click(event, player);
                return;
            }

            if (slot == 8) {
                if (MCIdentityManager.getSession(player.getUniqueId()).isEmpty()) {
                    player.sendMessage(Component.text("You must be wearing an identity to open the Cosmetic GUI!", NamedTextColor.RED));
                    return;
                }
                MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
                if (cmd != null) {
                    cmd.openCosmeticGui(player);
                }
                return;
            }
        } else {
            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
            }
        }
    }

    private void handleSlot4Click(InventoryClickEvent event, Player player) {
        event.setCancelled(true); 

        ItemStack cursorItem = event.getCursor();
        ItemStack currentItem = event.getCurrentItem();
        InventoryAction action = event.getAction();

        if (isPlaceAction(action)) {
            if (isIdentityKey(cursorItem)) {
                if (MCIdentityManager.getSession(player.getUniqueId()).isPresent()) {
                    player.sendMessage(Component.text("Please unequip your current identity first!", NamedTextColor.RED));
                    return;
                }

                if (plugin.isOnSwapCooldown(player.getUniqueId())) {
                    String remaining = plugin.formatTime(plugin.getRemainingSwapCooldownMillis(player.getUniqueId()));
                    player.sendMessage(Component.text("You must wait " + remaining + " before swapping identities!", NamedTextColor.RED));
                    return;
                }

                if (plugin.isEquipPending(player.getUniqueId())) {
                    return;
                }

                event.setCancelled(true);
                plugin.setEquipPending(player.getUniqueId(), true);

                ItemStack itemToEquip = cursorItem.clone();
                itemToEquip.setAmount(1);
                
                ItemMeta meta = itemToEquip.getItemMeta();
                String uuidStr = meta.getPersistentDataContainer().get(identityKey, PersistentDataType.STRING);
                String professionCode = meta.getPersistentDataContainer().get(professionKey, PersistentDataType.STRING);

                if (cursorItem.getAmount() > 1) {
                    cursorItem.setAmount(cursorItem.getAmount() - 1);
                } else {
                    event.getWhoClicked().setItemOnCursor(null);
                }

                // Handle Unbound State
                if (uuidStr == null && professionCode != null) {
                    finalizeUnboundIdentity(player, itemToEquip, professionCode, true);
                    return;
                }

                UUID identityUuid;
                try {
                    identityUuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException e) {
                    refundItemSafely(player, itemToEquip, "This identity item is corrupted!");
                    return;
                }

                initializeIdentity(player, identityUuid, professionCode, itemToEquip, true);

                if (cursorItem.getAmount() > 1) {
                    cursorItem.setAmount(cursorItem.getAmount() - 1);
                } else {
                    event.getWhoClicked().setItemOnCursor(null);
                }
            } else if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                player.sendMessage(Component.text("Only identity keys can be placed here!", NamedTextColor.RED));
            }
        } else if (isTakeAction(action)) {
            if (isIdentityKey(currentItem)) {
                if (plugin.isOnSwapCooldown(player.getUniqueId())) {
                    String remaining = plugin.formatTime(plugin.getRemainingSwapCooldownMillis(player.getUniqueId()));
                    player.sendMessage(Component.text("You must wait " + remaining + " before swapping identities!", NamedTextColor.RED));
                    return;
                }

                ItemStack itemToTake = currentItem.clone();
                
                String uuidStr = itemToTake.getItemMeta().getPersistentDataContainer().get(identityKey, PersistentDataType.STRING);
                UUID identityUuid = UUID.fromString(uuidStr);

                IdentityUnequipEvent unequipEvent = new IdentityUnequipEvent(player, identityUuid);
                Bukkit.getPluginManager().callEvent(unequipEvent);

                if (unequipEvent.isCancelled()) {
                    return;
                }
                
                Map<Integer, ItemStack> leftOver = player.getInventory().addItem(itemToTake);
                leftOver.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                
                MCIdentityProvider.instance.setActiveIdentity(player.getUniqueId(), null, null).thenAccept(v -> {
                    plugin.setSwapCooldown(player.getUniqueId());
                    // Fix: Fire Folia Scheduler before sending messages
                    player.getScheduler().run(plugin, task -> {
                        if (!plugin.isEnabled() || !player.isOnline()) return;
                        player.sendMessage(Component.text("Identity unequipped via GUI.", NamedTextColor.YELLOW));
                        MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
                        if (cmd != null) cmd.openIdentityGui(player);
                    }, null);
                    // Safe to call async
                    MCIdentityProvider.instance.logAction(player.getUniqueId(), identityUuid, ActionType.UNEQUIP);
                });
            }
        }
    }

    private boolean isPlaceAction(InventoryAction action) {
        return action == InventoryAction.PLACE_ALL || action == InventoryAction.PLACE_ONE || 
               action == InventoryAction.PLACE_SOME || action == InventoryAction.SWAP_WITH_CURSOR;
    }

    private boolean isTakeAction(InventoryAction action) {
        return action == InventoryAction.PICKUP_ALL || action == InventoryAction.PICKUP_HALF || 
               action == InventoryAction.PICKUP_ONE || action == InventoryAction.PICKUP_SOME || 
               action == InventoryAction.MOVE_TO_OTHER_INVENTORY;
    }

    private boolean isIdentityKey(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(identityKey, PersistentDataType.STRING) || 
               meta.getPersistentDataContainer().has(professionKey, PersistentDataType.STRING) ||
               meta.getPersistentDataContainer().has(unidentifiedKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (isIdentityKey(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("You cannot place the Identity Key as a block!", NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            plugin.getExperienceManager().awardExp(player, "deal_damage", event.getFinalDamage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player player = event.getEntity().getKiller();
            plugin.getExperienceManager().awardExp(player, "kill_mob", event.getEntity().getType().name().toLowerCase());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLocation = player.getLocation().clone();
        UUID playerUuid = player.getUniqueId();

        MCIdentityProvider.instance.getSession(playerUuid).thenAccept(optSession -> {
            optSession.ifPresent(session -> {
                UUID identityUuid = session.getIdentityUniqueId();
                String base64 = session.getContentKey();

                // Clear swap cooldown if player dies while wearing an identity
                plugin.clearSwapCooldown(playerUuid);

                try {
                    ItemStack identityItem = base64 != null ? ItemStackSerializer.deserialize(base64) : null;
                    
                    Bukkit.getRegionScheduler().run(plugin, deathLocation, task -> {
                        if (!plugin.isEnabled()) return;
                        IdentityDeathEvent deathEvent = new IdentityDeathEvent(player, identityUuid, identityItem);
                        deathEvent.setDrop(true); 
                        Bukkit.getPluginManager().callEvent(deathEvent);

                        if (deathEvent.isDrop()) {
                            MCIdentityProvider.instance.setActiveIdentity(playerUuid, null, null).thenAccept(v -> {
                                if (identityItem != null) {
                                    Bukkit.getRegionScheduler().run(plugin, deathLocation, dropTask -> {
                                        if (!plugin.isEnabled()) return;
                                        if (deathLocation.getWorld() != null) {
                                            deathLocation.getChunk().addPluginChunkTicket(plugin);
                                            try {
                                                deathLocation.getWorld().dropItemNaturally(deathLocation, identityItem);
                                                Player onlinePlayer = Bukkit.getPlayer(playerUuid);
                                                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                                                    onlinePlayer.sendMessage(Component.text("Your identity has been dropped due to death!", NamedTextColor.RED));
                                                }
                                            } finally {
                                                deathLocation.getChunk().removePluginChunkTicket(plugin);
                                            }
                                        }
                                    });
                                    MCIdentityProvider.instance.logAction(playerUuid, identityUuid, ActionType.DEATH_DROP);
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to process death event for " + player.getName());
                    e.printStackTrace();
                }
            });
        });
    }
}

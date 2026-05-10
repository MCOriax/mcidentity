package io.gitlab.mcoriax.foliamc.listeners;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MCIdentityListener implements Listener {

    private final MCIdentityPlugin plugin;
    private final NamespacedKey identityKey;
    private final NamespacedKey professionKey;

    public MCIdentityListener(MCIdentityPlugin plugin) {
        this.plugin = plugin;
        this.identityKey = new NamespacedKey(plugin, "identity_uuid");
        this.professionKey = new NamespacedKey(plugin, "profession_code");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        MCIdentityProvider.instance.getSession(player.getUniqueId()).thenAccept(optSession -> {
            optSession.ifPresent(session -> {
                MCIdentityProvider.instance.loadSessionToRam(player.getUniqueId(), session.getIdentityUniqueId()).thenRun(() -> {
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
        if (uuidStr == null) {
            return;
        }

        UUID identityUuid;
        try {
            identityUuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("This identity item is corrupted!", NamedTextColor.RED));
            return;
        }

        String professionCode = meta.getPersistentDataContainer().get(professionKey, PersistentDataType.STRING);

        if (MCIdentityManager.getSession(player.getUniqueId()).isPresent()) {
            player.sendMessage(Component.text("You are already wearing an identity!", NamedTextColor.RED));
            return;
        }

        event.setCancelled(true);

        ItemStack itemToEquip = item.clone();
        itemToEquip.setAmount(1);
        String base64 = ItemStackSerializer.serialize(itemToEquip);

        if (player.getInventory().getItemInMainHand().isSimilar(itemToEquip)) {
            player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
        } else {
            player.sendMessage(Component.text("Failed to verify item in hand.", NamedTextColor.RED));
            return;
        }

        MCIdentityProvider.instance.getIdentityKey(identityUuid).thenAccept(optKey -> {
            if (optKey.isEmpty()) {
                if (professionCode == null) {
                    // Random item: pick a profession and assign it
                    MCIdentityProvider.instance.getProfessions().thenAccept(professions -> {
                        if (professions.isEmpty()) {
                            refundItemSafely(player, itemToEquip, "No professions available to assign!");
                            return;
                        }

                        List<Profession> profList = professions.stream().toList();
                        Profession randomProf = profList.get(new java.util.Random().nextInt(profList.size()));
                        String newProfCode = randomProf.getSystemCode();
                        String newDisplayName = randomProf.getDisplayName();

                        // Update item meta with the new profession and textures if needed
                        player.getScheduler().run(plugin, task -> {
                            if (!plugin.isEnabled() || !player.isOnline()) return;

                            SkullMeta skullMeta = (SkullMeta) itemToEquip.getItemMeta();
                            String finalTexture = null;
                            if (skullMeta != null) {
                                // Assign texture if current texture is the UNIDENTIFIED placeholder
                                PlayerProfile profile = skullMeta.getPlayerProfile();
                                boolean isUnidentified = false;
                                if (profile != null) {
                                    isUnidentified = profile.getProperties().stream()
                                            .anyMatch(p -> p.getName().equals("textures") && p.getValue().equals(HeadTexture.getUnidentified(plugin)));
                                }

                                if (isUnidentified) {
                                    List<String> textures = HeadTexture.getProfessionDefaults(plugin, newProfCode);
                                    if (!textures.isEmpty()) {
                                        String activeTexture = textures.get(new java.util.Random().nextInt(textures.size()));
                                        PlayerProfile newProfile = Bukkit.createProfile(UUID.randomUUID());
                                        newProfile.setProperty(new ProfileProperty("textures", activeTexture));
                                        skullMeta.setPlayerProfile(newProfile);
                                        finalTexture = activeTexture;
                                    }
                                } else if (profile != null) {
                                    finalTexture = profile.getProperties().stream()
                                            .filter(p -> p.getName().equals("textures"))
                                            .map(ProfileProperty::getValue)
                                            .findFirst().orElse(null);
                                }

                                java.util.Random random = new java.util.Random();
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
                                        .append(Component.text(newDisplayName, randomNameColor))
                                        .append(Component.text("] [", NamedTextColor.GRAY))
                                        .append(Component.text(1, randomLevelColor)) // Level 1 for new identity
                                        .append(Component.text("]", NamedTextColor.GRAY))
                                        .decoration(TextDecoration.ITALIC, false)
                                );
                                skullMeta.getPersistentDataContainer().set(professionKey, PersistentDataType.STRING, newProfCode);
                                itemToEquip.setItemMeta(skullMeta);
                            }

                            // Re-serialize the updated item
                            String updatedBase64 = ItemStackSerializer.serialize(itemToEquip);

                            createIdentityInDB(identityUuid, newProfCode, finalTexture).thenAccept(v -> {
                                processEquip(player, identityUuid, itemToEquip, updatedBase64);
                            }).exceptionally(ex -> {
                                refundItemSafely(player, itemToEquip, "Failed to register identity: " + ex.getMessage());
                                return null;
                            });
                        }, null);
                    });
                    return;
                }

                // Existing profession item, extract texture
                String existingTexture = null;
                if (meta instanceof SkullMeta skullMeta) {
                    PlayerProfile profile = skullMeta.getPlayerProfile();
                    if (profile != null) {
                        existingTexture = profile.getProperties().stream()
                                .filter(p -> p.getName().equals("textures"))
                                .map(ProfileProperty::getValue)
                                .findFirst().orElse(null);
                    }
                }

                createIdentityInDB(identityUuid, professionCode, existingTexture).thenAccept(v -> {
                    processEquip(player, identityUuid, itemToEquip, base64);
                }).exceptionally(ex -> {
                    refundItemSafely(player, itemToEquip, "Failed to register identity: " + ex.getMessage());
                    return null;
                });
            } else {
                processEquip(player, identityUuid, itemToEquip, base64);
            }
        }).exceptionally(ex -> {
            refundItemSafely(player, itemToEquip, "Failed to fetch identity data: " + ex.getMessage());
            return null;
        });
    }

    private void refundItemSafely(Player player, ItemStack itemToEquip, String message) {
        player.getScheduler().run(plugin, task -> {
            if (!plugin.isEnabled() || !player.isOnline()) return;
            player.sendMessage(Component.text(message, NamedTextColor.RED));
            player.getInventory().addItem(itemToEquip).values()
                    .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }, null);
    }

    private void processEquip(Player player, UUID identityUuid, ItemStack itemToEquip, String base64) {
        player.getScheduler().run(plugin, task -> {
            if (!plugin.isEnabled() || !player.isOnline()) return;
            
            IdentityEquipEvent equipEvent = new IdentityEquipEvent(player, identityUuid, itemToEquip);
            Bukkit.getPluginManager().callEvent(equipEvent);
            
            if (equipEvent.isCancelled()) {
                refundItemSafely(player, itemToEquip, "Equipping was cancelled.");
                return;
            }

            MCIdentityProvider.instance.setActiveIdentity(player.getUniqueId(), identityUuid, base64).thenAccept(v -> {
                player.getScheduler().run(plugin, innerTask -> {
                    if (!plugin.isEnabled() || !player.isOnline()) return;
                    player.sendMessage(Component.text("Identity equipped!", NamedTextColor.GREEN));
                    MCIdentityProvider.instance.logAction(player.getUniqueId(), identityUuid, ActionType.EQUIP);
                }, null);
            }).exceptionally(ex -> {
                refundItemSafely(player, itemToEquip, "Failed to equip identity: " + ex.getMessage());
                return null;
            });
        }, null);
    }

    private java.util.concurrent.CompletableFuture<Void> createIdentityInDB(UUID identityUuid, String professionCode, String headTexture) {
        IdentityKey key = new IdentityKey() {
            @Override public UUID getUniqueId() { return identityUuid; }
            @Override public String getProfessionCode() { return professionCode; }
        };

        IdentityProperties props = new IdentityProperties() {
            @Override public int getLevel() { return 1; }
            @Override public int getExp() { return 0; }
            @Override public int getStatPoints() { return 0; }
            @Override public String getHeadText() { return headTexture; }
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

                ItemStack itemToEquip = cursorItem.clone();
                itemToEquip.setAmount(1);
                
                String uuidStr = itemToEquip.getItemMeta().getPersistentDataContainer().get(identityKey, PersistentDataType.STRING);
                UUID identityUuid;
                try {
                    identityUuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException e) {
                    player.sendMessage(Component.text("This identity item is corrupted!", NamedTextColor.RED));
                    return;
                }

                IdentityEquipEvent equipEvent = new IdentityEquipEvent(player, identityUuid, itemToEquip);
                Bukkit.getPluginManager().callEvent(equipEvent);

                if (equipEvent.isCancelled()) {
                    return;
                }

                if (cursorItem.getAmount() > 1) {
                    cursorItem.setAmount(cursorItem.getAmount() - 1);
                } else {
                    event.getWhoClicked().setItemOnCursor(null);
                }

                String base64 = ItemStackSerializer.serialize(itemToEquip);
                
                MCIdentityProvider.instance.setActiveIdentity(player.getUniqueId(), identityUuid, base64).thenAccept(v -> {
                    // Fix: Fire Folia Scheduler before sending messages & opening inventory 
                    player.getScheduler().run(plugin, task -> {
                        if (!plugin.isEnabled() || !player.isOnline()) return;
                        player.sendMessage(Component.text("Identity equipped via GUI!", NamedTextColor.GREEN));
                        MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
                        if (cmd != null) cmd.openIdentityGui(player);
                    }, null);
                    // Safe to call async
                    MCIdentityProvider.instance.logAction(player.getUniqueId(), identityUuid, ActionType.EQUIP);
                });
            } else if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                player.sendMessage(Component.text("Only identity keys can be placed here!", NamedTextColor.RED));
            }
        } else if (isTakeAction(action)) {
            if (isIdentityKey(currentItem)) {
                ItemStack itemToTake = currentItem.clone();
                
                String uuidStr = itemToTake.getItemMeta().getPersistentDataContainer().get(identityKey, PersistentDataType.STRING);
                UUID identityUuid = UUID.fromString(uuidStr);

                IdentityUnequipEvent unequipEvent = new IdentityUnequipEvent(player, identityUuid);
                Bukkit.getPluginManager().callEvent(unequipEvent);

                if (unequipEvent.isCancelled()) {
                    return;
                }
                
                java.util.Map<Integer, ItemStack> leftOver = player.getInventory().addItem(itemToTake);
                leftOver.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                
                MCIdentityProvider.instance.setActiveIdentity(player.getUniqueId(), null, null).thenAccept(v -> {
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
        return item.getItemMeta().getPersistentDataContainer().has(identityKey, PersistentDataType.STRING);
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

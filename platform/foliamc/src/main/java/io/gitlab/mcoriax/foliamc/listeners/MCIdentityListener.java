package io.gitlab.mcoriax.foliamc.listeners;

import io.gitlab.mcoriax.api.model.ActionType;
import io.gitlab.mcoriax.api.model.IdentityKey;
import io.gitlab.mcoriax.api.model.IdentityProperties;
import io.gitlab.mcoriax.api.model.IdentityStats;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import io.gitlab.mcoriax.foliamc.commands.MCIdentityCommand;
import io.gitlab.mcoriax.foliamc.util.ItemStackSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class MCIdentityListener implements Listener {

    private final MCIdentityPlugin plugin;
    private final NamespacedKey identityKey;
    private final NamespacedKey sigilKey;
    private final NamespacedKey professionKey;

    public MCIdentityListener(MCIdentityPlugin plugin) {
        this.plugin = plugin;
        this.identityKey = new NamespacedKey(plugin, "identity_uuid");
        this.sigilKey = new NamespacedKey(plugin, "sigil_code");
        this.professionKey = new NamespacedKey(plugin, "profession_code");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        MCIdentityProvider.getSession(player.getUniqueId()).thenAccept(optSession -> {
            optSession.ifPresent(session -> {
                MCIdentityProvider.loadSessionToRam(player.getUniqueId(), session.getIdentityUniqueId()).thenRun(() -> {
                    player.sendMessage(Component.text("You are currently wearing an identity."));
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

        UUID identityUuid = UUID.fromString(uuidStr);
        String sigilCode = meta.getPersistentDataContainer().get(sigilKey, PersistentDataType.STRING);
        String professionCode = meta.getPersistentDataContainer().get(professionKey, PersistentDataType.STRING);

        if (MCIdentityManager.getSession(player.getUniqueId()).isPresent()) {
            player.sendMessage(Component.text("You are already wearing an identity!", NamedTextColor.RED));
            return;
        }

        // Clone item for serialization but DO NOT remove yet to prevent item loss if disconnect
        ItemStack itemToEquip = item.clone();
        itemToEquip.setAmount(1);

        String base64 = ItemStackSerializer.serialize(itemToEquip);

        MCIdentityProvider.getIdentityKey(identityUuid).thenAccept(optKey -> {
            if (optKey.isEmpty()) {
                if (sigilCode == null || professionCode == null) {
                    player.getScheduler().run(plugin, task -> {
                        if (player.isOnline()) {
                            player.sendMessage(Component.text("This identity is missing metadata and cannot be registered!", NamedTextColor.RED));
                        }
                    }, null);
                    return;
                }

                // Create record in DB if not exist
                createIdentityInDB(identityUuid, sigilCode, professionCode).thenAccept(v -> {
                    equipIdentity(player, identityUuid, itemToEquip, base64);
                }).exceptionally(ex -> {
                    player.getScheduler().run(plugin, task -> {
                        if (player.isOnline()) {
                            player.sendMessage(Component.text("Failed to register identity: " + ex.getMessage(), NamedTextColor.RED));
                        }
                    }, null);
                    return null;
                });
            } else {
                equipIdentity(player, identityUuid, itemToEquip, base64);
            }
        }).exceptionally(ex -> {
            player.getScheduler().run(plugin, task -> {
                if (player.isOnline()) {
                    player.sendMessage(Component.text("Failed to fetch identity data: " + ex.getMessage(), NamedTextColor.RED));
                }
            }, null);
            return null;
        });
    }

    private void equipIdentity(Player player, UUID identityUuid, ItemStack itemToEquip, String base64) {
        MCIdentityProvider.setActiveIdentity(player.getUniqueId(), identityUuid, base64).thenAccept(v -> {
            player.getScheduler().run(plugin, task -> {
                if (player.isOnline()) {
                    // Step 2: Fix Item Loss Risk - ONLY remove item after success
                    ItemStack held = player.getInventory().getItemInMainHand();
                    if (held != null && held.getType() != Material.AIR && held.isSimilar(itemToEquip)) {
                        held.setAmount(held.getAmount() - 1);
                        player.sendMessage(Component.text("Identity equipped!", NamedTextColor.GREEN));
                        MCIdentityProvider.logAction(player.getUniqueId(), identityUuid, ActionType.EQUIP);
                    } else {
                        // If item is no longer in hand (moved during async), find and remove 1
                        if (player.getInventory().removeItem(itemToEquip).isEmpty()) {
                             player.sendMessage(Component.text("Identity equipped!", NamedTextColor.GREEN));
                             MCIdentityProvider.logAction(player.getUniqueId(), identityUuid, ActionType.EQUIP);
                        } else {
                             // This should technically not happen if player didn't drop it
                             player.sendMessage(Component.text("Failed to find the identity item in your inventory, but it has been activated.", NamedTextColor.YELLOW));
                        }
                    }
                }
            }, null);
        }).exceptionally(ex -> {
            player.getScheduler().run(plugin, task -> {
                if (player.isOnline()) {
                    player.sendMessage(Component.text("Failed to equip identity: " + ex.getMessage(), NamedTextColor.RED));
                }
            }, null);
            return null;
        });
    }

    private java.util.concurrent.CompletableFuture<Void> createIdentityInDB(UUID identityUuid, String sigilCode, String professionCode) {
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

        return MCIdentityProvider.createIdentity(key, props, stats);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Component title = event.getView().title();
        if (title.equals(Component.translatable("mcidentity.gui.title", "Identity GUI"))) {
            handleIdentityGuiClick(event);
        } else if (title.equals(Component.translatable("mcidentity.gui.rune.title", "Identity Rune GUI"))) {
            handleRuneGuiClick(event);
        } else if (title.equals(Component.translatable("mcidentity.gui.ability.title", "Identity Ability GUI"))) {
            handleAbilityGuiClick(event);
        }
    }

    private void handleAbilityGuiClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == 45) {
            // Back button
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
            // Back button
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

        // 1. Block all clicks in top inventory except slot 49
        if (slot >= 0 && slot < event.getView().getTopInventory().getSize()) {
            if (slot == 42) {
                event.setCancelled(true);
                // Open Rune GUI
                MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
                if (cmd != null) {
                    cmd.openIdentityRuneGui(player);
                }
                return;
            }

            if (slot == 41) {
                event.setCancelled(true);
                // Open Ability GUI
                MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
                if (cmd != null) {
                    cmd.openIdentityAbilityGui(player);
                }
                return;
            }

            if (slot != 49) {
                event.setCancelled(true);
                return;
            }
            
            // Interaction with slot 49
            handleSlot49Click(event, player);
        } else {
            // Clicks in player inventory (bottom inventory)
            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                // Prevent shift-clicking items into the GUI easily or incorrectly
                event.setCancelled(true);
            }
        }
    }

    private void handleSlot49Click(InventoryClickEvent event, Player player) {
        ItemStack cursorItem = event.getCursor();
        ItemStack currentItem = event.getCurrentItem();
        InventoryAction action = event.getAction();

        // Logic:
        // If placing (cursor has item):
        //   Check if cursor is identity key
        //   If yes, setActiveIdentity(cursor)
        //   If no, cancel
        // If taking (current has item):
        //   setActiveIdentity(null)

        if (isPlaceAction(action)) {
            if (isIdentityKey(cursorItem)) {
                String uuidStr = cursorItem.getItemMeta().getPersistentDataContainer().get(identityKey, PersistentDataType.STRING);
                UUID identityUuid = UUID.fromString(uuidStr);
                String base64 = ItemStackSerializer.serialize(cursorItem);
                
                MCIdentityProvider.setActiveIdentity(player.getUniqueId(), identityUuid, base64).thenAccept(v -> {
                    player.sendMessage(Component.text("Identity equipped via GUI!", NamedTextColor.GREEN));
                    MCIdentityProvider.logAction(player.getUniqueId(), identityUuid, ActionType.EQUIP);
                });
            } else {
                event.setCancelled(true);
                player.sendMessage(Component.text("Only identity keys can be placed here!", NamedTextColor.RED));
            }
        } else if (isTakeAction(action)) {
            if (isIdentityKey(currentItem)) {
                String uuidStr = currentItem.getItemMeta().getPersistentDataContainer().get(identityKey, PersistentDataType.STRING);
                UUID identityUuid = UUID.fromString(uuidStr);
                
                MCIdentityProvider.setActiveIdentity(player.getUniqueId(), null, null).thenAccept(v -> {
                    player.sendMessage(Component.text("Identity unequipped via GUI.", NamedTextColor.YELLOW));
                    MCIdentityProvider.logAction(player.getUniqueId(), identityUuid, ActionType.UNEQUIP);
                });
            }
        } else if (action == InventoryAction.NOTHING || action == InventoryAction.DROP_ALL_SLOT || action == InventoryAction.DROP_ONE_SLOT) {
             // Let drop actions handle themselves or handle specifically
        } else {
            // Catch-all for other complex actions (swapping, etc)
            event.setCancelled(true);
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
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLocation = player.getLocation().clone();

        MCIdentityProvider.getSession(player.getUniqueId()).thenAccept(optSession -> {
            optSession.ifPresent(session -> {
                UUID identityUuid = session.getIdentityUniqueId();
                String base64 = session.getContentKey();

                // Clear content_key and active identity
                MCIdentityProvider.setActiveIdentity(player.getUniqueId(), null, null).thenAccept(v -> {
                    if (base64 != null) {
                        try {
                            ItemStack item = ItemStackSerializer.deserialize(base64);
                            // Step 4: Fix Folia Death Drop Desync - ensure region is used correctly with chunk loading
                            Bukkit.getRegionScheduler().run(plugin, deathLocation, task -> {
                                if (deathLocation.getWorld() != null) {
                                    // Ensure chunk is loaded for dropping item
                                    deathLocation.getChunk().addPluginChunkTicket(plugin);
                                    try {
                                        deathLocation.getWorld().dropItemNaturally(deathLocation, item);
                                        if (player.isOnline()) {
                                            player.sendMessage(Component.text("Your identity has been dropped due to death!", NamedTextColor.RED));
                                        }
                                    } finally {
                                        deathLocation.getChunk().removePluginChunkTicket(plugin);
                                    }
                                }
                            });
                            MCIdentityProvider.logAction(player.getUniqueId(), identityUuid, ActionType.DEATH_DROP);
                        } catch (Exception e) {
                            plugin.getLogger().severe("Failed to deserialize identity item for " + player.getName());
                            e.printStackTrace();
                        }
                    }
                });
            });
        });
    }
}

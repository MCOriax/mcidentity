package io.gitlab.mcoriax.foliamc.listeners;

import io.gitlab.mcoriax.api.model.ActionType;
import io.gitlab.mcoriax.api.model.IdentityKey;
import io.gitlab.mcoriax.api.model.IdentityProperties;
import io.gitlab.mcoriax.api.model.IdentityStats;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import io.gitlab.mcoriax.foliamc.util.ItemStackSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
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

        MCIdentityProvider.getIdentityKey(identityUuid).thenAccept(optKey -> {
            if (optKey.isEmpty()) {
                if (sigilCode == null || professionCode == null) {
                    player.sendMessage(Component.text("This identity is missing metadata and cannot be registered!"));
                    return;
                }
                
                // Create record in DB if not exist
                createIdentityInDB(identityUuid, sigilCode, professionCode).thenRun(() -> {
                    equipIdentity(player, identityUuid, item);
                });
            } else {
                equipIdentity(player, identityUuid, item);
            }
        });
    }

    private void equipIdentity(Player player, UUID identityUuid, ItemStack item) {
        String base64 = ItemStackSerializer.serialize(item);
        MCIdentityProvider.setActiveIdentity(player.getUniqueId(), identityUuid, base64).thenAccept(v -> {
            player.getScheduler().run(plugin, task -> {
                player.getInventory().remove(item);
            }, null);
            player.sendMessage(Component.text("Identity equipped!"));
            MCIdentityProvider.logAction(player.getUniqueId(), identityUuid, ActionType.EQUIP);
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
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        MCIdentityProvider.getSession(player.getUniqueId()).thenAccept(optSession -> {
            optSession.ifPresent(session -> {
                UUID identityUuid = session.getIdentityUniqueId();
                String base64 = session.getContentKey();

                // Clear content_key and active identity
                MCIdentityProvider.setActiveIdentity(player.getUniqueId(), null, null).thenAccept(v -> {
                    if (base64 != null) {
                        try {
                            ItemStack item = ItemStackSerializer.deserialize(base64);
                            player.getScheduler().run(plugin, task -> {
                                player.getWorld().dropItemNaturally(player.getLocation(), item);
                                player.sendMessage(Component.text("Your identity has been dropped due to death!", NamedTextColor.RED));
                            }, null);
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

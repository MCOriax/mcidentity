package io.github.mcoriax.mcidentity.bukkit.listeners;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;

import io.github.mcoriax.mcidentity.bukkit.event.MCIdentityUnequipEvent;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.mcidentity.bukkit.listeners.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MCIdentityListener implements Listener {

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;
    /**
     * The NamespacedKey for identity UUID.
     */
    private final NamespacedKey identityKey;
    /**
     * The NamespacedKey for profession code.
     */
    private final NamespacedKey professionKey;
    /**
     * The NamespacedKey for identity texture.
     */
    private final NamespacedKey textureKey;
    /**
     * The NamespacedKey for identification status.
     */
    private final NamespacedKey unidentifiedKey;

    /**
     * Creates a new MCIdentityListener.
     * @param plugin The plugin instance.
     */
    public MCIdentityListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.identityKey = new NamespacedKey(plugin, "identity_uuid");
        this.professionKey = new NamespacedKey(plugin, "profession_code");
        this.textureKey = new NamespacedKey(plugin, "identity_texture");
        this.unidentifiedKey = new NamespacedKey(plugin, "is_unidentified");
    }

    /**
     * Restores the joining player's identity session and gameplay state.
     * @param event The join event being handled.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        OnPlayerJoin.execute(plugin, event);
    }

    /**
     * Flushes the leaving player's identity state and drops their cached session.
     * @param event The quit event being handled.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        OnPlayerQuit.execute(plugin, event);
    }

    /**
     * Handles equipping an identity key held in hand.
     * @param event The interact event being handled.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        OnPlayerInteract.execute(plugin, event, identityKey, professionKey, textureKey, unidentifiedKey);
    }

    /**
     * Handles identity key interactions inside an inventory, including the plugin's own menus.
     * @param event The inventory click event being handled.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        OnInventoryClick.execute(plugin, event, identityKey, professionKey, unidentifiedKey, textureKey);
    }

    /**
     * Stops an identity key from being placed as a block.
     * @param event The block place event being handled.
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        OnBlockPlace.execute(plugin, event, identityKey, professionKey, unidentifiedKey);
    }

    /**
     * Awards combat EXP once the damage has been applied and not cancelled.
     * @param event The damage event being handled.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        OnEntityDamageByEntity.execute(plugin, event);
    }

    /**
     * Awards kill EXP once the death has been confirmed.
     * @param event The entity death event being handled.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        OnEntityDeath.execute(plugin, event);
    }

    /**
     * Applies the configured death behaviour to the player's active identity.
     * @param event The player death event being handled.
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        OnPlayerDeath.execute(plugin, event);
    }

    /**
     * Resets the player's vanilla attributes once an identity has been unequipped.
     * @param event The unequip event being handled.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIdentityUnequip(MCIdentityUnequipEvent event) {
        Player player = event.getPlayer();
        Runnable task = () -> {
            if (!player.isOnline()) return;
            
            // Reset food level to maximum
            player.setFoodLevel(20);
            
            // Reset health to maximum health attribute
            double maxHealth = 20.0;
            if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
                maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            }
            player.setHealth(maxHealth);
        };

        if (plugin.isEnabled()) {
            Schedulers.forEntity(player).run(plugin, scheduledTask -> task.run(), null);
        } else if (Bukkit.isPrimaryThread()) {
            task.run();
        }
    }
}

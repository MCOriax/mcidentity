package io.gitlab.mcoriax.bukkit.listeners;

import io.gitlab.mcoriax.bukkit.event.MCIdentityUnequipEvent;
import org.bukkit.plugin.java.JavaPlugin;
import io.gitlab.mcoriax.bukkit.listeners.utils.*;
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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        OnPlayerJoin.execute(plugin, event);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        OnPlayerQuit.execute(plugin, event);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        OnPlayerInteract.execute(plugin, event, identityKey, professionKey, textureKey, unidentifiedKey);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        OnInventoryClick.execute(plugin, event, identityKey, professionKey, unidentifiedKey, textureKey);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        OnBlockPlace.execute(plugin, event, identityKey, professionKey, unidentifiedKey);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        OnEntityDamageByEntity.execute(plugin, event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        OnEntityDeath.execute(plugin, event);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        OnPlayerDeath.execute(plugin, event);
    }

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
            player.getScheduler().run(plugin, scheduledTask -> task.run(), null);
        } else if (Bukkit.isPrimaryThread()) {
            task.run();
        }
    }
}

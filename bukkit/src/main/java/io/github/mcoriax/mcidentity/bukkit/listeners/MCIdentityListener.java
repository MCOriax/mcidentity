package io.github.mcoriax.mcidentity.bukkit.listeners;

import io.github.mcoriax.mcidentity.bukkit.listeners.utils.OnEntityDamageByEntity;
import io.github.mcoriax.mcidentity.bukkit.listeners.utils.OnEntityDeath;
import io.github.mcoriax.mcidentity.bukkit.listeners.utils.OnInventoryClick;
import io.github.mcoriax.mcidentity.bukkit.listeners.utils.OnPlayerDeath;
import io.github.mcoriax.mcidentity.bukkit.listeners.utils.OnPlayerJoin;
import io.github.mcoriax.mcidentity.bukkit.listeners.utils.OnPlayerQuit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Routes the player lifecycle, GUI and combat events to their handlers.
 */
public class MCIdentityListener implements Listener {

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;

    /**
     * Creates a new MCIdentityListener.
     * @param plugin The plugin instance.
     */
    public MCIdentityListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads the joining player's character and worn profession.
     * @param event The join event being handled.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        OnPlayerJoin.execute(plugin, event);
    }

    /**
     * Flushes the leaving player's state and drops their cached session.
     * @param event The quit event being handled.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        OnPlayerQuit.execute(plugin, event);
    }

    /**
     * Handles clicks inside the plugin's own GUIs.
     * @param event The inventory click event being handled.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        OnInventoryClick.execute(plugin, event);
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
     * Restores the dying player's current stats for the respawn.
     * @param event The player death event being handled.
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        OnPlayerDeath.execute(plugin, event);
    }
}

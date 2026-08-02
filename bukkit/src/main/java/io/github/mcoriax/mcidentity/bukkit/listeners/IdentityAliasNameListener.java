package io.github.mcoriax.mcidentity.bukkit.listeners;

import io.github.mcoriax.mcidentity.bukkit.event.MCIdentityEquipEvent;
import io.github.mcoriax.mcidentity.bukkit.event.MCIdentityUnequipEvent;
import io.github.mcoriax.mcidentity.bukkit.listeners.utils.ApplyIdentityListName;
import io.github.mcoriax.mcidentity.bukkit.listeners.utils.ResolveIdentityAlias;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Presents players under their active identity's alias rather than their
 * Minecraft username, in chat and in the tab list.
 *
 * <p>Both paths resolve the name from the RAM session cache through
 * {@link ResolveIdentityAlias}, never from the database, so neither the game
 * thread nor the asynchronous chat thread ever waits on storage. A player with
 * no cached session, or an identity with no claimed alias, keeps their
 * username.</p>
 *
 * <p>The tab list is refreshed when a player joins and again whenever an
 * identity is equipped or unequipped, so a swap is reflected immediately
 * instead of waiting for the periodic visual refresh in
 * {@link MCIdentityStatsListener}.</p>
 */
public class IdentityAliasNameListener implements Listener {

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;

    /**
     * Creates a new IdentityAliasNameListener.
     * @param plugin The plugin instance.
     */
    public IdentityAliasNameListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Names the speaker by their identity alias in the outgoing chat format.
     * @param event The chat event being handled.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        String alias = ResolveIdentityAlias.execute(event.getPlayer());
        if (alias.equals(event.getPlayer().getName())) {
            return;
        }
        // Only the name placeholder is substituted, so any prefix another
        // plugin added to the format survives. An alias is alphanumeric, so the
        // replacement can never introduce a format specifier of its own.
        event.setFormat(event.getFormat().replace("%1$s", alias));
    }

    /**
     * Shows the joining player's alias in the tab list.
     * @param event The join event being handled.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        ApplyIdentityListName.execute(plugin, event.getPlayer());
    }

    /**
     * Refreshes the tab list once an identity has been equipped.
     * @param event The equip event being handled.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIdentityEquip(MCIdentityEquipEvent event) {
        ApplyIdentityListName.execute(plugin, event.getPlayer());
    }

    /**
     * Restores the tab list name once an identity has been unequipped.
     * @param event The unequip event being handled.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIdentityUnequip(MCIdentityUnequipEvent event) {
        ApplyIdentityListName.execute(plugin, event.getPlayer());
    }
}

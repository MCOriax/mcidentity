package io.github.mcoriax.mcidentity.bukkit.listeners;

import io.github.mcoriax.mcidentity.bukkit.listeners.utils.ApplyProfessionListName;
import io.github.mcoriax.mcidentity.bukkit.listeners.utils.BuildProfessionNameTag;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Tags players with their worn profession in chat and in the tab list.
 *
 * <p>Both paths use the tag from {@link BuildProfessionNameTag}, read from the
 * RAM session cache and never from the database, so neither the game thread nor
 * the asynchronous chat thread ever waits on storage. A player with no cached
 * session keeps their plain username.</p>
 *
 * <p>The tab list is refreshed on join; a wear or unwear refreshes it from the
 * service that changed the profession, once the change has completed.</p>
 */
public class ProfessionNameTagListener implements Listener {

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;

    /**
     * Creates a new ProfessionNameTagListener.
     * @param plugin The plugin instance.
     */
    public ProfessionNameTagListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Names the speaker by their profession name tag in the outgoing chat format.
     * @param event The chat event being handled.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (MCIdentityManager.getSession(player.getUniqueId()).isEmpty()) {
            return;
        }
        // Only the %1$s placeholder is touched, so a prefix another plugin put
        // in the format survives. Percent signs are doubled first: a profession
        // display name comes from a server's own YAML and may hold one.
        String nameTag = BuildProfessionNameTag.execute(player).replace("%", "%%");
        event.setFormat(event.getFormat().replace("%1$s", nameTag));
    }

    /**
     * Shows the joining player's profession in the tab list.
     * @param event The join event being handled.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        ApplyProfessionListName.execute(plugin, event.getPlayer());
    }
}

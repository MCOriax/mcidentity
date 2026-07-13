package io.github.mcoriax.bukkit.listeners;

import io.github.mcoriax.common.db.central.MCIdentityCentralServer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Cold-boot login protection for the central server backend.
 *
 * <p>Registered only when {@code database.type} is {@code central}. The
 * central server may be hosted on infrastructure that suspends idle instances
 * (a "cold" server), so before a player is allowed in, the plugin pings the
 * server's {@code /health} endpoint. The ping doubles as the wake-up signal:
 * even when it fails because the instance is still booting, the request has
 * already triggered the cold start, so the server is typically ready by the
 * time the player retries.</p>
 *
 * <p>The check runs inside {@link AsyncPlayerPreLoginEvent}, which Bukkit
 * fires on an async login thread — waiting here delays only the joining
 * player's login handshake and never blocks the main game thread. When the
 * central server is unreachable or the request times out, the player is
 * refused with a message asking them to retry shortly.</p>
 */
public class PlayerLoginListener implements Listener {

    /**
     * The exact kick message shown while the central server is waking up.
     */
    static final String DATABASE_STARTING_MESSAGE = "The database is starting up. Please come back in 1 minute.";

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;

    /**
     * The central server client whose health endpoint is pinged.
     */
    private final MCIdentityCentralServer centralServer;

    /**
     * Creates a new PlayerLoginListener.
     * @param plugin The plugin instance.
     * @param centralServer The central server client backing the plugin.
     */
    public PlayerLoginListener(JavaPlugin plugin, MCIdentityCentralServer centralServer) {
        this.plugin = plugin;
        this.centralServer = centralServer;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        // join() is safe here: the event runs on an async login thread, and
        // pingHealth() always completes within the client's HTTP timeout.
        boolean online = centralServer.pingHealth().join();
        if (!online) {
            plugin.getLogger().warning("Refused login for " + event.getName()
                + ": the central server is offline or still starting up");
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, DATABASE_STARTING_MESSAGE);
        }
    }
}

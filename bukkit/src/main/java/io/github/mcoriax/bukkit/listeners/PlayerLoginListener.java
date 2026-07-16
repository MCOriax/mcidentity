package io.github.mcoriax.bukkit.listeners;

import io.github.mcoriax.common.central.MCIdentityCentralServer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.BooleanSupplier;

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
 * player's login handshake and never blocks the main game thread. Players are
 * refused while the plugin's storage initialization (schema check plus
 * profession registration) has not completed yet — a failed initialization's
 * own health request may already have woken the cold server, so a passing
 * ping alone does not prove the plugin is ready — and whenever the central
 * server is unreachable or the request times out.</p>
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
     * Reports whether the plugin's storage initialization has completed.
     */
    private final BooleanSupplier storageReady;

    /**
     * Creates a new PlayerLoginListener.
     * @param plugin The plugin instance.
     * @param centralServer The central server client backing the plugin.
     * @param storageReady Supplies whether storage initialization has
     *                     completed; logins are refused while it is false.
     */
    public PlayerLoginListener(JavaPlugin plugin, MCIdentityCentralServer centralServer, BooleanSupplier storageReady) {
        this.plugin = plugin;
        this.centralServer = centralServer;
        this.storageReady = storageReady;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!storageReady.getAsBoolean()) {
            plugin.getLogger().warning("Refused login for " + event.getName()
                + ": central storage initialization has not completed yet");
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, DATABASE_STARTING_MESSAGE);
            return;
        }

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

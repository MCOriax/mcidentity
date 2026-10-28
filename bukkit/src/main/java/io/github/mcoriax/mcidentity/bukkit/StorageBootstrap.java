package io.github.mcoriax.mcidentity.bukkit;

import io.github.mcoriax.mcidentity.bukkit.listeners.PlayerLoginListener;
import io.github.mcoriax.mcidentity.bukkit.scheduler.PlatformTask;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Brings the storage backend selected by {@code database.type} up through
 * {@link MCIdentityProvider}, retrying until it answers.
 *
 * <p><strong>Internal.</strong> Package-private, owned by
 * {@link AbstractMCIdentityPlugin}, which answers
 * {@link AbstractMCIdentityPlugin#isStorageInitialized()} from it.</p>
 */
final class StorageBootstrap {

    /**
     * Milliseconds between storage initialization retries.
     */
    private static final long STORAGE_RETRY_PERIOD_MILLIS = 10_000L;

    /**
     * The plugin whose configuration selects the backend.
     */
    private final JavaPlugin plugin;

    /**
     * Whether the schema initialization and profession registration chain has
     * completed successfully since enable.
     */
    private volatile boolean storageInitialized;

    /**
     * Guards against overlapping initialization attempts when one attempt is
     * still waiting on the database while the retry ticker fires again.
     */
    private final AtomicBoolean storageInitAttemptInFlight = new AtomicBoolean();

    /**
     * The 10-second retry ticker driving initialization attempts until one
     * succeeds; null once cancelled.
     */
    private volatile PlatformTask storageRetryTask;

    /**
     * Creates the bootstrap.
     * @param plugin The plugin whose configuration selects the backend.
     */
    StorageBootstrap(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Whether the schema exists and the loaded professions are registered.
     * @return True once storage initialization has completed successfully.
     */
    boolean isInitialized() {
        return storageInitialized;
    }

    /**
     * Initializes the storage backend selected by {@code database.type} in
     * config.yml — SQLite (the default), MySQL, PostgreSQL, MongoDB, or the
     * MCIdentity central server — and registers the loaded professions there.
     */
    void start() {
        String type = plugin.getConfig().getString("database.type", "sqlite");

        MCIdentityProvider provider;
        try {
            provider = MCIdentityProvider.create(type, databaseOptions(type));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().severe(e.getMessage() + " (supported: sqlite, mysql, postgresql, mongodb, central). Disabling...");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        // Cold-boot login protection: with remote storage, hold logins until
        // initialization has completed and the (possibly cold-started) server
        // answers its health probe, so players never join while their data is
        // unreachable. Which backend that is stays behind the provider.
        if (provider.isRemoteStorage()) {
            plugin.getServer().getPluginManager().registerEvents(
                new PlayerLoginListener(plugin, this::isInitialized), plugin);
        }

        // The ticker keeps retrying the initialization until one attempt
        // succeeds, so a cold-started central server (or a briefly unreachable
        // database) only delays startup instead of leaving the plugin
        // half-initialized. The first attempt runs immediately.
        storageRetryTask = Schedulers.runAsyncAtFixedRate(
            () -> attemptStorageInitialization(provider, type),
            STORAGE_RETRY_PERIOD_MILLIS, STORAGE_RETRY_PERIOD_MILLIS);
        attemptStorageInitialization(provider, type);
    }

    /**
     * Runs one attempt of the schema initialization and profession
     * registration chain. Attempts are skipped while storage is already
     * initialized or a previous attempt is still in flight; a failed attempt
     * is logged and retried by the ticker every ten seconds.
     * @param provider The provider whose storage should be initialized.
     * @param type The configured database type, used in log messages.
     */
    private void attemptStorageInitialization(MCIdentityProvider provider, String type) {
        if (storageInitialized || !storageInitAttemptInFlight.compareAndSet(false, true)) {
            return;
        }
        provider.initializeStorage()
            .thenCompose(v -> registerProfessionsSequentially())
            .whenComplete((v, ex) -> {
                storageInitAttemptInFlight.set(false);
                if (ex == null) {
                    storageInitialized = true;
                    cancelRetries();
                    plugin.getLogger().info("Connected to " + type + " storage and registered professions");
                } else {
                    plugin.getLogger().warning("Failed to initialize the " + type + " storage: " + ex.getMessage()
                        + ". Retrying in 10 seconds...");
                }
            });
    }

    /**
     * Cancels the storage initialization retry ticker if it is still armed.
     */
    void cancelRetries() {
        PlatformTask task = storageRetryTask;
        if (task != null) {
            task.cancel();
            storageRetryTask = null;
        }
    }

    /**
     * Collects the connection settings of the selected database type from
     * config.yml.
     * @param type The configured database type.
     * @return The connection settings passed to the backend factory.
     */
    private Map<String, Object> databaseOptions(String type) {
        String normalized = type == null ? "" : type.trim().toLowerCase();
        String sectionPath = switch (normalized) {
            case "central", "central_server" -> "central_server";
            case "postgres" -> "database.postgresql";
            case "mongo" -> "database.mongodb";
            case "mariadb" -> "database.mysql";
            default -> "database." + normalized;
        };

        ConfigurationSection section = plugin.getConfig().getConfigurationSection(sectionPath);
        Map<String, Object> options = section != null ? new HashMap<>(section.getValues(false)) : new HashMap<>();

        if (normalized.equals("sqlite")) {
            // The configured file name is relative to the plugin data folder.
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            String fileName = options.getOrDefault("file", "mcidentity.db").toString();
            options.put("file", new File(dataFolder, fileName).getAbsolutePath());
        }
        return options;
    }

    /**
     * Registers all loaded professions in the database sequentially.
     * @return A CompletableFuture representing the registration process.
     */
    private static CompletableFuture<Void> registerProfessionsSequentially() {
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        for (Map.Entry<String, String> entry : MCIdentityManager.getRegisteredProfessions().entrySet()) {
            future = future.thenCompose(v -> MCIdentityProvider.instance.createProfession(entry.getKey(), entry.getValue()));
        }
        return future;
    }
}

package io.github.mcoriax.mcidentity.bukkit;

import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import io.github.mcoriax.mcidentity.bukkit.bridge.mcagents.MCAgentsToolRegistrar;
import io.github.mcoriax.mcidentity.bukkit.config.AgentsSettings;
import io.github.mcoriax.mcidentity.bukkit.config.MCAgentsGlobal;
import io.github.mcoriax.mcidentity.bukkit.listeners.MCIdentityStatsListener;
import io.github.mcoriax.mcidentity.bukkit.scheduler.PlatformScheduler;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;
import io.github.mcoriax.mcidentity.bukkit.util.ExperienceManager;
import io.github.mcoriax.mcidentity.bukkit.util.PassiveExpService;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Shared bootstrap for every platform plugin entry point.
 * <p>
 * The enable/disable lifecycle, config loading, profession parsing, command and
 * listener registration, and database wiring are identical across SpigotMC,
 * PaperMC, and Folia. Each platform module supplies only its scheduler via
 * {@link #createScheduler()} and extends this class with a concrete entry point.
 * </p>
 * <p>
 * This class owns the lifecycle order; the work itself is delegated:
 * profession files to {@code ProfessionConfigLoader}, the storage backend and
 * its retries to {@code StorageBootstrap}, and the command and listeners to
 * {@code ListenerRegistrar}.
 * </p>
 */
public abstract class AbstractMCIdentityPlugin extends JavaPlugin {

    /**
     * Brings storage up and tracks whether it has finished.
     */
    private final StorageBootstrap storage = new StorageBootstrap(this);

    /**
     * The {@code agents} block of {@code config.yml}, read on enable; both
     * values {@value AgentsSettings#GLOBAL} until then.
     */
    private volatile AgentsSettings agentsSettings = new AgentsSettings(null, null);

    /**
     * Supplies the platform specific scheduler installed during {@link #onEnable()}.
     * @return the scheduler implementation for this platform.
     */
    protected abstract PlatformScheduler createScheduler();

    /**
     * Whether the storage backend is fully initialized: the schema exists and
     * the loaded professions are registered. The central-server login
     * protection refuses players until this is true.
     * @return True once storage initialization has completed successfully.
     */
    public boolean isStorageInitialized() {
        return storage.isInitialized();
    }

    /**
     * Gets the {@code agents} block as configured, with {@code global} values
     * left unresolved.
     * @return The agents settings read on enable.
     */
    public AgentsSettings getAgentsSettings() {
        return agentsSettings;
    }

    /**
     * Gets the AI model this plugin's agent work runs on: {@code agents.text},
     * or MCAgents' own when it is {@code global}.
     * @return The model id, blank when it is global and MCAgents has none.
     */
    public String getAgentsModel() {
        return agentsSettings.resolveModel(AgentsSettings.Modality.TEXT, new MCAgentsGlobal());
    }

    /**
     * Gets the OpenRouter API key this plugin's agent work runs on:
     * {@code agents.token}, or MCAgents' own when it is {@code global}.
     * @return The key, blank when it is global and MCAgents has none.
     */
    public String getAgentsToken() {
        return agentsSettings.resolveToken(new MCAgentsGlobal());
    }

    @Override
    /**
     * Boots the plugin: loads configuration and professions, brings storage up
     * through {@link MCIdentityProvider}, and registers commands and listeners.
     */
    public void onEnable() {
        Schedulers.set(createScheduler());

        // Only config.yml is written up front, because the enable flag lives in
        // it and the admin needs the file to toggle the plugin on. Everything
        // else (textures.yml, the professions folder, and the SQLite
        // mcidentity.db created later in setupDatabase) is generated only once
        // the plugin is enabled, so a disabled plugin never litters the data
        // folder.
        saveDefaultConfig();

        if (!getConfig().getBoolean("enable", false)) {
            getLogger().warning("Plugin is disabled in config.yml. No data files were generated. Disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig("textures", false);
        loadCustomConfigs();
        ProfessionConfigLoader.registerBaseProfession();

        checkLicense(getConfig().getString("license", "free"));

        agentsSettings = AgentsSettings.read(getConfig());
        getLogger().info("AI agent model: " + (agentsSettings.globalModel(AgentsSettings.Modality.TEXT)
            ? "global (MCAgents)" : agentsSettings.model(AgentsSettings.Modality.TEXT)));

        MCIdentityManager.setIdentitySwapCooldownMinutes(getConfig().getInt("identity_swap_cooldown", 0));
        MCIdentityManager.setExperienceManager(new ExperienceManager(this));

        storage.start();
        PassiveExpService.start(this);

        ListenerRegistrar.registerCommand(this);
        ListenerRegistrar.registerListeners(this);

        // Optional: expose the read-only identity tools to MCAgents when it is
        // installed. Reflective, so MCIdentity runs unchanged without it.
        if (isEnabled()) {
            MCAgentsToolRegistrar.registerAll(this);
        }

        getLogger().info("MCIdentityPlugin has been enabled!");
    }

    /**
     * Verifies the license type and logs information.
     * @param license The license key/type.
     */
    private void checkLicense(String license) {
        if (license.equalsIgnoreCase("free")) {
            getLogger().info("Running with free license. Some features may be limited.");
        } else if (license.equalsIgnoreCase("premium")) {
            getLogger().info("Running with premium license. All features enabled!");
        } else {
            getLogger().warning("Unknown license type in config.yml. Defaulting to free license.");
        }
    }

    /**
     * Loads custom configuration files.
     */
    private void loadCustomConfigs() {
        File texturesFile = new File(getDataFolder(), "textures.yml");
        MCIdentityManager.setTexturesConfig(YamlConfiguration.loadConfiguration(texturesFile));

        new ProfessionConfigLoader(this).loadProfessions();
    }

    /**
     * Saves a default configuration file if it doesn't exist.
     * @param name The name of the resource.
     * @param replace Whether to replace the existing file.
     */
    public void saveDefaultConfig(String name, boolean replace) {
        File file = new File(getDataFolder(), name + ".yml");
        if (!file.exists() || replace) {
            saveResource(name + ".yml", replace);
        }
    }

    /**
     * Gets the experience manager instance.
     * @return The experience manager.
     */
    public ExperienceManager getExperienceManager() {
        return (ExperienceManager) MCIdentityManager.getExperienceManager();
    }

    /**
     * Gets the identity stats listener instance.
     * @return The stats listener.
     */
    public MCIdentityStatsListener getStatsListener() {
        return (MCIdentityStatsListener) MCIdentityManager.getStatsListener();
    }

    /**
     * Gets the textures configuration instance.
     * @return The textures configuration.
     */
    public FileConfiguration getTexturesConfig() {
        return (FileConfiguration) MCIdentityManager.getTexturesConfig();
    }

    @Override
    /**
     * Shuts the plugin down: flushes buffered EXP, cancels the storage retry
     * ticker, and closes the storage backend through the provider.
     */
    public void onDisable() {
        storage.cancelRetries();

        // Flush buffered passive EXP before the database client goes away.
        PassiveExpService.stop();

        if (MCIdentityManager.getStatsListener() != null) {
            ((MCIdentityStatsListener) MCIdentityManager.getStatsListener()).cancelTasks();
        }

        // Gracefully close the connection pool
        if (MCIdentityProvider.hasDatabase()) {
            MCIdentityProvider.instance.shutdown();
        }

        getLogger().info("MCIdentityPlugin has been disabled!");
    }
}

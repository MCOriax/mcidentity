package io.github.mcoriax.bukkit;

import io.github.mcoriax.common.MCIdentityManager;
import io.github.mcoriax.common.MCIdentityProvider;
import io.github.mcoriax.common.db.postgresql.MCIdentityPostgreSQL;
import io.github.mcoriax.common.db.sqlite.MCIdentitySQLite;
import io.github.mcoriax.common.db.mongodb.MCIdentityMongoDB;
import io.github.mcoriax.bukkit.commands.MCIdentityCommand;
import io.github.mcoriax.bukkit.listeners.AdvancedExplorationExpListener;
import io.github.mcoriax.bukkit.listeners.BuildingAndSurvivalExpListener;
import io.github.mcoriax.bukkit.listeners.CombatExpListener;
import io.github.mcoriax.bukkit.listeners.CraftingExpListener;
import io.github.mcoriax.bukkit.listeners.DimensionsExpListener;
import io.github.mcoriax.bukkit.listeners.EngineeringExpListener;
import io.github.mcoriax.bukkit.listeners.ExplorationAndTravelExpListener;
import io.github.mcoriax.bukkit.listeners.FarmingAndRanchingExpListener;
import io.github.mcoriax.bukkit.listeners.GatheringExpListener;
import io.github.mcoriax.bukkit.listeners.InteractionAndArtExpListener;
import io.github.mcoriax.bukkit.listeners.InteractionExpListener;
import io.github.mcoriax.bukkit.listeners.MCIdentityInventoryListener;
import io.github.mcoriax.bukkit.listeners.MCIdentityListener;
import io.github.mcoriax.bukkit.listeners.MCIdentityStatsListener;
import io.github.mcoriax.bukkit.listeners.MagicAndEconomyExpListener;
import io.github.mcoriax.bukkit.listeners.SpecialEventsExpListener;
import io.github.mcoriax.bukkit.listeners.SpecialtyFarmingExpListener;
import io.github.mcoriax.bukkit.listeners.SupportExpListener;
import io.github.mcoriax.bukkit.listeners.SurvivalExpListener;
import io.github.mcoriax.bukkit.listeners.UtilityAndCombatExpListener;
import io.github.mcoriax.bukkit.listeners.RanchingAndWildlifeExpListener;
import io.github.mcoriax.bukkit.listeners.WorkstationExpListener;
import io.github.mcoriax.bukkit.listeners.WorldInteractionExpListener;
import io.github.mcoriax.bukkit.tabcompleter.MCIdentityTabCompleter;
import io.github.mcoriax.bukkit.scheduler.PlatformScheduler;
import io.github.mcoriax.bukkit.scheduler.Schedulers;
import io.github.mcoriax.bukkit.util.ExperienceManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Shared bootstrap for every platform plugin entry point.
 * <p>
 * The enable/disable lifecycle, config loading, profession parsing, command and
 * listener registration, and database wiring are identical across SpigotMC,
 * PaperMC, and Folia. Each platform module supplies only its scheduler via
 * {@link #createScheduler()} and extends this class with a concrete entry point.
 * </p>
 */
public abstract class AbstractMCIdentityPlugin extends JavaPlugin {

    /**
     * Supplies the platform specific scheduler installed during {@link #onEnable()}.
     * @return the scheduler implementation for this platform.
     */
    protected abstract PlatformScheduler createScheduler();

    @Override
    public void onEnable() {
        Schedulers.set(createScheduler());

        saveDefaultConfig();
        saveDefaultConfig("textures", false);

        loadCustomConfigs();
        if (!getConfig().getBoolean("enable", false)) {
            getLogger().warning("Plugin is disabled in config.yml. Disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        checkLicense(getConfig().getString("license", "free"));

        MCIdentityManager.setIdentitySwapCooldownMinutes(getConfig().getInt("identity_swap_cooldown", 0));
        MCIdentityManager.setExperienceManager(new ExperienceManager(this));

        setupDatabase();

        getCommand("identity").setExecutor(new MCIdentityCommand(this));
        getCommand("identity").setTabCompleter(new MCIdentityTabCompleter());

        getServer().getPluginManager().registerEvents(new MCIdentityListener(this), this);
        getServer().getPluginManager().registerEvents(new MCIdentityInventoryListener(this), this);
        MCIdentityManager.setStatsListener(new MCIdentityStatsListener(this));
        getServer().getPluginManager().registerEvents((MCIdentityStatsListener) MCIdentityManager.getStatsListener(), this);

        // Modular EXP gain listeners grouped by category.
        getServer().getPluginManager().registerEvents(new CombatExpListener(this), this);
        getServer().getPluginManager().registerEvents(new GatheringExpListener(this), this);
        getServer().getPluginManager().registerEvents(new CraftingExpListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractionExpListener(this), this);
        getServer().getPluginManager().registerEvents(new MagicAndEconomyExpListener(this), this);
        getServer().getPluginManager().registerEvents(new BuildingAndSurvivalExpListener(this), this);
        getServer().getPluginManager().registerEvents(new AdvancedExplorationExpListener(this), this);
        getServer().getPluginManager().registerEvents(new SupportExpListener(this), this);
        getServer().getPluginManager().registerEvents(new FarmingAndRanchingExpListener(this), this);
        getServer().getPluginManager().registerEvents(new ExplorationAndTravelExpListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractionAndArtExpListener(this), this);
        getServer().getPluginManager().registerEvents(new UtilityAndCombatExpListener(this), this);
        getServer().getPluginManager().registerEvents(new EngineeringExpListener(this), this);
        getServer().getPluginManager().registerEvents(new DimensionsExpListener(this), this);
        getServer().getPluginManager().registerEvents(new SurvivalExpListener(this), this);
        getServer().getPluginManager().registerEvents(new SpecialEventsExpListener(this), this);
        getServer().getPluginManager().registerEvents(new SpecialtyFarmingExpListener(this), this);
        getServer().getPluginManager().registerEvents(new RanchingAndWildlifeExpListener(this), this);
        getServer().getPluginManager().registerEvents(new WorkstationExpListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldInteractionExpListener(this), this);

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

        loadProfessions();
    }

    /**
     * Loads all profession configurations from the professions folder.
     */
    private void loadProfessions() {
        File professionsFolder = new File(getDataFolder(), "professions");
        if (!professionsFolder.exists()) {
            professionsFolder.mkdirs();
            // Save default professions if they don't exist
            saveResource("professions/archer.yml", false);
            saveResource("professions/builder.yml", false);
            saveResource("professions/crafter.yml", false);
            saveResource("professions/enchanter.yml", false);
            saveResource("professions/explorer.yml", false);
            saveResource("professions/farmer.yml", false);
            saveResource("professions/fighter.yml", false);
            saveResource("professions/fisher.yml", false);
            saveResource("professions/gatherer.yml", false);
            saveResource("professions/supporter.yml", false);
            saveResource("professions/tamer.yml", false);
            saveResource("professions/tanker.yml", false);
            saveResource("professions/acrobat.yml", false);
            saveResource("professions/archaeologist.yml", false);
            saveResource("professions/chef.yml", false);
            saveResource("professions/engineer.yml", false);
            saveResource("professions/guard.yml", false);
            saveResource("professions/lumberjack.yml", false);
            saveResource("professions/mariner.yml", false);
            saveResource("professions/merchant.yml", false);
            saveResource("professions/miner.yml", false);
        }

        File[] files = professionsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String code = config.getString("system_code");
            String displayName = config.getString("display_name");

            if (code == null) {
                code = file.getName().replace(".yml", "").toLowerCase();
            } else {
                code = code.toLowerCase();
            }

            if (displayName == null) {
                displayName = code.substring(0, 1).toUpperCase() + code.substring(1);
            }

            // Register in the common manager
            MCIdentityManager.setProfessionDisplayName(code, displayName);

            Map<String, Object> expConfig = new HashMap<>();
            Map<String, Integer> baseStats = new HashMap<>();

            int maxLevel = config.getInt("level.max", 100);
            int expUp = config.getInt("level.exp_up", 10000);
            MCIdentityManager.setProfessionLevelConfig(code, maxLevel, expUp);

            String statsRandom = config.getString("stats_random", "0");
            MCIdentityManager.setProfessionStatsRandom(code, statsRandom);

            if (config.isConfigurationSection("stats")) {
                ConfigurationSection statsSection = config.getConfigurationSection("stats");
                if (statsSection != null) {
                    for (String statKey : statsSection.getKeys(false)) {
                        baseStats.put(statKey, statsSection.getInt(statKey));
                    }
                }
            }
            MCIdentityManager.setProfessionBaseStats(code, baseStats);

            List<String> skinList = config.getStringList("skins");
            if (!skinList.isEmpty()) {
                Map<Integer, String> skins = new HashMap<>();
                for (int i = 0; i < skinList.size(); i++) {
                    // Index entries from 1 to preserve the previous level based keys
                    skins.put(i + 1, skinList.get(i));
                }
                MCIdentityManager.setProfessionSkins(code, skins);
            }

            for (String key : config.getKeys(false)) {
                if (key.equals("system_code") || key.equals("display_name") || key.equals("level") || key.equals("stats") || key.equals("stats_random") || key.equals("skins")) continue;
                if (config.isConfigurationSection(key)) {
                    expConfig.put(key, convertSectionToMap(config.getConfigurationSection(key)));
                } else {
                    expConfig.put(key, config.getString(key));
                }
            }

            MCIdentityManager.setProfessionExpConfig(code, expConfig);
            getLogger().info("Loaded profession config for: " + displayName + " (" + code + ")");
        }
    }

    /**
     * Converts a configuration section to a Map.
     * @param section The configuration section.
     * @return A map containing the section's keys and values.
     */
    private Map<String, Object> convertSectionToMap(ConfigurationSection section) {
        Map<String, Object> map = new HashMap<>();
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            if (section.isConfigurationSection(key)) {
                map.put(key, convertSectionToMap(section.getConfigurationSection(key)));
            } else {
                map.put(key, section.get(key));
            }
        }
        return map;
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

    /**
     * Sets up the database connection based on configuration.
     */
    private void setupDatabase() {
        String dbType = getConfig().getString("database.type", "sqlite");

        if (dbType.equalsIgnoreCase("sqlite")) {
            String dbName = getConfig().getString("database.sqlite.file", "mcidentity.db");
            File dbFile = new File(getDataFolder(), dbName);

            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }

            MCIdentitySQLite sqlite = new MCIdentitySQLite(dbFile.getAbsolutePath()) {};
            new MCIdentityProvider(sqlite);

            initializeDatabase("SQLite: " + dbName);
        } else if (dbType.equalsIgnoreCase("postgresql")) {
            String host = getConfig().getString("database.postgresql.host", "localhost");
            int port = getConfig().getInt("database.postgresql.port", 5432);
            String database = getConfig().getString("database.postgresql.database", "mcidentity");
            String username = getConfig().getString("database.postgresql.username", "postgres");
            String password = getConfig().getString("database.postgresql.password", "password");

            MCIdentityPostgreSQL postgres = new MCIdentityPostgreSQL(host, port, database, username, password) {};
            new MCIdentityProvider(postgres);

            initializeDatabase("PostgreSQL: " + database);
        } else if (dbType.equalsIgnoreCase("mongodb")) {
            String uri = getConfig().getString("database.mongodb.uri", "mongodb://localhost:27017");
            String database = getConfig().getString("database.mongodb.database", "mcidentity");

            MCIdentityMongoDB mongo = new MCIdentityMongoDB(uri, database) {};
            new MCIdentityProvider(mongo);

            initializeDatabase("MongoDB: " + database);
        }
    }

    /**
     * Registers professions and logs the outcome for the chosen backend.
     * @param backendLabel A human readable label describing the active backend.
     */
    private void initializeDatabase(String backendLabel) {
        registerProfessionsSequentially()
            .thenRun(() -> getLogger().info("Database schema and professions initialized using " + backendLabel))
            .exceptionally(ex -> {
                getLogger().severe("Failed to initialize database schema: " + ex.getMessage());
                ex.printStackTrace();
                return null;
            });
    }

    /**
     * Registers all loaded professions in the database sequentially.
     * @return A CompletableFuture representing the registration process.
     */
    private CompletableFuture<Void> registerProfessionsSequentially() {
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        for (Map.Entry<String, String> entry : MCIdentityManager.getRegisteredProfessions().entrySet()) {
            future = future.thenCompose(v -> MCIdentityProvider.instance.createProfession(entry.getKey(), entry.getValue()));
        }
        return future;
    }

    @Override
    public void onDisable() {
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

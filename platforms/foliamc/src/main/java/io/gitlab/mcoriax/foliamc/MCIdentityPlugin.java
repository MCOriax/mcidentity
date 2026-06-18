package io.gitlab.mcoriax.foliamc;

import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.common.db.postgresql.MCIdentityPostgreSQL;
import io.gitlab.mcoriax.common.db.sqlite.MCIdentitySQLite;
import io.gitlab.mcoriax.common.db.mongodb.MCIdentityMongoDB;
import io.gitlab.mcoriax.bukkit.commands.MCIdentityCommand;
import io.gitlab.mcoriax.bukkit.listeners.MCIdentityInventoryListener;
import io.gitlab.mcoriax.bukkit.listeners.MCIdentityListener;
import io.gitlab.mcoriax.bukkit.listeners.MCIdentityStatsListener;
import io.gitlab.mcoriax.bukkit.tabcompleter.MCIdentityTabCompleter;
import io.gitlab.mcoriax.bukkit.scheduler.Schedulers;
import io.gitlab.mcoriax.foliamc.scheduler.FoliaPlatformScheduler;
import io.gitlab.mcoriax.bukkit.util.ExperienceManager;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MCIdentityPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        Schedulers.set(new FoliaPlatformScheduler(this));

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
            saveResource("professions/tanker.yml", false);
            saveResource("professions/fighter.yml", false);
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
                    // Index entries from 1 to preserve the previous level-based keys
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
            MCIdentityProvider provider = new MCIdentityProvider(sqlite);

            provider.initializeSchema().thenCompose(v -> registerProfessionsSequentially())
                .thenRun(() -> getLogger().info("Database schema and professions initialized using SQLite: " + dbName))
                .exceptionally(ex -> {
                    getLogger().severe("Failed to initialize database schema: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
        } else if (dbType.equalsIgnoreCase("postgresql")) {
            String host = getConfig().getString("database.postgresql.host", "localhost");
            int port = getConfig().getInt("database.postgresql.port", 5432);
            String database = getConfig().getString("database.postgresql.database", "mcidentity");
            String username = getConfig().getString("database.postgresql.username", "postgres");
            String password = getConfig().getString("database.postgresql.password", "password");

            MCIdentityPostgreSQL postgres = new MCIdentityPostgreSQL(host, port, database, username, password) {};
            MCIdentityProvider provider = new MCIdentityProvider(postgres);

            provider.initializeSchema().thenCompose(v -> registerProfessionsSequentially())
                .thenRun(() -> getLogger().info("Database schema and professions initialized using PostgreSQL: " + database))
                .exceptionally(ex -> {
                    getLogger().severe("Failed to initialize database schema: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
        } else if (dbType.equalsIgnoreCase("mongodb")) {
            String uri = getConfig().getString("database.mongodb.uri", "mongodb://localhost:27017");
            String database = getConfig().getString("database.mongodb.database", "mcidentity");

            MCIdentityMongoDB mongo = new MCIdentityMongoDB(uri, database) {};
            MCIdentityProvider provider = new MCIdentityProvider(mongo);

            provider.initializeSchema().thenCompose(v -> registerProfessionsSequentially())
                .thenRun(() -> getLogger().info("Database schema and professions initialized using MongoDB: " + database))
                .exceptionally(ex -> {
                    getLogger().severe("Failed to initialize database schema: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
        }
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
        
        // Step 3: Gracefully close connection pool
        if (MCIdentityProvider.hasDatabase()) {
            MCIdentityProvider.instance.shutdown();
        }

        getLogger().info("MCIdentityPlugin has been disabled!");
    }
}

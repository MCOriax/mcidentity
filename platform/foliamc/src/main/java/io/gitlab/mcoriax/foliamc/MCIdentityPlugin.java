package io.gitlab.mcoriax.foliamc;

import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.common.db.postgresql.MCIdentityPostgreSQL;
import io.gitlab.mcoriax.common.db.sqlite.MCIdentitySQLite;
import io.gitlab.mcoriax.common.db.mongodb.MCIdentityMongoDB;
import io.gitlab.mcoriax.foliamc.commands.MCIdentityCommand;
import io.gitlab.mcoriax.foliamc.listeners.MCIdentityListener;
import io.gitlab.mcoriax.foliamc.listeners.MCIdentityStatsListener;
import io.gitlab.mcoriax.foliamc.tabcompleter.MCIdentityTabCompleter;
import io.github.mcengine.mcextension.common.MCExtensionManager;
import io.gitlab.mcoriax.foliamc.util.ExperienceManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

public class MCIdentityPlugin extends JavaPlugin {

    private MCExtensionManager extensionManager;
    private FileConfiguration texturesConfig;
    private FileConfiguration skinsConfig;
    private ExperienceManager experienceManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveDefaultConfig("textures", false);
        saveDefaultConfig("skins", false);
        
        loadCustomConfigs();
        if (!getConfig().getBoolean("enable", false)) {
            getLogger().warning("Plugin is disabled in config.yml. Disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.experienceManager = new ExperienceManager(this);
        
        setupDatabase();
        
        getCommand("identity").setExecutor(new MCIdentityCommand(this));
        getCommand("identity").setTabCompleter(new MCIdentityTabCompleter());

        getServer().getPluginManager().registerEvents(new MCIdentityListener(this), this);
        getServer().getPluginManager().registerEvents(new MCIdentityStatsListener(this), this);

        // Initialize MCExtensionManager
        Executor foliaExecutor = task -> Bukkit.getAsyncScheduler().runNow(this, scheduledTask -> task.run());
        this.extensionManager = new MCExtensionManager(-1);
        this.extensionManager.loadAllExtensions(this, foliaExecutor);
        
        getLogger().info("MCIdentityPlugin has been enabled!");
    }

    private void loadCustomConfigs() {
        File texturesFile = new File(getDataFolder(), "textures.yml");
        texturesConfig = YamlConfiguration.loadConfiguration(texturesFile);

        File skinsFile = new File(getDataFolder(), "skins.yml");
        skinsConfig = YamlConfiguration.loadConfiguration(skinsFile);

        loadProfessions();
    }

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
            io.gitlab.mcoriax.common.MCIdentityManager.setProfessionDisplayName(code, displayName);

            java.util.Map<String, String> expConfig = new java.util.HashMap<>();
            java.util.Map<String, Integer> baseStats = new java.util.HashMap<>();
            
            int maxLevel = config.getInt("level.max", 100);
            int expUp = config.getInt("level.exp_up", 10000);
            io.gitlab.mcoriax.common.MCIdentityManager.setProfessionLevelConfig(code, maxLevel, expUp);

            String statsRandom = config.getString("stats_random", "0");
            io.gitlab.mcoriax.common.MCIdentityManager.setProfessionStatsRandom(code, statsRandom);

            if (config.isConfigurationSection("stats")) {
                org.bukkit.configuration.ConfigurationSection statsSection = config.getConfigurationSection("stats");
                if (statsSection != null) {
                    for (String statKey : statsSection.getKeys(false)) {
                        baseStats.put(statKey, statsSection.getInt(statKey));
                    }
                }
            }
            io.gitlab.mcoriax.common.MCIdentityManager.setProfessionBaseStats(code, baseStats);

            for (String key : config.getKeys(false)) {
                if (key.equals("system_code") || key.equals("display_name") || key.equals("level") || key.equals("stats") || key.equals("stats_random")) continue;
                expConfig.put(key, config.getString(key));
            }
            
            io.gitlab.mcoriax.common.MCIdentityManager.setProfessionExpConfig(code, expConfig);
            getLogger().info("Loaded profession config for: " + displayName + " (" + code + ")");
        }
    }

    public void saveDefaultConfig(String name, boolean replace) {
        File file = new File(getDataFolder(), name + ".yml");
        if (!file.exists() || replace) {
            saveResource(name + ".yml", replace);
        }
    }

    public ExperienceManager getExperienceManager() {
        return experienceManager;
    }

    public FileConfiguration getTexturesConfig() {
        return texturesConfig;
    }

    public FileConfiguration getSkinsConfig() {
        return skinsConfig;
    }

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
            
            sqlite.initializeSchema().thenRun(() -> {
                getLogger().info("Database schema initialized using SQLite: " + dbName);
                // Register professions dynamically from loaded configs
                io.gitlab.mcoriax.common.MCIdentityManager.getRegisteredProfessions().forEach((code, name) -> {
                    MCIdentityProvider.instance.createProfession(code, name);
                });
            }).exceptionally(ex -> {
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
            new MCIdentityProvider(postgres);
            
            postgres.initializeSchema().thenRun(() -> {
                getLogger().info("Database schema initialized using PostgreSQL: " + database);
                // Register professions dynamically from loaded configs
                io.gitlab.mcoriax.common.MCIdentityManager.getRegisteredProfessions().forEach((code, name) -> {
                    MCIdentityProvider.instance.createProfession(code, name);
                });
            }).exceptionally(ex -> {
                getLogger().severe("Failed to initialize database schema: " + ex.getMessage());
                ex.printStackTrace();
                return null;
            });
        } else if (dbType.equalsIgnoreCase("mongodb")) {
            String uri = getConfig().getString("database.mongodb.uri", "mongodb://localhost:27017");
            String database = getConfig().getString("database.mongodb.database", "mcidentity");

            MCIdentityMongoDB mongo = new MCIdentityMongoDB(uri, database) {};
            new MCIdentityProvider(mongo);
            
            mongo.initializeSchema().thenRun(() -> {
                getLogger().info("Database schema initialized using MongoDB: " + database);
                // Register professions dynamically from loaded configs
                io.gitlab.mcoriax.common.MCIdentityManager.getRegisteredProfessions().forEach((code, name) -> {
                    MCIdentityProvider.instance.createProfession(code, name);
                });
            }).exceptionally(ex -> {
                getLogger().severe("Failed to initialize database schema: " + ex.getMessage());
                ex.printStackTrace();
                return null;
            });
        }
    }

    @Override
    public void onDisable() {
        // Step 3: Gracefully close connection pool
        if (MCIdentityProvider.hasDatabase()) {
            MCIdentityProvider.instance.shutdown();
        }

        if (extensionManager != null) {
            Executor shutdownExecutor = task -> Bukkit.getGlobalRegionScheduler().execute(this, task);
            extensionManager.disableAllExtensions(this, shutdownExecutor);
        }
        getLogger().info("MCIdentityPlugin has been disabled!");
    }
}

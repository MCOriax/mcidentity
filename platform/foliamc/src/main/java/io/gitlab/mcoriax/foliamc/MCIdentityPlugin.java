package io.gitlab.mcoriax.foliamc;

import io.gitlab.mcoriax.common.MCIdentityManager;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class MCIdentityPlugin extends JavaPlugin {

    private MCExtensionManager extensionManager;
    private FileConfiguration texturesConfig;
    private ExperienceManager experienceManager;
    private MCIdentityStatsListener statsListener;
    private final Map<UUID, Long> swapCooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> pendingEquips = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveDefaultConfig("textures", false);
        
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
        this.statsListener = new MCIdentityStatsListener(this);
        getServer().getPluginManager().registerEvents(this.statsListener, this);

        // Initialize MCExtensionManager
        Executor foliaExecutor = task -> Bukkit.getAsyncScheduler().runNow(this, scheduledTask -> task.run());
        this.extensionManager = new MCExtensionManager(-1);
        this.extensionManager.loadAllExtensions(this, foliaExecutor);
        
        getLogger().info("MCIdentityPlugin has been enabled!");
    }

    private void loadCustomConfigs() {
        File texturesFile = new File(getDataFolder(), "textures.yml");
        texturesConfig = YamlConfiguration.loadConfiguration(texturesFile);

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

            if (config.isConfigurationSection("skin")) {
                ConfigurationSection skinSection = config.getConfigurationSection("skin");
                if (skinSection != null) {
                    Map<Integer, String> skins = new HashMap<>();
                    for (String key : skinSection.getKeys(false)) {
                        try {
                            int level = Integer.parseInt(key);
                            skins.put(level, skinSection.getString(key));
                        } catch (NumberFormatException ignored) {}
                    }
                    MCIdentityManager.setProfessionSkins(code, skins);
                }
            }

            for (String key : config.getKeys(false)) {
                if (key.equals("system_code") || key.equals("display_name") || key.equals("level") || key.equals("stats") || key.equals("stats_random") || key.equals("skin")) continue;
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

    public boolean isOnSwapCooldown(UUID playerId) {
        int cooldownMinutes = getConfig().getInt("identity_swap_cooldown", 0);
        if (cooldownMinutes <= 0) return false;

        Long lastSwap = swapCooldowns.get(playerId);
        if (lastSwap == null) return false;

        long elapsedMillis = System.currentTimeMillis() - lastSwap;
        long cooldownMillis = TimeUnit.MINUTES.toMillis(cooldownMinutes);

        if (elapsedMillis >= cooldownMillis) {
            swapCooldowns.remove(playerId);
            return false;
        }
        return true;
    }

    public void setSwapCooldown(UUID playerId) {
        swapCooldowns.put(playerId, System.currentTimeMillis());
    }

    public void clearSwapCooldown(UUID playerId) {
        swapCooldowns.remove(playerId);
    }

    public boolean isEquipPending(UUID playerId) {
        return pendingEquips.contains(playerId);
    }

    public void setEquipPending(UUID playerId, boolean pending) {
        if (pending) {
            pendingEquips.add(playerId);
        } else {
            pendingEquips.remove(playerId);
        }
    }

    public long getRemainingSwapCooldownMillis(UUID playerId) {
        int cooldownMinutes = getConfig().getInt("identity_swap_cooldown", 0);
        Long lastSwap = swapCooldowns.get(playerId);
        if (lastSwap == null) return 0;

        long elapsedMillis = System.currentTimeMillis() - lastSwap;
        long cooldownMillis = TimeUnit.MINUTES.toMillis(cooldownMinutes);
        long remainingMillis = cooldownMillis - elapsedMillis;

        return Math.max(0, remainingMillis);
    }

    public String formatTime(long millis) {
        if (millis <= 0) return "0s";
        
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (seconds > 0 || minutes == 0) {
            sb.append(seconds).append("s");
        }
        return sb.toString().trim();
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

    public MCIdentityStatsListener getStatsListener() {
        return statsListener;
    }

    public FileConfiguration getTexturesConfig() {
        return texturesConfig;
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
                MCIdentityManager.getRegisteredProfessions().forEach((code, name) -> {
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
                MCIdentityManager.getRegisteredProfessions().forEach((code, name) -> {
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
                MCIdentityManager.getRegisteredProfessions().forEach((code, name) -> {
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
        if (statsListener != null) {
            statsListener.cancelTasks();
        }
        
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

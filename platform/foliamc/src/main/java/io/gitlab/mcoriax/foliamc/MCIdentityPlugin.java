package io.gitlab.mcoriax.foliamc;

import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.common.db.postgresql.MCIdentityPostgreSQL;
import io.gitlab.mcoriax.common.db.sqlite.MCIdentitySQLite;
import io.gitlab.mcoriax.common.db.mongodb.MCIdentityMongoDB;
import io.gitlab.mcoriax.foliamc.commands.MCIdentityCommand;
import io.gitlab.mcoriax.foliamc.listeners.MCIdentityListener;
import io.gitlab.mcoriax.foliamc.tabcompleter.MCIdentityTabCompleter;
import io.github.mcengine.mcextension.common.MCExtensionManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.concurrent.Executor;

public class MCIdentityPlugin extends JavaPlugin {

    private MCExtensionManager extensionManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        if (!getConfig().getBoolean("enable", false)) {
            getLogger().warning("Plugin is disabled in config.yml. Disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        setupDatabase();
        
        getCommand("identity").setExecutor(new MCIdentityCommand(this));
        getCommand("identity").setTabCompleter(new MCIdentityTabCompleter());

        getServer().getPluginManager().registerEvents(new MCIdentityListener(this), this);

        // Initialize MCExtensionManager
        Executor foliaExecutor = task -> Bukkit.getAsyncScheduler().runNow(this, scheduledTask -> task.run());
        this.extensionManager = new MCExtensionManager(-1);
        this.extensionManager.loadAllExtensions(this, foliaExecutor);
        
        getLogger().info("MCIdentityPlugin has been enabled!");
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
                MCIdentityProvider.instance.createProfession("tanker", "Tanker");
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
                MCIdentityProvider.instance.createProfession("tanker", "Tanker");
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
                MCIdentityProvider.instance.createProfession("tanker", "Tanker");
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

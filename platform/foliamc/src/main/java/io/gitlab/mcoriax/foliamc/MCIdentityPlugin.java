package io.gitlab.mcoriax.foliamc;

import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.common.db.sqlite.MCIdentitySQLite;
import io.gitlab.mcoriax.foliamc.commands.MCIdentityCommand;
import io.gitlab.mcoriax.foliamc.listeners.MCIdentityListener;
import io.gitlab.mcoriax.foliamc.tabcompleter.MCIdentityTabCompleter;
import io.github.mcengine.mcextension.common.MCExtensionManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
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
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                getLogger().severe("SQLite JDBC driver not found! Database operations will fail.");
                e.printStackTrace();
                return;
            }

            String dbName = getConfig().getString("database.sqlite.file", "mcidentity.db");
            File dbFile = new File(getDataFolder(), dbName);
            
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }

            MCIdentitySQLite sqlite = new MCIdentitySQLite(dbFile.getAbsolutePath()) {
                @Override
                protected Connection getConnection() throws SQLException {
                    return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
                }
            };
            
            MCIdentityProvider.setDatabase(sqlite);
            sqlite.initializeSchema().join(); // Initialize tables
            getLogger().info("Database initialized using SQLite: " + dbName);
        }
        // PostgreSQL can be added here later
    }

    @Override
    public void onDisable() {
        if (extensionManager != null) {
            Executor shutdownExecutor = task -> Bukkit.getGlobalRegionScheduler().execute(this, task);
            extensionManager.disableAllExtensions(this, shutdownExecutor);
        }
        getLogger().info("MCIdentityPlugin has been disabled!");
    }
}

package io.github.mcoriax.engine;

import io.github.mcoriax.bukkit.AbstractMCIdentityPlugin;
import io.github.mcoriax.bukkit.scheduler.BukkitPlatformScheduler;
import io.github.mcoriax.bukkit.scheduler.PlatformScheduler;
import io.github.mcoriax.foliamc.scheduler.FoliaPlatformScheduler;

/**
 * Universal entry point.
 * <p>
 * A single jar supports SpigotMC, PaperMC, and Folia. The running server is
 * detected at enable time and the matching scheduler is installed, so the same
 * artifact behaves correctly on every platform without separate downloads. The
 * rest of the bootstrap is inherited from {@link AbstractMCIdentityPlugin}.
 * </p>
 */
public class MCIdentityEnginePlugin extends AbstractMCIdentityPlugin {

    /**
     * The server platforms the universal jar can run on.
     */
    public enum ServerPlatform {
        /**
         * Folia and its regionised, multi-threaded schedulers.
         */
        FOLIA,
        /**
         * PaperMC and its downstream forks.
         */
        PAPER,
        /**
         * SpigotMC and its downstream forks (the fallback).
         */
        SPIGOT
    }

    /**
     * Detects which server platform this jar is running on.
     * <p>
     * Folia is probed first (its regionised server class only exists there),
     * then Paper (its config class is absent on plain Spigot), and Spigot is
     * used as the fallback when neither is present.
     * </p>
     * @return the detected {@link ServerPlatform}.
     */
    private ServerPlatform detectPlatform() {
        if (classExists("io.papermc.paper.threadedregions.RegionizedServer")) {
            return ServerPlatform.FOLIA;
        }
        if (classExists("com.destroystokyo.paper.PaperConfig")) {
            return ServerPlatform.PAPER;
        }
        return ServerPlatform.SPIGOT;
    }

    /**
     * Checks whether a class is present on the runtime classpath.
     * @param className The fully qualified class name to probe.
     * @return True if the class can be loaded, false otherwise.
     */
    private boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    protected PlatformScheduler createScheduler() {
        ServerPlatform platform = detectPlatform();
        switch (platform) {
            case FOLIA:
                getLogger().info("Detected Folia server. Installing the region-aware Folia scheduler.");
                return new FoliaPlatformScheduler(this);
            case PAPER:
                getLogger().info("Detected Paper server. Installing the Bukkit scheduler.");
                return new BukkitPlatformScheduler(this);
            case SPIGOT:
            default:
                getLogger().info("Detected Spigot server. Installing the Bukkit scheduler.");
                return new BukkitPlatformScheduler(this);
        }
    }
}

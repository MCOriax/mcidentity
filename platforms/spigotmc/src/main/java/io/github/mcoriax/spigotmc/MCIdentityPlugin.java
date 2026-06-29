package io.github.mcoriax.spigotmc;

import io.github.mcoriax.bukkit.AbstractMCIdentityPlugin;
import io.github.mcoriax.bukkit.scheduler.BukkitPlatformScheduler;
import io.github.mcoriax.bukkit.scheduler.PlatformScheduler;

/**
 * SpigotMC entry point. Installs the classic Bukkit scheduler and inherits the
 * shared bootstrap from {@link AbstractMCIdentityPlugin}.
 */
public class MCIdentityPlugin extends AbstractMCIdentityPlugin {

    @Override
    protected PlatformScheduler createScheduler() {
        return new BukkitPlatformScheduler(this);
    }
}

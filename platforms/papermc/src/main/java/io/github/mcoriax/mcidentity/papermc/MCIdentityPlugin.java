package io.github.mcoriax.mcidentity.papermc;

import io.github.mcoriax.mcidentity.bukkit.AbstractMCIdentityPlugin;
import io.github.mcoriax.mcidentity.bukkit.scheduler.BukkitPlatformScheduler;
import io.github.mcoriax.mcidentity.bukkit.scheduler.PlatformScheduler;

/**
 * PaperMC entry point. Installs the classic Bukkit scheduler and inherits the
 * shared bootstrap from {@link AbstractMCIdentityPlugin}.
 */
public class MCIdentityPlugin extends AbstractMCIdentityPlugin {

    @Override
    /**
     * {@inheritDoc}
     */
    protected PlatformScheduler createScheduler() {
        return new BukkitPlatformScheduler(this);
    }
}

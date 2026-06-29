package io.github.mcoriax.foliamc;

import io.github.mcoriax.bukkit.AbstractMCIdentityPlugin;
import io.github.mcoriax.bukkit.scheduler.PlatformScheduler;
import io.github.mcoriax.foliamc.scheduler.FoliaPlatformScheduler;

/**
 * Folia entry point. Installs the region aware scheduler and inherits the shared
 * bootstrap from {@link AbstractMCIdentityPlugin}.
 */
public class MCIdentityPlugin extends AbstractMCIdentityPlugin {

    @Override
    protected PlatformScheduler createScheduler() {
        return new FoliaPlatformScheduler(this);
    }
}

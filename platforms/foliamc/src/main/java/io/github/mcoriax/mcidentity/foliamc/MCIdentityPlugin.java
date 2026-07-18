package io.github.mcoriax.mcidentity.foliamc;

import io.github.mcoriax.mcidentity.bukkit.AbstractMCIdentityPlugin;
import io.github.mcoriax.mcidentity.bukkit.scheduler.PlatformScheduler;
import io.github.mcoriax.mcidentity.foliamc.scheduler.FoliaPlatformScheduler;

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

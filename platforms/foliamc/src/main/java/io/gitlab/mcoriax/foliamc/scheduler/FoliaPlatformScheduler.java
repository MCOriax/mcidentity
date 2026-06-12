package io.gitlab.mcoriax.foliamc.scheduler;

import io.gitlab.mcoriax.bukkit.scheduler.PlatformScheduler;
import io.gitlab.mcoriax.bukkit.scheduler.PlatformTask;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * {@link PlatformScheduler} backed by Folia's region-aware schedulers.
 * <p>
 * Entity-bound work is dispatched through the owning entity's region scheduler,
 * and asynchronous work through the global async scheduler, so the shared bukkit
 * logic keeps tick-thread correctness on a regionised server.
 * </p>
 */
public final class FoliaPlatformScheduler implements PlatformScheduler {

    private final Plugin plugin;

    /**
     * @param plugin the owning plugin used to schedule tasks
     */
    public FoliaPlatformScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runForEntity(Entity entity, Runnable task, Runnable retired) {
        entity.getScheduler().run(plugin, scheduledTask -> task.run(), retired);
    }

    @Override
    public void runAsync(Runnable task) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
    }

    @Override
    public PlatformTask runAsyncAtFixedRate(Runnable task, long initialDelayMillis, long periodMillis) {
        ScheduledTask handle = plugin.getServer().getAsyncScheduler().runAtFixedRate(
                plugin, scheduledTask -> task.run(), initialDelayMillis, periodMillis, TimeUnit.MILLISECONDS);
        return handle::cancel;
    }
}

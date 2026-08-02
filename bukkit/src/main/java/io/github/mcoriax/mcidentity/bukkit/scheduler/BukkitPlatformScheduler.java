package io.github.mcoriax.mcidentity.bukkit.scheduler;

import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * {@link PlatformScheduler} backed by the classic {@code BukkitScheduler}.
 * <p>
 * Suitable for Spigot and Paper, where all entity/world state lives on a single
 * main thread. Folia, which partitions the world into independently ticking
 * regions, requires a dedicated region-aware implementation instead.
 * </p>
 */
public final class BukkitPlatformScheduler implements PlatformScheduler {

    private static final long MILLIS_PER_TICK = 50L;

    private final Plugin plugin;

    /**
     * @param plugin the owning plugin used to schedule tasks
     */
    public BukkitPlatformScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public void runForEntity(Entity entity, Runnable task, Runnable retired) {
        // On a single-threaded server the entity always lives on the main thread.
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public void runAsync(Runnable task) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public PlatformTask runAsyncAtFixedRate(Runnable task, long initialDelayMillis, long periodMillis) {
        long delayTicks = Math.max(1L, initialDelayMillis / MILLIS_PER_TICK);
        long periodTicks = Math.max(1L, periodMillis / MILLIS_PER_TICK);
        BukkitTask handle = plugin.getServer().getScheduler()
                .runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        return handle::cancel;
    }
}

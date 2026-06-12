package io.gitlab.mcoriax.bukkit.scheduler;

import org.bukkit.entity.Entity;

/**
 * Platform-agnostic scheduling abstraction for the shared bukkit module.
 * <p>
 * The shared code targets the plain spigot-api and therefore cannot reference
 * Paper's {@code AsyncScheduler} or Folia's region/entity schedulers directly.
 * Each platform module supplies its own implementation:
 * </p>
 * <ul>
 *   <li>Spigot &amp; Paper: {@link BukkitPlatformScheduler} (classic Bukkit scheduler).</li>
 *   <li>Folia: a region-aware implementation living in the foliamc platform module.</li>
 * </ul>
 */
public interface PlatformScheduler {

    /**
     * Runs a task on the thread that owns the given entity.
     * <p>
     * On Folia this is the entity's region thread; on Spigot/Paper this is the
     * main server thread. Use for entity/world state mutations.
     * </p>
     *
     * @param entity  the entity whose owning thread should execute the task
     * @param task    the task to run
     * @param retired optional callback invoked if the entity is no longer valid
     *                (e.g. logged out) before the task could run; may be {@code null}
     */
    void runForEntity(Entity entity, Runnable task, Runnable retired);

    /**
     * Runs a task asynchronously, off the main server thread.
     *
     * @param task the task to run
     */
    void runAsync(Runnable task);

    /**
     * Schedules a repeating asynchronous task.
     *
     * @param task               the task to run on each interval
     * @param initialDelayMillis delay before the first execution, in milliseconds
     * @param periodMillis       interval between executions, in milliseconds
     * @return a handle that can cancel the repeating task
     */
    PlatformTask runAsyncAtFixedRate(Runnable task, long initialDelayMillis, long periodMillis);
}

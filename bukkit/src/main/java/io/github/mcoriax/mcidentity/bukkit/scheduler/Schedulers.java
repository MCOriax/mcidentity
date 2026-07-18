package io.github.mcoriax.mcidentity.bukkit.scheduler;

import org.bukkit.entity.Entity;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Static access point to the active {@link PlatformScheduler}.
 * <p>
 * Each platform plugin installs its scheduler during {@code onEnable} via
 * {@link #set(PlatformScheduler)}; shared bukkit code then schedules work
 * through the convenience methods here without knowing the platform.
 * </p>
 */
public final class Schedulers {

    private static PlatformScheduler scheduler;

    private Schedulers() {
    }

    /**
     * Installs the platform scheduler implementation. Call once during plugin enable.
     *
     * @param impl the platform-specific scheduler
     */
    public static void set(PlatformScheduler impl) {
        scheduler = impl;
    }

    /**
     * @return the active platform scheduler, or {@code null} if none was installed
     */
    public static PlatformScheduler get() {
        return scheduler;
    }

    /**
     * Runs a task on the thread that owns the given entity (no retired callback).
     *
     * @param entity the entity whose owning thread should execute the task
     * @param task   the task to run
     */
    public static void runForEntity(Entity entity, Runnable task) {
        scheduler.runForEntity(entity, task, null);
    }

    /**
     * Runs a task on the thread that owns the given entity.
     *
     * @param entity  the entity whose owning thread should execute the task
     * @param task    the task to run
     * @param retired callback invoked if the entity is no longer valid; may be {@code null}
     */
    public static void runForEntity(Entity entity, Runnable task, Runnable retired) {
        scheduler.runForEntity(entity, task, retired);
    }

    /**
     * Runs a task asynchronously, off the main server thread.
     *
     * @param task the task to run
     */
    public static void runAsync(Runnable task) {
        scheduler.runAsync(task);
    }

    /**
     * Schedules a repeating asynchronous task.
     *
     * @param task               the task to run on each interval
     * @param initialDelayMillis delay before the first execution, in milliseconds
     * @param periodMillis       interval between executions, in milliseconds
     * @return a handle that can cancel the repeating task
     */
    public static PlatformTask runAsyncAtFixedRate(Runnable task, long initialDelayMillis, long periodMillis) {
        return scheduler.runAsyncAtFixedRate(task, initialDelayMillis, periodMillis);
    }

    /**
     * Returns a bridge mirroring the Folia {@code entity.getScheduler()} call shape,
     * so callers can keep their {@code run(plugin, task, retired)} form unchanged.
     *
     * @param entity the entity whose owning thread should execute scheduled tasks
     * @return an entity-scheduler bridge
     */
    public static EntityBridge forEntity(Entity entity) {
        return new EntityBridge(entity);
    }

    /**
     * Returns a bridge mirroring the Paper/Folia {@code getAsyncScheduler()} call shape.
     *
     * @return an async-scheduler bridge
     */
    public static AsyncBridge async() {
        return AsyncBridge.INSTANCE;
    }

    /**
     * Bridge mirroring Folia's per-entity scheduler. The {@code plugin} argument and
     * the task's {@code ScheduledTask}-shaped parameter are accepted for source
     * compatibility and ignored.
     */
    public static final class EntityBridge {
        private final Entity entity;

        private EntityBridge(Entity entity) {
            this.entity = entity;
        }

        /**
         * Runs the task on the entity's owning thread.
         *
         * @param pluginIgnored accepted for compatibility; ignored
         * @param task          task to run (its argument is always {@code null})
         * @param retired       optional callback if the entity is no longer valid
         */
        public void run(Object pluginIgnored, Consumer<Object> task, Runnable retired) {
            scheduler.runForEntity(entity, () -> task.accept(null), retired);
        }
    }

    /**
     * Bridge mirroring the Paper/Folia async scheduler.
     */
    public static final class AsyncBridge {
        private static final AsyncBridge INSTANCE = new AsyncBridge();

        private AsyncBridge() {
        }

        /**
         * Runs the task asynchronously, as soon as possible.
         *
         * @param pluginIgnored accepted for compatibility; ignored
         * @param task          task to run (its argument is always {@code null})
         */
        public void runNow(Object pluginIgnored, Consumer<Object> task) {
            scheduler.runAsync(() -> task.accept(null));
        }

        /**
         * Schedules a repeating async task.
         *
         * @param pluginIgnored accepted for compatibility; ignored
         * @param task          task to run on each interval (its argument is always {@code null})
         * @param initialDelay  initial delay in {@code unit}
         * @param period        interval in {@code unit}
         * @param unit          time unit for the delay and period
         * @return a handle that can cancel the repeating task
         */
        public PlatformTask runAtFixedRate(Object pluginIgnored, Consumer<Object> task, long initialDelay, long period, TimeUnit unit) {
            return scheduler.runAsyncAtFixedRate(() -> task.accept(null), unit.toMillis(initialDelay), unit.toMillis(period));
        }
    }
}

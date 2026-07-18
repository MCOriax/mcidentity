package io.github.mcoriax.mcidentity.bukkit.scheduler;

/**
 * Handle to a scheduled task that can be cancelled.
 * <p>
 * Abstracts over the platform-specific task handle (Bukkit task id on
 * Spigot/Paper, {@code ScheduledTask} on Folia) so shared code can cancel
 * repeating work without referencing a platform scheduler type.
 * </p>
 */
public interface PlatformTask {

    /**
     * Cancels this task. Safe to call multiple times.
     */
    void cancel();
}

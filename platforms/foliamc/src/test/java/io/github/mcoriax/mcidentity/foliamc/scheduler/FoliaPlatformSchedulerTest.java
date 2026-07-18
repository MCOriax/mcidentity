package io.github.mcoriax.mcidentity.foliamc.scheduler;

import io.github.mcoriax.mcidentity.bukkit.scheduler.PlatformTask;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link FoliaPlatformScheduler}, verifying that each scheduling
 * call is routed onto the correct Folia region-aware scheduler for its thread
 * context (entity region vs. global async).
 */
class FoliaPlatformSchedulerTest {

    private Plugin plugin;
    private Server server;
    private AsyncScheduler asyncScheduler;
    private FoliaPlatformScheduler scheduler;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        server = mock(Server.class);
        asyncScheduler = mock(AsyncScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getAsyncScheduler()).thenReturn(asyncScheduler);
        scheduler = new FoliaPlatformScheduler(plugin);
    }

    @Test
    @DisplayName("runForEntity dispatches through the entity's own region scheduler")
    void testRunForEntity() {
        Entity entity = mock(Entity.class);
        EntityScheduler entityScheduler = mock(EntityScheduler.class);
        when(entity.getScheduler()).thenReturn(entityScheduler);
        Runnable task = mock(Runnable.class);
        Runnable retired = mock(Runnable.class);

        scheduler.runForEntity(entity, task, retired);

        // The entity scheduler owns the region; the retired callback must be forwarded.
        verify(entityScheduler).run(eq(plugin), any(), eq(retired));
    }

    @Test
    @DisplayName("runAsync dispatches through the global async scheduler")
    void testRunAsync() {
        Runnable task = mock(Runnable.class);

        scheduler.runAsync(task);

        verify(asyncScheduler).runNow(eq(plugin), any());
    }

    @Test
    @DisplayName("runAsyncAtFixedRate schedules a repeating async task whose handle cancels it")
    void testRunAsyncAtFixedRate() {
        ScheduledTask handle = mock(ScheduledTask.class);
        when(asyncScheduler.runAtFixedRate(eq(plugin), any(), eq(100L), eq(200L), eq(TimeUnit.MILLISECONDS)))
            .thenReturn(handle);

        PlatformTask platformTask = scheduler.runAsyncAtFixedRate(() -> { }, 100L, 200L);
        assertNotNull(platformTask);

        platformTask.cancel();
        verify(handle).cancel();
    }
}

package io.github.mcoriax.bukkit.util;
import org.bukkit.ChatColor;
import io.github.mcoriax.bukkit.scheduler.Schedulers;

import io.github.mcoriax.api.model.IdentityProperties;
import io.github.mcoriax.bukkit.scheduler.PlatformTask;
import io.github.mcoriax.common.BaseProfession;
import io.github.mcoriax.common.MCIdentityProvider;
import io.github.mcoriax.common.model.MutableIdentityProperties;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Grants every online player one passive EXP per second on their permanent
 * base "player" profession, regardless of which identity is equipped.
 *
 * <p>The EXP is never written to the database per tick: gains accumulate in a
 * memory buffer and are flushed in bulk every five minutes, when a player
 * quits, and when the plugin disables. Leveling has no cap and costs
 * {@link BaseProfession#EXP_PER_LEVEL} EXP per level.</p>
 */
public final class PassiveExpService {

    /**
     * Milliseconds between passive EXP ticks (one second, i.e. 20 ticks).
     */
    private static final long TICK_PERIOD_MILLIS = 1_000L;

    /**
     * Milliseconds between bulk flushes of the buffer to the database.
     */
    private static final long FLUSH_PERIOD_MILLIS = 5 * 60 * 1_000L;

    /**
     * Buffered, not-yet-persisted passive EXP per player UUID.
     */
    private static final Map<UUID, Integer> BUFFER = new ConcurrentHashMap<>();

    private static JavaPlugin plugin;
    private static PlatformTask tickTask;
    private static PlatformTask flushTask;

    private PassiveExpService() {
    }

    /**
     * Starts the passive EXP tick and the periodic bulk flush.
     * @param owningPlugin The plugin instance.
     */
    public static synchronized void start(JavaPlugin owningPlugin) {
        stopTasks();
        plugin = owningPlugin;
        tickTask = Schedulers.runAsyncAtFixedRate(PassiveExpService::tick, TICK_PERIOD_MILLIS, TICK_PERIOD_MILLIS);
        flushTask = Schedulers.runAsyncAtFixedRate(PassiveExpService::flushAll, FLUSH_PERIOD_MILLIS, FLUSH_PERIOD_MILLIS);
    }

    /**
     * Stops the tasks and flushes everything still buffered, waiting briefly
     * so the writes reach the database before the HTTP client shuts down.
     */
    public static synchronized void stop() {
        stopTasks();
        try {
            flushAllAsFuture().get(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().warning("Passive EXP flush on shutdown did not complete: " + e.getMessage());
            }
        }
        plugin = null;
    }

    /**
     * Flushes the buffered passive EXP of one player to the database. Called
     * on quit and by the periodic bulk flush.
     * @param playerUuid The UUID of the player.
     * @return A CompletableFuture that completes when the flush is done.
     */
    public static CompletableFuture<Void> flush(UUID playerUuid) {
        Integer pending = BUFFER.remove(playerUuid);
        if (pending == null || pending <= 0 || !MCIdentityProvider.hasDatabase()) {
            return CompletableFuture.completedFuture(null);
        }

        UUID baseUuid = BaseProfession.identityUuid(playerUuid);
        final int gained = pending;
        return MCIdentityProvider.instance.getProperties(baseUuid).thenCompose(optProps -> {
            if (optProps.isEmpty()) {
                // Base identity not created yet (join still in flight): retry
                // on the next flush instead of dropping the EXP.
                BUFFER.merge(playerUuid, gained, Integer::sum);
                return CompletableFuture.completedFuture(null);
            }

            IdentityProperties props = optProps.get();
            long exp = (long) props.getExp() + gained;
            int level = props.getLevel();
            int statPoints = props.getStatPoints();
            int levelsGained = 0;
            while (exp >= BaseProfession.EXP_PER_LEVEL) {
                exp -= BaseProfession.EXP_PER_LEVEL;
                level++;
                statPoints += BaseProfession.STAT_POINTS_PER_LEVEL;
                levelsGained++;
            }

            MutableIdentityProperties newProps = new MutableIdentityProperties(
                level, (int) exp, statPoints, props.getHeadText(),
                props.getSkin(), props.getAura(),
                props.getInventoryContents(), props.getEnderchestContents());

            final int finalLevel = level;
            final boolean leveledUp = levelsGained > 0;
            return MCIdentityProvider.instance.updateProperties(baseUuid, newProps).thenRun(() -> {
                if (!leveledUp || plugin == null) return;
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null && player.isOnline()) {
                    Schedulers.forEntity(player).run(plugin, st ->
                        player.sendMessage(ChatColor.GREEN + "Your base Player profession is now level " + finalLevel + "!"), null);
                }
            });
        });
    }

    /**
     * Flushes the buffered passive EXP of every player to the database.
     */
    public static void flushAll() {
        flushAllAsFuture();
    }

    private static CompletableFuture<Void> flushAllAsFuture() {
        List<CompletableFuture<Void>> flushes = new ArrayList<>();
        for (UUID playerUuid : BUFFER.keySet()) {
            flushes.add(flush(playerUuid));
        }
        return CompletableFuture.allOf(flushes.toArray(CompletableFuture[]::new));
    }

    private static void tick() {
        for (Player player : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            BUFFER.merge(player.getUniqueId(), 1, Integer::sum);
        }
    }

    private static void stopTasks() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (flushTask != null) {
            flushTask.cancel();
            flushTask = null;
        }
    }
}

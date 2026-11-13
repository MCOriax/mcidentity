package io.github.mcoriax.mcidentity.bukkit.util;
import org.bukkit.ChatColor;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;

import io.github.mcoriax.mcidentity.api.model.ActiveProfessionSession;
import io.github.mcoriax.mcidentity.api.model.PlayerStats;
import io.github.mcoriax.mcidentity.api.model.ProfessionProgress;
import io.github.mcoriax.mcidentity.bukkit.scheduler.PlatformTask;
import io.github.mcoriax.mcidentity.common.BaseProfession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import io.github.mcoriax.mcidentity.common.model.PlayerStatsRecord;
import io.github.mcoriax.mcidentity.common.model.ProfessionProgressRecord;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Grants passive playtime EXP to the player's base {@code player} profession.
 *
 * <p>One EXP accrues per online second, whichever profession is worn, and the
 * base profession levels at {@link BaseProfession#EXP_PER_LEVEL} EXP per level.
 * Each level grants {@link BaseProfession#STAT_POINTS_PER_LEVEL} stat points to
 * the player's own stats.</p>
 *
 * <p>The EXP is never written to the database per tick: gains accumulate in a
 * memory buffer keyed by the player's UUID and are flushed in bulk every five
 * minutes, when a player quits, and when the plugin disables. Leveling has no
 * cap.</p>
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
     * Buffered, not yet persisted passive EXP per player UUID.
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            warnFlushIncomplete(e);
        } catch (Exception e) {
            warnFlushIncomplete(e);
        }
        plugin = null;
    }

    /**
     * Logs that the shutdown flush did not finish, leaving buffered EXP unwritten.
     * @param cause Why the flush did not complete.
     */
    private static void warnFlushIncomplete(Exception cause) {
        if (plugin != null) {
            plugin.getLogger().warning("Passive EXP flush on shutdown did not complete: " + cause.getMessage());
        }
    }

    /**
     * Flushes the buffered passive EXP of one player to the database. Called on
     * quit and by the periodic bulk flush.
     * @param playerUuid The UUID of the player.
     * @return A CompletableFuture that completes when the flush is done.
     */
    public static CompletableFuture<Void> flush(UUID playerUuid) {
        if (!MCIdentityProvider.hasDatabase()) {
            return CompletableFuture.completedFuture(null);
        }
        Integer pending = BUFFER.remove(playerUuid);
        if (pending == null || pending <= 0) {
            return CompletableFuture.completedFuture(null);
        }
        return creditExp(playerUuid, pending).exceptionally(ex -> {
            // Storage refused the write: keep the EXP for the next flush.
            BUFFER.merge(playerUuid, pending, Integer::sum);
            return null;
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
        for (UUID playerUuid : new ArrayList<>(BUFFER.keySet())) {
            flushes.add(flush(playerUuid));
        }
        return CompletableFuture.allOf(flushes.toArray(CompletableFuture[]::new));
    }

    /**
     * Credits buffered EXP to the base profession, levelling it at
     * {@link BaseProfession#EXP_PER_LEVEL} and granting stat points per level.
     * @param playerUuid The player's UUID.
     * @param gained The buffered EXP amount.
     * @return A CompletableFuture that completes when storage is updated.
     */
    private static CompletableFuture<Void> creditExp(UUID playerUuid, int gained) {
        MCIdentityProvider provider = MCIdentityProvider.instance;
        return provider.getProgress(playerUuid, BaseProfession.CODE).thenCompose(optProgress -> {
            ProfessionProgress progress = optProgress.orElseGet(ProfessionProgressRecord::starting);
            long exp = (long) progress.getExp() + gained;
            int levelsGained = (int) (exp / BaseProfession.EXP_PER_LEVEL);
            int level = progress.getLevel() + levelsGained;
            ProfessionProgressRecord updated = new ProfessionProgressRecord(level, (int) (exp % BaseProfession.EXP_PER_LEVEL));

            CompletableFuture<Void> write = provider.updateProgress(playerUuid, BaseProfession.CODE, updated);
            if (levelsGained <= 0) {
                return write;
            }
            return write
                .thenCompose(v -> grantStatPoints(provider, playerUuid, levelsGained * BaseProfession.STAT_POINTS_PER_LEVEL))
                .thenRun(() -> announceLevelUp(playerUuid, level));
        });
    }

    /**
     * Adds stat points to the player's stats, reading the cached session first
     * so a point spent a moment ago is not overwritten by an older stored row.
     * @param provider The provider.
     * @param playerUuid The player's UUID.
     * @param points The stat points to add.
     * @return A CompletableFuture that completes when the stats are stored.
     */
    private static CompletableFuture<Void> grantStatPoints(MCIdentityProvider provider, UUID playerUuid, int points) {
        Optional<PlayerStats> cached = MCIdentityManager.getSession(playerUuid).map(ActiveProfessionSession::stats);
        CompletableFuture<Optional<PlayerStats>> statsFuture = cached.isPresent()
            ? CompletableFuture.completedFuture(cached)
            : provider.getStats(playerUuid);
        return statsFuture.thenCompose(optStats -> {
            if (optStats.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            PlayerStats stats = optStats.get();
            return provider.updateStats(playerUuid,
                PlayerStatsRecord.copyOf(stats).withStatPoints(stats.getStatPoints() + points));
        });
    }

    /**
     * Tells an online player their base profession levelled from passive EXP.
     * @param playerUuid The player's UUID.
     * @param level The level just reached.
     */
    private static void announceLevelUp(UUID playerUuid, int level) {
        if (plugin == null) {
            return;
        }
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null || !player.isOnline()) {
            return;
        }
        String message = ChatColor.GREEN + "Your base Player profession is now level " + level + "!";
        Schedulers.forEntity(player).run(plugin, st -> player.sendMessage(message), null);
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

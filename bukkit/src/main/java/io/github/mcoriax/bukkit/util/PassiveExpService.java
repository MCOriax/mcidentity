package io.github.mcoriax.bukkit.util;
import org.bukkit.ChatColor;
import io.github.mcoriax.bukkit.scheduler.Schedulers;

import io.github.mcoriax.api.model.ActiveIdentitySession;
import io.github.mcoriax.api.model.IdentityProperties;
import io.github.mcoriax.bukkit.scheduler.PlatformTask;
import io.github.mcoriax.common.BaseProfession;
import io.github.mcoriax.common.MCIdentityManager;
import io.github.mcoriax.common.MCIdentityProvider;
import io.github.mcoriax.common.model.MutableIdentityProperties;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Grants passive playtime EXP on the base "player" identities.
 *
 * <p>Two tracks accrue one EXP per second each. The <em>primary</em> base
 * identity (keyed by the player's own UUID) gains whenever the player is
 * online, regardless of which identity is currently equipped, and levels at
 * {@link BaseProfession#EXP_PER_LEVEL} EXP per level. An <em>alternative</em>
 * base identity gains only while it is the currently equipped identity and
 * levels at the faster {@link BaseProfession#ALTERNATIVE_EXP_PER_LEVEL}. A
 * second is never credited twice to the same identity: while the primary
 * identity is equipped it receives only its always-on gain.</p>
 *
 * <p>The EXP is never written to the database per tick: gains accumulate in
 * memory buffers — alternative gains stay keyed by the identity that was
 * equipped when the second ticked, so a swap never misdirects them — and are
 * flushed in bulk every five minutes, when a player quits, and when the
 * plugin disables. Leveling has no cap.</p>
 */
public final class PassiveExpService {

    /**
     * Milliseconds between passive EXP ticks (one second, i.e. 20 ticks).
     */
    private static final long TICK_PERIOD_MILLIS = 1_000L;

    /**
     * Milliseconds between bulk flushes of the buffers to the database.
     */
    private static final long FLUSH_PERIOD_MILLIS = 5 * 60 * 1_000L;

    /**
     * Buffered, not-yet-persisted primary-identity EXP per player UUID.
     */
    private static final Map<UUID, Integer> PRIMARY_BUFFER = new ConcurrentHashMap<>();

    /**
     * Buffered, not-yet-persisted alternative-identity EXP, keyed by the
     * owning player and then by the alternative identity that was equipped
     * when the EXP accrued.
     */
    private static final Map<UUID, ConcurrentHashMap<UUID, Integer>> ALTERNATIVE_BUFFER = new ConcurrentHashMap<>();

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
     * Flushes the buffered passive EXP of one player to the database: the
     * always-on primary gain plus any gains buffered for the alternative
     * identities the player had equipped. Called on quit and by the periodic
     * bulk flush. The identities are separate database rows, so the writes
     * never conflict with each other.
     * @param playerUuid The UUID of the player.
     * @return A CompletableFuture that completes when the flush is done.
     */
    public static CompletableFuture<Void> flush(UUID playerUuid) {
        if (!MCIdentityProvider.hasDatabase()) {
            return CompletableFuture.completedFuture(null);
        }

        List<CompletableFuture<Void>> flushes = new ArrayList<>();

        Integer primaryPending = PRIMARY_BUFFER.remove(playerUuid);
        if (primaryPending != null && primaryPending > 0) {
            flushes.add(creditExp(playerUuid, BaseProfession.identityUuid(playerUuid), primaryPending));
        }

        Map<UUID, Integer> alternativePending = ALTERNATIVE_BUFFER.remove(playerUuid);
        if (alternativePending != null) {
            for (Map.Entry<UUID, Integer> entry : alternativePending.entrySet()) {
                if (entry.getValue() > 0) {
                    flushes.add(creditExp(playerUuid, entry.getKey(), entry.getValue()));
                }
            }
        }

        return CompletableFuture.allOf(flushes.toArray(CompletableFuture[]::new));
    }

    /**
     * Flushes the buffered passive EXP of every player to the database.
     */
    public static void flushAll() {
        flushAllAsFuture();
    }

    private static CompletableFuture<Void> flushAllAsFuture() {
        Set<UUID> players = new HashSet<>(PRIMARY_BUFFER.keySet());
        players.addAll(ALTERNATIVE_BUFFER.keySet());
        List<CompletableFuture<Void>> flushes = new ArrayList<>();
        for (UUID playerUuid : players) {
            flushes.add(flush(playerUuid));
        }
        return CompletableFuture.allOf(flushes.toArray(CompletableFuture[]::new));
    }

    /**
     * Credits buffered EXP to one base identity, leveling it at the threshold
     * of its track: {@link BaseProfession#EXP_PER_LEVEL} for the primary
     * identity, {@link BaseProfession#ALTERNATIVE_EXP_PER_LEVEL} for an
     * alternative one.
     * @param playerUuid The owning player's UUID.
     * @param identityUuid The base identity receiving the EXP.
     * @param gained The buffered EXP amount.
     * @return A CompletableFuture that completes when the identity is updated.
     */
    private static CompletableFuture<Void> creditExp(UUID playerUuid, UUID identityUuid, int gained) {
        return MCIdentityProvider.instance.getProperties(identityUuid).thenCompose(optProps -> {
            if (optProps.isEmpty()) {
                // Identity not created yet (join or slot grant still in
                // flight): retry on the next flush instead of dropping the EXP.
                requeue(playerUuid, identityUuid, gained);
                return CompletableFuture.completedFuture(null);
            }

            int expPerLevel = BaseProfession.expPerLevel(playerUuid, identityUuid);
            IdentityProperties props = optProps.get();
            long exp = (long) props.getExp() + gained;
            int level = props.getLevel();
            int statPoints = props.getStatPoints();
            int levelsGained = 0;
            while (exp >= expPerLevel) {
                exp -= expPerLevel;
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
            return MCIdentityProvider.instance.updateProperties(identityUuid, newProps).thenRun(() -> {
                if (!leveledUp || plugin == null) return;
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null && player.isOnline()) {
                    String message = BaseProfession.isBaseIdentity(playerUuid, identityUuid)
                        ? ChatColor.GREEN + "Your base Player profession is now level " + finalLevel + "!"
                        : ChatColor.GREEN + "Your alternative Player identity is now level " + finalLevel + "!";
                    Schedulers.forEntity(player).run(plugin, st -> player.sendMessage(message), null);
                }
            });
        });
    }

    /**
     * Puts unflushed EXP back into the buffer it came from so the next flush
     * retries it.
     * @param playerUuid The owning player's UUID.
     * @param identityUuid The base identity the EXP belongs to.
     * @param gained The EXP amount to requeue.
     */
    private static void requeue(UUID playerUuid, UUID identityUuid, int gained) {
        if (BaseProfession.isBaseIdentity(playerUuid, identityUuid)) {
            PRIMARY_BUFFER.merge(playerUuid, gained, Integer::sum);
        } else {
            ALTERNATIVE_BUFFER.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
                .merge(identityUuid, gained, Integer::sum);
        }
    }

    private static void tick() {
        for (Player player : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            UUID playerUuid = player.getUniqueId();

            // The primary identity always accrues, whatever is equipped.
            PRIMARY_BUFFER.merge(playerUuid, 1, Integer::sum);

            // An alternative base identity accrues only while equipped. The
            // primary identity is excluded here so its second is never
            // counted twice.
            Optional<ActiveIdentitySession> session = MCIdentityManager.getSession(playerUuid);
            if (session.isPresent()
                    && BaseProfession.isBaseProfession(session.get().identityKey().getProfessionCode())
                    && !BaseProfession.isBaseIdentity(playerUuid, session.get().identityUuid())) {
                ALTERNATIVE_BUFFER.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
                    .merge(session.get().identityUuid(), 1, Integer::sum);
            }
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

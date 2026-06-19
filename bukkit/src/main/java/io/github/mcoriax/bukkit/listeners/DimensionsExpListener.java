package io.github.mcoriax.bukkit.listeners;

import io.github.mcoriax.bukkit.util.CooldownTracker;
import io.github.mcoriax.bukkit.util.ExpAction;
import io.github.mcoriax.bukkit.util.ExperienceManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles dimensions and aquatic related EXP actions:
 * {@code swim_distance}, {@code enter_portal} and {@code use_totem}.
 *
 * <p>Swim distance is accumulated per player and rewarded once for every completed
 * {@link #SWIM_SEGMENT_BLOCKS} blocks of swimming, so the reward scales with genuine
 * travel rather than firing on every move tick. Portal travel is rate-limited to stop
 * back-and-forth farming, and totem usage is naturally rare.</p>
 */
public class DimensionsExpListener implements Listener {

    /** Number of swum blocks that must accumulate before a swim reward is granted. */
    private static final double SWIM_SEGMENT_BLOCKS = 10.0D;

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;
    /**
     * Accumulated swim distance per player, in blocks, not yet converted into a reward.
     */
    private final Map<UUID, Double> swimProgress = new ConcurrentHashMap<>();
    /**
     * Cooldown for portal-travel rewards.
     */
    private final CooldownTracker portalCooldown = new CooldownTracker(5_000L);

    /**
     * Creates a new DimensionsExpListener.
     * @param plugin The plugin instance.
     */
    public DimensionsExpListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Accumulates swimming distance and rewards the player once per completed segment.
     * @param event The player move event.
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!player.isSwimming()) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        World world = from.getWorld();
        if (world == null || !world.equals(to.getWorld())) {
            return;
        }

        double moved = from.distance(to);
        if (moved <= 0.0D) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        UUID id = player.getUniqueId();
        double total = swimProgress.getOrDefault(id, 0.0D) + moved;
        // Drain whole segments so a long single move still rewards the right amount.
        while (total >= SWIM_SEGMENT_BLOCKS) {
            total -= SWIM_SEGMENT_BLOCKS;
            exp.awardExp(player, ExpAction.SWIM_DISTANCE);
        }
        swimProgress.put(id, total);
    }

    /**
     * Awards EXP when a player travels through a nether or end portal.
     * @param event The player portal event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        Location target = event.getTo();
        if (target == null || target.getWorld() == null) {
            return;
        }
        if (!portalCooldown.tryAcquire(player.getUniqueId())) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        // Keyed by the destination environment so configs can reward the nether and end differently.
        exp.awardExp(player, ExpAction.ENTER_PORTAL, target.getWorld().getEnvironment().name().toLowerCase());
    }

    /**
     * Awards EXP when a totem of undying saves a player from death.
     * @param event The entity resurrect event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityResurrect(EntityResurrectEvent event) {
        // ignoreCancelled = true means a totem was actually present and consumed.
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.USE_TOTEM);
    }

    /**
     * Clears tracked swim and cooldown state when a player leaves the server.
     * @param event The player quit event.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        swimProgress.remove(id);
        portalCooldown.clear(id);
    }
}

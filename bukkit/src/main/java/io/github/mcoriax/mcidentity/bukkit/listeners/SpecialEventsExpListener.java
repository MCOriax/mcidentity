package io.github.mcoriax.mcidentity.bukkit.listeners;

import io.github.mcoriax.mcidentity.bukkit.util.ExpAction;
import io.github.mcoriax.mcidentity.bukkit.util.ExperienceManager;
import io.github.mcoriax.mcidentity.bukkit.util.PlayerProximity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles special event and boss related EXP actions:
 * {@code trigger_raid}, {@code win_raid} and {@code summon_boss}.
 *
 * <p>Raid events carry the responsible player directly, so they are credited without a
 * heuristic. A summoned boss (the Wither) is built from blocks and is therefore not
 * attributed by the API, so the reward goes to the nearest player around the spawn,
 * matching how {@code create_golem} attributes constructed entities.</p>
 */
public class SpecialEventsExpListener implements Listener {

    /** Maximum distance from a boss spawn to look for the summoning player. */
    private static final double BOSS_ATTRIBUTION_RADIUS = 16.0D;

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;

    /**
     * Creates a new SpecialEventsExpListener.
     * @param plugin The plugin instance.
     */
    public SpecialEventsExpListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Awards EXP when a player triggers a village raid.
     * @param event The raid trigger event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRaidTrigger(RaidTriggerEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.TRIGGER_RAID);
    }

    /**
     * Awards EXP to every player credited with winning a village raid.
     * @param event The raid finish event.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRaidFinish(RaidFinishEvent event) {
        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        for (Player winner : event.getWinners()) {
            if (winner != null) {
                exp.awardExp(winner, ExpAction.WIN_RAID);
            }
        }
    }

    /**
     * Awards EXP to the nearest player when a boss is summoned.
     * @param event The creature spawn event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.BUILD_WITHER) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        LivingEntity boss = event.getEntity();
        Player summoner = PlayerProximity.findNearest(boss.getLocation(), BOSS_ATTRIBUTION_RADIUS);
        if (summoner == null) {
            return;
        }

        exp.awardExp(summoner, ExpAction.SUMMON_BOSS, boss.getType().name().toLowerCase());
    }
}

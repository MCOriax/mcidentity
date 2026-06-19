package io.github.mcoriax.bukkit.listeners;

import io.github.mcoriax.bukkit.util.CooldownTracker;
import io.github.mcoriax.bukkit.util.ExpAction;
import io.github.mcoriax.bukkit.util.ExperienceManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;

/**
 * Handles exploration and travel related EXP actions:
 * {@code discover_structure}, {@code ride_vehicle} and {@code leash_entity}.
 *
 * <p>Structure discovery is detected through the vanilla advancements that fire on
 * entering a major structure, which is reliable and self-deduplicating (an
 * advancement only completes once). Vehicle and leash rewards are rate-limited per
 * player to stop enter/exit and attach/detach spam.</p>
 */
public class ExplorationAndTravelExpListener implements Listener {

    /**
     * Advancement keys that vanilla grants upon discovering a major structure.
     */
    private static final Set<String> STRUCTURE_ADVANCEMENTS = Set.of(
            "minecraft:story/follow_ender_eye",  // Stronghold
            "minecraft:nether/find_fortress",    // Nether Fortress
            "minecraft:nether/find_bastion"      // Bastion Remnant
    );

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;
    /**
     * Cooldown for vehicle-ride rewards.
     */
    private final CooldownTracker rideCooldown = new CooldownTracker(10_000L);
    /**
     * Cooldown for leash rewards.
     */
    private final CooldownTracker leashCooldown = new CooldownTracker(3_000L);

    /**
     * Creates a new ExplorationAndTravelExpListener.
     * @param plugin The plugin instance.
     */
    public ExplorationAndTravelExpListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Awards EXP when a player completes a structure-discovery advancement.
     * @param event The advancement done event.
     */
    @EventHandler
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        String key = event.getAdvancement().getKey().toString();
        if (!STRUCTURE_ADVANCEMENTS.contains(key)) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(event.getPlayer(), ExpAction.DISCOVER_STRUCTURE);
    }

    /**
     * Awards EXP when a player starts riding a vehicle.
     * @param event The vehicle enter event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player player)) {
            return;
        }
        if (!rideCooldown.tryAcquire(player.getUniqueId())) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.RIDE_VEHICLE, event.getVehicle().getType().name().toLowerCase());
    }

    /**
     * Awards EXP when a player leashes an entity.
     * @param event The player leash entity event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeashEntity(PlayerLeashEntityEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        if (!leashCooldown.tryAcquire(player.getUniqueId())) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.LEASH_ENTITY, event.getEntity().getType().name().toLowerCase());
    }

    /**
     * Clears tracked cooldown state when a player leaves the server.
     * @param event The player quit event.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        rideCooldown.clear(id);
        leashCooldown.clear(id);
    }
}

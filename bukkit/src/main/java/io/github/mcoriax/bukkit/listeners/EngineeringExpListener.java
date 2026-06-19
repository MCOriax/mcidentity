package io.github.mcoriax.bukkit.listeners;

import com.cryptomorin.xseries.XMaterial;
import io.github.mcoriax.bukkit.util.BlockFlag;
import io.github.mcoriax.bukkit.util.ExpAction;
import io.github.mcoriax.bukkit.util.ExperienceManager;
import io.github.mcoriax.bukkit.util.PlayerProximity;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

/**
 * Handles engineering and mechanics related EXP actions:
 * {@code place_redstone}, {@code create_golem} and {@code activate_conduit}.
 *
 * <p>Block placements are flagged per location so the classic place / break / replace
 * EXP farm is blocked at a fixed spot. Utility golems are not attributed to a player
 * by the Bukkit API, so the reward is credited to the nearest player around the spawn,
 * which is the conventional heuristic for constructed entities.</p>
 */
public class EngineeringExpListener implements Listener {

    /** Metadata flag marking a redstone component that was already rewarded for placement. */
    private static final String REDSTONE_FLAG = "mcidentity_redstone_placed";
    /** Metadata flag marking a conduit that was already rewarded for activation. */
    private static final String CONDUIT_FLAG = "mcidentity_conduit_placed";
    /** Maximum distance from a golem spawn to look for the building player. */
    private static final double GOLEM_ATTRIBUTION_RADIUS = 10.0D;

    /**
     * Core redstone component materials. Buttons, pressure plates and powered rails are
     * matched separately by name so the set stays small and version-stable.
     */
    private static final Set<XMaterial> REDSTONE_COMPONENTS = Set.of(
            XMaterial.REDSTONE_WIRE,
            XMaterial.REPEATER,
            XMaterial.COMPARATOR,
            XMaterial.REDSTONE_TORCH,
            XMaterial.REDSTONE_WALL_TORCH,
            XMaterial.REDSTONE_BLOCK,
            XMaterial.REDSTONE_LAMP,
            XMaterial.PISTON,
            XMaterial.STICKY_PISTON,
            XMaterial.OBSERVER,
            XMaterial.DISPENSER,
            XMaterial.DROPPER,
            XMaterial.HOPPER,
            XMaterial.LEVER,
            XMaterial.DAYLIGHT_DETECTOR,
            XMaterial.TARGET,
            XMaterial.TRIPWIRE_HOOK,
            XMaterial.NOTE_BLOCK
    );

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;

    /**
     * Creates a new EngineeringExpListener.
     * @param plugin The plugin instance.
     */
    public EngineeringExpListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Awards EXP when a player places a redstone component or a conduit.
     * @param event The block place event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        Material type = block.getType();

        if (XMaterial.matchXMaterial(type) == XMaterial.CONDUIT) {
            // Anti-exploit: only the first placement at a given location is rewarded.
            if (BlockFlag.isSet(block, plugin, CONDUIT_FLAG)) {
                return;
            }
            BlockFlag.set(block, plugin, CONDUIT_FLAG);
            exp.awardExp(player, ExpAction.ACTIVATE_CONDUIT);
        } else if (isRedstoneComponent(type)) {
            // Anti-exploit: only the first placement at a given location is rewarded.
            if (BlockFlag.isSet(block, plugin, REDSTONE_FLAG)) {
                return;
            }
            BlockFlag.set(block, plugin, REDSTONE_FLAG);
            exp.awardExp(player, ExpAction.PLACE_REDSTONE, type.name().toLowerCase());
        }
    }

    /**
     * Awards EXP to the nearest player when a utility golem is built.
     * @param event The creature spawn event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason != CreatureSpawnEvent.SpawnReason.BUILD_IRONGOLEM
                && reason != CreatureSpawnEvent.SpawnReason.BUILD_SNOWMAN) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        LivingEntity golem = event.getEntity();
        Player builder = PlayerProximity.findNearest(golem.getLocation(), GOLEM_ATTRIBUTION_RADIUS);
        if (builder == null) {
            return;
        }

        exp.awardExp(builder, ExpAction.CREATE_GOLEM, golem.getType().name().toLowerCase());
    }

    /**
     * Determines whether a material is a placeable redstone component.
     * @param type The material.
     * @return true if the material is part of a redstone contraption.
     */
    private boolean isRedstoneComponent(Material type) {
        if (REDSTONE_COMPONENTS.contains(XMaterial.matchXMaterial(type))) {
            return true;
        }
        String name = type.name();
        return name.endsWith("_BUTTON")
                || name.endsWith("_PRESSURE_PLATE")
                || name.endsWith("_RAIL");
    }
}

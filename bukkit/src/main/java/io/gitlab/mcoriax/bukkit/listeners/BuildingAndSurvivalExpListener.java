package io.gitlab.mcoriax.bukkit.listeners;

import io.gitlab.mcoriax.bukkit.util.ExpAction;
import io.gitlab.mcoriax.bukkit.util.ExperienceManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles building and survival related EXP actions:
 * {@code place_block}, {@code consume_food} and {@code shear_entity}.
 *
 * <p>The {@code place_block} handler marks every player-placed block with a
 * metadata flag. A location that already carries the flag is never rewarded
 * again, which prevents the classic place / break / replace EXP farm at a fixed
 * spot while still rewarding genuine building.</p>
 */
public class BuildingAndSurvivalExpListener implements Listener {

    /** Metadata key used to flag blocks that were already rewarded once. */
    private static final String PLACED_METADATA_KEY = "mcidentity_placed";

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;

    /**
     * Creates a new BuildingAndSurvivalExpListener.
     * @param plugin The plugin instance.
     */
    public BuildingAndSurvivalExpListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Awards EXP when a player places a block for the first time at a given location.
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

        // Anti-exploit: skip blocks placed where we have already rewarded a placement.
        if (hasPlacedFlag(block)) {
            return;
        }
        block.setMetadata(PLACED_METADATA_KEY, new FixedMetadataValue(plugin, Boolean.TRUE));

        exp.awardExp(player, ExpAction.PLACE_BLOCK, block.getType().name().toLowerCase());
    }

    /**
     * Awards EXP when a player consumes a food item.
     * @param event The player item consume event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !item.getType().isEdible()) {
            // Ignore potions, milk buckets and other non-food consumables.
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(event.getPlayer(), ExpAction.CONSUME_FOOD, item.getType().name().toLowerCase());
    }

    /**
     * Awards EXP when a player shears an entity.
     * @param event The player shear entity event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShearEntity(PlayerShearEntityEvent event) {
        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(event.getPlayer(), ExpAction.SHEAR_ENTITY, event.getEntity().getType().name().toLowerCase());
    }

    /**
     * Checks whether a block already carries our "rewarded placement" flag.
     * @param block The block.
     * @return true if the block was already rewarded for a placement.
     */
    private boolean hasPlacedFlag(Block block) {
        if (!block.hasMetadata(PLACED_METADATA_KEY)) {
            return false;
        }
        for (MetadataValue value : block.getMetadata(PLACED_METADATA_KEY)) {
            // Only trust the flag written by this plugin instance.
            if (value.getOwningPlugin() == plugin && value.asBoolean()) {
                return true;
            }
        }
        return false;
    }
}

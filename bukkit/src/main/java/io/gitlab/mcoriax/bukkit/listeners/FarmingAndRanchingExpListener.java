package io.gitlab.mcoriax.bukkit.listeners;

import io.gitlab.mcoriax.bukkit.util.BlockFlag;
import io.gitlab.mcoriax.bukkit.util.ExpAction;
import io.gitlab.mcoriax.bukkit.util.ExperienceManager;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Goat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;

/**
 * Handles farming and ranching related EXP actions:
 * {@code plant_crop}, {@code strip_log}, {@code milk_entity} and {@code catch_entity_bucket}.
 *
 * <p>Listeners only detect the action and delegate to {@link ExperienceManager};
 * the active profession's YAML decides whether any EXP is awarded.</p>
 */
public class FarmingAndRanchingExpListener implements Listener {

    /** Metadata flag marking a log that was already rewarded for stripping. */
    private static final String STRIPPED_FLAG = "mcidentity_stripped";

    /** Block materials considered plantable crops. */
    private static final Set<Material> CROPS = Set.of(
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS,
            Material.NETHER_WART,
            Material.MELON_STEM,
            Material.PUMPKIN_STEM,
            Material.COCOA,
            Material.SWEET_BERRY_BUSH,
            Material.TORCHFLOWER_CROP,
            Material.PITCHER_CROP
    );

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;

    /**
     * Creates a new FarmingAndRanchingExpListener.
     * @param plugin The plugin instance.
     */
    public FarmingAndRanchingExpListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Awards EXP when a player plants a crop seed.
     * @param event The block place event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlantCrop(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (!CROPS.contains(block.getType())) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        // Keyed by the crop material so configs may reward specific crops.
        exp.awardExp(event.getPlayer(), ExpAction.PLANT_CROP, block.getType().name().toLowerCase());
    }

    /**
     * Awards EXP when a player strips a log with an axe.
     * @param event The player interact event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStripLog(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();
        if (block == null || item == null) {
            return;
        }

        Material logType = block.getType();
        // Must be an axe used on a non-stripped log.
        if (!item.getType().name().endsWith("_AXE")
                || !Tag.LOGS.isTagged(logType)
                || logType.name().startsWith("STRIPPED_")) {
            return;
        }

        // Anti-exploit: never reward stripping at a location we already rewarded.
        if (BlockFlag.isSet(block, plugin, STRIPPED_FLAG)) {
            return;
        }
        BlockFlag.set(block, plugin, STRIPPED_FLAG);

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(event.getPlayer(), ExpAction.STRIP_LOG, logType.name().toLowerCase());
    }

    /**
     * Awards EXP when a player milks an entity with a bucket.
     * @param event The player interact entity event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMilkEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Entity target = event.getRightClicked();
        // Cow covers mushroom cows; goats are milkable too.
        if (!(target instanceof Cow) && !(target instanceof Goat)) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.BUCKET) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.MILK_ENTITY, target.getType().name().toLowerCase());
    }

    /**
     * Awards EXP when a player captures an entity inside a bucket.
     * @param event The player bucket entity event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketEntity(PlayerBucketEntityEvent event) {
        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(event.getPlayer(), ExpAction.CATCH_ENTITY_BUCKET,
                event.getEntity().getType().name().toLowerCase());
    }
}

package io.gitlab.mcoriax.bukkit.listeners;

import io.gitlab.mcoriax.bukkit.util.ExpAction;
import io.gitlab.mcoriax.bukkit.util.ExperienceManager;
import io.gitlab.mcoriax.bukkit.util.PlayerProximity;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Sniffer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles ranching and wildlife related EXP actions:
 * {@code shear_sheep_color}, {@code shear_mushroom_cow}, {@code spawn_axolotl_bucket}
 * and {@code sniff_sniffer_egg}.
 *
 * <p>Each handler only detects the action and forwards it to the
 * {@link ExperienceManager}; the active profession's YAML decides whether any EXP is
 * awarded. Shearing is naturally self-limiting (wool regrows over time and a mooshroom
 * reverts to a cow), bucket release places a single entity, and the sniffer dig is
 * credited to the nearest player because the Bukkit API does not attribute it.</p>
 */
public class RanchingAndWildlifeExpListener implements Listener {

    /** Maximum distance from a freshly dug seed to confirm the sniffer that produced it. */
    private static final double SNIFFER_PROXIMITY = 3.0D;
    /** Maximum distance from a sniffer dig to look for the player to credit. */
    private static final double SNIFFER_ATTRIBUTION_RADIUS = 12.0D;

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;

    /**
     * Creates a new RanchingAndWildlifeExpListener.
     * @param plugin The plugin instance.
     */
    public RanchingAndWildlifeExpListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Awards EXP when a player shears a sheep or a mooshroom.
     * @param event The player shear entity event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShearEntity(PlayerShearEntityEvent event) {
        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        Player player = event.getPlayer();
        Entity entity = event.getEntity();

        if (entity instanceof MushroomCow) {
            // A mooshroom can only be sheared once before it reverts to a cow, so this is unfarmable.
            exp.awardExp(player, ExpAction.SHEAR_MUSHROOM_COW);
        } else if (entity instanceof Sheep sheep) {
            DyeColor color = sheep.getColor();
            if (color == null) {
                return;
            }
            // Shearing only fires while the sheep still has wool, which gates re-shearing until it regrows.
            exp.awardExp(player, ExpAction.SHEAR_SHEEP_COLOR, color.name().toLowerCase());
        }
    }

    /**
     * Awards EXP when a player releases an axolotl from a bucket.
     * @param event The player bucket empty event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAxolotlBucketEmpty(PlayerBucketEmptyEvent event) {
        if (event.getBucket() != Material.AXOLOTL_BUCKET) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(event.getPlayer(), ExpAction.SPAWN_AXOLOTL_BUCKET);
    }

    /**
     * Awards EXP to the nearest player when a sniffer digs up an ancient seed.
     * @param event The item spawn event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item drop = event.getEntity();
        Material type = drop.getItemStack().getType();
        // Sniffers only ever unearth the ancient torchflower seed or the pitcher pod.
        if (type != Material.TORCHFLOWER_SEEDS && type != Material.PITCHER_POD) {
            return;
        }
        // Confirm a sniffer is right next to the new seed so ordinary item drops are ignored.
        if (!hasSnifferNearby(drop)) {
            return;
        }

        Player farmer = PlayerProximity.findNearest(drop.getLocation(), SNIFFER_ATTRIBUTION_RADIUS);
        if (farmer == null) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(farmer, ExpAction.SNIFF_SNIFFER_EGG, type.name().toLowerCase());
    }

    /**
     * Determines whether a sniffer is standing next to a freshly spawned seed item.
     * @param drop The spawned seed item.
     * @return true if at least one sniffer is within {@link #SNIFFER_PROXIMITY} blocks.
     */
    private boolean hasSnifferNearby(Item drop) {
        for (Entity nearby : drop.getNearbyEntities(SNIFFER_PROXIMITY, SNIFFER_PROXIMITY, SNIFFER_PROXIMITY)) {
            if (nearby instanceof Sniffer) {
                return true;
            }
        }
        return false;
    }
}

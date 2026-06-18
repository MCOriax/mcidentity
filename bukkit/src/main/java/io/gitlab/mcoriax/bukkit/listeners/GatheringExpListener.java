package io.gitlab.mcoriax.bukkit.listeners;

import io.gitlab.mcoriax.bukkit.util.ExpAction;
import io.gitlab.mcoriax.bukkit.util.ExperienceManager;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles gathering related EXP actions:
 * {@code break_ore}, {@code chop_tree}, {@code harvest_crop} and {@code fish_caught}.
 */
public class GatheringExpListener implements Listener {

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;

    /**
     * Creates a new GatheringExpListener.
     * @param plugin The plugin instance.
     */
    public GatheringExpListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Awards EXP for breaking ores, chopping logs and harvesting mature crops.
     * @param event The block break event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material type = block.getType();
        String typeName = type.name().toLowerCase();

        if (isOre(type)) {
            exp.awardExp(player, ExpAction.BREAK_ORE, typeName);
        } else if (Tag.LOGS.isTagged(type)) {
            exp.awardExp(player, ExpAction.CHOP_TREE, typeName);
        } else if (isMatureCrop(block)) {
            exp.awardExp(player, ExpAction.HARVEST_CROP, typeName);
        }
    }

    /**
     * Awards EXP when a player successfully reels in a fish.
     * @param event The player fish event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(event.getPlayer(), ExpAction.FISH_CAUGHT);
    }

    /**
     * Determines whether a material is an ore block.
     * @param type The material.
     * @return true if the material is an ore (or ancient debris).
     */
    private boolean isOre(Material type) {
        return type.name().endsWith("_ORE") || type == Material.ANCIENT_DEBRIS;
    }

    /**
     * Determines whether a block is a fully grown crop.
     * @param block The block.
     * @return true if the block is a crop at its maximum age.
     */
    private boolean isMatureCrop(Block block) {
        if (block.getBlockData() instanceof Ageable ageable) {
            return ageable.getAge() == ageable.getMaximumAge();
        }
        return false;
    }
}

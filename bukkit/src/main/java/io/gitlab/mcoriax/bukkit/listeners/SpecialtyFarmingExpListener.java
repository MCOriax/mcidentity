package io.gitlab.mcoriax.bukkit.listeners;

import io.gitlab.mcoriax.bukkit.util.CooldownTracker;
import io.gitlab.mcoriax.bukkit.util.ExpAction;
import io.gitlab.mcoriax.bukkit.util.ExperienceManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Beehive;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles specialty farming and pet related EXP actions:
 * {@code feed_pet}, {@code hatch_egg} and {@code collect_honey}.
 *
 * <p>Feeding only counts for a player's own tamed pet held with an actual food item,
 * and is rate-limited to defeat click spamming. Egg hatching is detected through the
 * vanilla hatch roll, and honey is only rewarded from a full hive, which the bees take
 * real time to refill.</p>
 */
public class SpecialtyFarmingExpListener implements Listener {

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;
    /**
     * Cooldown for pet-feeding rewards.
     */
    private final CooldownTracker feedCooldown = new CooldownTracker(1_500L);

    /**
     * Creates a new SpecialtyFarmingExpListener.
     * @param plugin The plugin instance.
     */
    public SpecialtyFarmingExpListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Awards EXP when a player feeds one of their own tamed pets.
     * @param event The player interact entity event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFeedPet(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Entity target = event.getRightClicked();
        if (!(target instanceof Tameable tameable) || !tameable.isTamed()) {
            return;
        }

        Player player = event.getPlayer();
        AnimalTamer owner = tameable.getOwner();
        // Only the pet's own owner may earn the feeding reward.
        if (owner == null || !owner.getUniqueId().equals(player.getUniqueId())) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isPetFood(item)) {
            return;
        }
        if (!feedCooldown.tryAcquire(player.getUniqueId())) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.FEED_PET, target.getType().name().toLowerCase());
    }

    /**
     * Awards EXP when a thrown egg hatches into a chick.
     * @param event The player egg throw event.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEggThrow(PlayerEggThrowEvent event) {
        // Only reward when the vanilla roll actually spawns at least one chick.
        if (!event.isHatching()) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(event.getPlayer(), ExpAction.HATCH_EGG);
    }

    /**
     * Awards EXP when a player bottles honey from a full beehive or bee nest.
     * @param event The player interact event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCollectHoney(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();
        if (block == null || item == null || item.getType() != Material.GLASS_BOTTLE) {
            return;
        }

        BlockData data = block.getBlockData();
        if (!(data instanceof Beehive hive)) {
            return;
        }
        // Only a full hive actually yields honey, which prevents instant re-harvesting.
        if (hive.getHoneyLevel() < hive.getMaximumHoneyLevel()) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(event.getPlayer(), ExpAction.COLLECT_HONEY);
    }

    /**
     * Clears tracked cooldown state when a player leaves the server.
     * @param event The player quit event.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        feedCooldown.clear(event.getPlayer().getUniqueId());
    }

    /**
     * Determines whether an item can plausibly be used to feed a pet.
     * @param item The item held by the player.
     * @return true if the item is edible food or a seed used to feed tamed birds.
     */
    private boolean isPetFood(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        // Meats and most pet foods are edible; seeds feed tamed parrots but are not edible.
        return item.getType().isEdible() || item.getType().name().endsWith("_SEEDS");
    }
}

package io.gitlab.mcoriax.bukkit.listeners;

import io.gitlab.mcoriax.bukkit.util.BlockFlag;
import io.gitlab.mcoriax.bukkit.util.CooldownTracker;
import io.gitlab.mcoriax.bukkit.util.ExpAction;
import io.gitlab.mcoriax.bukkit.util.ExperienceManager;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.block.data.type.EndPortalFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;

/**
 * Handles world interaction related EXP actions:
 * {@code ignite_campfire}, {@code harvest_sweet_berries}, {@code brush_archaeology},
 * {@code use_ender_eye_portal}, {@code plant_flower} and {@code clean_armor_trim}.
 *
 * <p>Spam-prone actions (lighting a campfire, washing armor) are rate-limited per
 * player, while one-time-per-location actions (brushing a suspicious block, filling an
 * end portal frame, planting a flower) are guarded with a {@link BlockFlag} so the
 * place / break / replace farm cannot be exploited. Sweet berries are gated by the
 * vanilla bush growth stage and need no extra guard.</p>
 */
public class WorldInteractionExpListener implements Listener {

    /** Metadata flag marking a suspicious block that was already rewarded for brushing. */
    private static final String BRUSHED_FLAG = "mcidentity_brushed";
    /** Metadata flag marking an end portal frame that was already rewarded for an eye. */
    private static final String ENDER_EYE_FLAG = "mcidentity_ender_eye";
    /** Metadata flag marking a flower planted in the ground that was already rewarded. */
    private static final String FLOWER_PLANTED_FLAG = "mcidentity_flower_planted";
    /** Metadata flag marking a flower pot that was already rewarded for planting. */
    private static final String FLOWER_POTTED_FLAG = "mcidentity_flower_potted";

    /** Sweet berry bush growth stage that first bears collectable berries. */
    private static final int SWEET_BERRY_RIPE_AGE = 2;

    /**
     * Two-block tall flowers. The Bukkit API does not expose a {@code TALL_FLOWERS} tag,
     * so they are listed explicitly and combined with {@link Tag#SMALL_FLOWERS}.
     */
    private static final Set<Material> TALL_FLOWERS = Set.of(
            Material.SUNFLOWER,
            Material.LILAC,
            Material.ROSE_BUSH,
            Material.PEONY,
            Material.PITCHER_PLANT
    );

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;
    /**
     * Cooldown for campfire-lighting rewards.
     */
    private final CooldownTracker campfireCooldown = new CooldownTracker(3_000L);
    /**
     * Cooldown for armor-wash rewards.
     */
    private final CooldownTracker washCooldown = new CooldownTracker(3_000L);

    /**
     * Creates a new WorldInteractionExpListener.
     * @param plugin The plugin instance.
     */
    public WorldInteractionExpListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Routes block right-clicks to the campfire, berry, brush, ender eye and flower pot handlers.
     * @param event The player interact event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Material blockType = block.getType();

        if (blockType == Material.CAMPFIRE || blockType == Material.SOUL_CAMPFIRE) {
            handleIgniteCampfire(player, block, item);
        } else if (blockType == Material.SWEET_BERRY_BUSH) {
            handleSweetBerries(player, block);
        } else if (blockType == Material.SUSPICIOUS_SAND || blockType == Material.SUSPICIOUS_GRAVEL) {
            handleBrush(player, block, item);
        } else if (blockType == Material.END_PORTAL_FRAME) {
            handleEnderEye(player, block, item);
        } else if (blockType == Material.FLOWER_POT) {
            handlePotFlower(player, block, item);
        }
    }

    /**
     * Awards EXP when a player plants a flower on the ground.
     * @param event The block place event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFlowerPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (!isFlower(block.getType())) {
            return;
        }
        // Anti-exploit: only the first planting at a given location is rewarded.
        if (BlockFlag.isSet(block, plugin, FLOWER_PLANTED_FLAG)) {
            return;
        }
        BlockFlag.set(block, plugin, FLOWER_PLANTED_FLAG);

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(event.getPlayer(), ExpAction.PLANT_FLOWER, block.getType().name().toLowerCase());
    }

    /**
     * Awards EXP when a player washes a trimmed armor piece in a cauldron.
     * @param event The cauldron level change event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCauldronLevelChange(CauldronLevelChangeEvent event) {
        if (event.getReason() != CauldronLevelChangeEvent.ChangeReason.ARMOR_WASH) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack armor = player.getInventory().getItemInMainHand();
        if (!(armor.getItemMeta() instanceof ArmorMeta armorMeta) || !armorMeta.hasTrim()) {
            return;
        }
        // Anti-exploit: rate-limit so repeated dips into the same cauldron cannot be farmed.
        if (!washCooldown.tryAcquire(player.getUniqueId())) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        ArmorTrim trim = armorMeta.getTrim();
        exp.awardExp(player, ExpAction.CLEAN_ARMOR_TRIM, trim.getPattern().getKey().getKey().toLowerCase());
    }

    /**
     * Awards EXP for lighting an unlit campfire with flint and steel or a fire charge.
     * @param player The interacting player.
     * @param block The campfire block.
     * @param item The item used.
     */
    private void handleIgniteCampfire(Player player, Block block, ItemStack item) {
        if (item == null) {
            return;
        }
        Material tool = item.getType();
        if (tool != Material.FLINT_AND_STEEL && tool != Material.FIRE_CHARGE) {
            return;
        }
        // A campfire that is already lit ignores the interaction, so it is not a real lighting.
        if (block.getBlockData() instanceof Campfire campfire && campfire.isLit()) {
            return;
        }
        // Anti-exploit: rate-limit to defeat the light / extinguish farm.
        if (!campfireCooldown.tryAcquire(player.getUniqueId())) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.IGNITE_CAMPFIRE, block.getType().name().toLowerCase());
    }

    /**
     * Awards EXP for gathering berries from a ripe sweet berry bush.
     * @param player The interacting player.
     * @param block The sweet berry bush block.
     */
    private void handleSweetBerries(Player player, Block block) {
        // Berries are only collected once the bush has grown them; harvesting resets the age.
        if (!(block.getBlockData() instanceof Ageable ageable) || ageable.getAge() < SWEET_BERRY_RIPE_AGE) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.HARVEST_SWEET_BERRIES);
    }

    /**
     * Awards EXP for brushing a suspicious block to uncover its loot.
     * @param player The interacting player.
     * @param block The suspicious block.
     * @param item The item used.
     */
    private void handleBrush(Player player, Block block, ItemStack item) {
        if (item == null || item.getType() != Material.BRUSH) {
            return;
        }
        // Anti-exploit: a suspicious block holds a single loot and is rewarded only once.
        if (BlockFlag.isSet(block, plugin, BRUSHED_FLAG)) {
            return;
        }
        BlockFlag.set(block, plugin, BRUSHED_FLAG);

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.BRUSH_ARCHAEOLOGY, block.getType().name().toLowerCase());
    }

    /**
     * Awards EXP for placing an eye of ender into an empty end portal frame slot.
     * @param player The interacting player.
     * @param block The end portal frame block.
     * @param item The item used.
     */
    private void handleEnderEye(Player player, Block block, ItemStack item) {
        if (item == null || item.getType() != Material.ENDER_EYE) {
            return;
        }
        // A frame that already holds an eye cannot accept another, so it is unfarmable.
        if (block.getBlockData() instanceof EndPortalFrame frame && frame.hasEye()) {
            return;
        }
        if (BlockFlag.isSet(block, plugin, ENDER_EYE_FLAG)) {
            return;
        }
        BlockFlag.set(block, plugin, ENDER_EYE_FLAG);

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.USE_ENDER_EYE_PORTAL);
    }

    /**
     * Awards EXP for planting a flower into an empty flower pot.
     * @param player The interacting player.
     * @param block The flower pot block.
     * @param item The item used.
     */
    private void handlePotFlower(Player player, Block block, ItemStack item) {
        // Only small flowers can actually be potted, which keeps the reward honest.
        if (item == null || !isPottableFlower(item.getType())) {
            return;
        }
        // Anti-exploit: only the first planting at a given pot is rewarded.
        if (BlockFlag.isSet(block, plugin, FLOWER_POTTED_FLAG)) {
            return;
        }
        BlockFlag.set(block, plugin, FLOWER_POTTED_FLAG);

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.PLANT_FLOWER, item.getType().name().toLowerCase());
    }

    /**
     * Clears tracked cooldown state when a player leaves the server.
     * @param event The player quit event.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        campfireCooldown.clear(id);
        washCooldown.clear(id);
    }

    /**
     * Determines whether a material is a placeable flower (small or tall).
     * @param type The material.
     * @return true if the material is a flower.
     */
    private boolean isFlower(Material type) {
        return Tag.SMALL_FLOWERS.isTagged(type) || TALL_FLOWERS.contains(type);
    }

    /**
     * Determines whether a material is a flower that can be placed inside a flower pot.
     * @param type The material.
     * @return true if the material is a pottable small flower.
     */
    private boolean isPottableFlower(Material type) {
        return Tag.SMALL_FLOWERS.isTagged(type);
    }
}

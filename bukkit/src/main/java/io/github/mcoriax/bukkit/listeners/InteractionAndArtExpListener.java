package io.github.mcoriax.bukkit.listeners;

import com.cryptomorin.xseries.XMaterial;
import io.github.mcoriax.bukkit.util.BlockFlag;
import io.github.mcoriax.bukkit.util.CooldownTracker;
import io.github.mcoriax.bukkit.util.ExpAction;
import io.github.mcoriax.bukkit.util.ExperienceManager;
import io.github.mcoriax.bukkit.util.ServerVersion;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Handles interaction and art related EXP actions:
 * {@code play_music_disc}, {@code dye_item}, {@code carve_pumpkin} and {@code ring_bell}.
 *
 * <p>Jukebox rewards only fire when inserting a disc into an empty jukebox, pumpkin
 * carving is flagged per block to block the place/carve/break/replace farm, and bell
 * ringing and sheep dyeing are rate-limited per player.</p>
 */
public class InteractionAndArtExpListener implements Listener {

    /** Metadata flag marking a pumpkin that was already rewarded for carving. */
    private static final String CARVED_FLAG = "mcidentity_carved";

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;
    /**
     * Cooldown for bell-ring rewards.
     */
    private final CooldownTracker bellCooldown = new CooldownTracker(5_000L);
    /**
     * Cooldown for sheep-dye rewards.
     */
    private final CooldownTracker dyeCooldown = new CooldownTracker(3_000L);

    /**
     * Creates a new InteractionAndArtExpListener.
     * @param plugin The plugin instance.
     */
    public InteractionAndArtExpListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Routes block right-clicks to the jukebox, pumpkin and bell handlers.
     * @param event The player interact event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (ServerVersion.hasOffhand() && event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        XMaterial blockType = XMaterial.matchXMaterial(block.getType());

        if (blockType == XMaterial.JUKEBOX) {
            handleJukebox(player, block, item);
        } else if (blockType == XMaterial.PUMPKIN) {
            handleCarvePumpkin(player, block, item);
        } else if (blockType == XMaterial.BELL) {
            handleRingBell(player);
        }
    }

    /**
     * Awards EXP when a player dyes a sheep.
     * @param event The player interact entity event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDyeEntity(PlayerInteractEntityEvent event) {
        if (ServerVersion.hasOffhand() && event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!(event.getRightClicked() instanceof Sheep)) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!item.getType().name().endsWith("_DYE")) {
            return;
        }
        if (!dyeCooldown.tryAcquire(player.getUniqueId())) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.DYE_ITEM, item.getType().name().toLowerCase());
    }

    /**
     * Awards EXP for inserting a music disc into an empty jukebox.
     * @param player The interacting player.
     * @param block The jukebox block.
     * @param item The item used.
     */
    private void handleJukebox(Player player, Block block, ItemStack item) {
        if (item == null || !item.getType().name().startsWith("MUSIC_DISC_")) {
            return;
        }
        // A jukebox that already holds a disc would eject it instead of playing.
        if (block.getState() instanceof Jukebox jukebox) {
            ItemStack record = jukebox.getRecord();
            if (record != null && record.getType() != Material.AIR) {
                return;
            }
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.PLAY_MUSIC_DISC);
    }

    /**
     * Awards EXP for carving an uncarved pumpkin with shears.
     * @param player The interacting player.
     * @param block The pumpkin block.
     * @param item The item used.
     */
    private void handleCarvePumpkin(Player player, Block block, ItemStack item) {
        if (item == null || XMaterial.matchXMaterial(item.getType()) != XMaterial.SHEARS) {
            return;
        }
        if (BlockFlag.isSet(block, plugin, CARVED_FLAG)) {
            return;
        }
        BlockFlag.set(block, plugin, CARVED_FLAG);

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.CARVE_PUMPKIN);
    }

    /**
     * Awards EXP for ringing a bell, respecting a per-player cooldown.
     * @param player The interacting player.
     */
    private void handleRingBell(Player player) {
        if (!bellCooldown.tryAcquire(player.getUniqueId())) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.RING_BELL);
    }

    /**
     * Clears tracked cooldown state when a player leaves the server.
     * @param event The player quit event.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        bellCooldown.clear(id);
        dyeCooldown.clear(id);
    }
}

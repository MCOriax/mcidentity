package io.github.mcoriax.mcidentity.bukkit.listeners;

import io.github.mcoriax.mcidentity.bukkit.util.ExpAction;
import io.github.mcoriax.mcidentity.bukkit.util.ExperienceManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles advanced exploration and movement related EXP actions:
 * {@code open_loot_chest}, {@code glide_elytra} and {@code survive_fall}.
 *
 * <p>{@code open_loot_chest} relies on {@link LootGenerateEvent}, which a vanilla
 * loot table only fires once per container, so the reward cannot be farmed. The
 * {@code glide_elytra} reward is rate-limited per player to stop rapid glide
 * toggling from inflating EXP gains.</p>
 */
public class AdvancedExplorationExpListener implements Listener {

    /** Minimum delay between two consecutive glide rewards for the same player. */
    private static final long GLIDE_REWARD_COOLDOWN_MILLIS = 5_000L;

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;
    /**
     * Tracks the last glide-reward timestamp per player to rate-limit gains.
     */
    private final Map<UUID, Long> lastGlideReward = new ConcurrentHashMap<>();

    /**
     * Creates a new AdvancedExplorationExpListener.
     * @param plugin The plugin instance.
     */
    public AdvancedExplorationExpListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Awards EXP when a player triggers loot generation in a natural container.
     * @param event The loot generate event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLootGenerate(LootGenerateEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.OPEN_LOOT_CHEST);
    }

    /**
     * Awards EXP when a player starts gliding with an elytra, respecting a cooldown.
     * @param event The entity toggle glide event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onToggleGlide(EntityToggleGlideEvent event) {
        // Only the start of a glide is rewarded.
        if (!event.isGliding() || !(event.getEntity() instanceof Player player)) {
            return;
        }

        // Make sure the glide is powered by an elytra, not a riptide trident burst.
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || chestplate.getType() != Material.ELYTRA) {
            return;
        }

        long now = System.currentTimeMillis();
        Long last = lastGlideReward.get(player.getUniqueId());
        if (last != null && now - last < GLIDE_REWARD_COOLDOWN_MILLIS) {
            return;
        }
        lastGlideReward.put(player.getUniqueId(), now);

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.GLIDE_ELYTRA);
    }

    /**
     * Awards EXP when a player takes fall damage but survives it.
     * @param event The entity damage event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL
                || !(event.getEntity() instanceof Player player)) {
            return;
        }

        // The damage is not applied yet at MONITOR priority, so compare against current health.
        double finalDamage = event.getFinalDamage();
        if (player.getHealth() - finalDamage <= 0.0D) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        // Scales with the survived fall damage via the numerical config overload.
        exp.awardExp(player, ExpAction.SURVIVE_FALL, finalDamage);
    }

    /**
     * Clears tracked glide state when a player leaves the server.
     * @param event The player quit event.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastGlideReward.remove(event.getPlayer().getUniqueId());
    }
}

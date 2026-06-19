package io.github.mcoriax.bukkit.listeners;

import com.cryptomorin.xseries.XMaterial;
import io.github.mcoriax.bukkit.util.CooldownTracker;
import io.github.mcoriax.bukkit.util.ExpAction;
import io.github.mcoriax.bukkit.util.ExperienceManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Handles survival and resilience related EXP actions:
 * {@code sleep_in_bed}, {@code extinguish_fire} and {@code survive_explosion}.
 *
 * <p>Sleeping and fire extinguishing are rate-limited per player so that climbing in
 * and out of a bed or igniting and putting out fire cannot be farmed. Surviving an
 * explosion mirrors the existing {@code survive_fall} action: it only rewards when the
 * blast is non-lethal and scales with the survived damage.</p>
 */
public class SurvivalExpListener implements Listener {

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;
    /**
     * Cooldown for bed-sleep rewards.
     */
    private final CooldownTracker sleepCooldown = new CooldownTracker(30_000L);
    /**
     * Cooldown for fire-extinguish rewards.
     */
    private final CooldownTracker extinguishCooldown = new CooldownTracker(3_000L);

    /**
     * Creates a new SurvivalExpListener.
     * @param plugin The plugin instance.
     */
    public SurvivalExpListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Awards EXP when a player successfully lies down to sleep.
     * @param event The player bed enter event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        // Only a successful entry counts as actually going to sleep.
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return;
        }

        Player player = event.getPlayer();
        if (!sleepCooldown.tryAcquire(player.getUniqueId())) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.SLEEP_IN_BED);
    }

    /**
     * Awards EXP when a player extinguishes a fire block.
     * @param event The block break event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExtinguishFire(BlockBreakEvent event) {
        Material type = event.getBlock().getType();
        XMaterial xType = XMaterial.matchXMaterial(type);
        if (xType != XMaterial.FIRE && xType != XMaterial.SOUL_FIRE) {
            return;
        }

        Player player = event.getPlayer();
        // Anti-exploit: rate-limit to defeat the ignite / extinguish farm.
        if (!extinguishCooldown.tryAcquire(player.getUniqueId())) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.EXTINGUISH_FIRE, type.name().toLowerCase());
    }

    /**
     * Awards EXP when a player takes explosion damage but survives it.
     * @param event The entity damage event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSurviveExplosion(EntityDamageEvent event) {
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                && cause != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
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

        // Scales with the survived explosion damage via the numerical config overload.
        exp.awardExp(player, ExpAction.SURVIVE_EXPLOSION, finalDamage);
    }

    /**
     * Clears tracked cooldown state when a player leaves the server.
     * @param event The player quit event.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        sleepCooldown.clear(id);
        extinguishCooldown.clear(id);
    }
}

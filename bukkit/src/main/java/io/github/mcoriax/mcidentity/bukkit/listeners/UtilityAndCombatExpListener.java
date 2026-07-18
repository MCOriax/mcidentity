package io.github.mcoriax.mcidentity.bukkit.listeners;

import io.github.mcoriax.mcidentity.bukkit.util.CooldownTracker;
import io.github.mcoriax.mcidentity.bukkit.util.ExpAction;
import io.github.mcoriax.mcidentity.bukkit.util.ExperienceManager;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.util.Set;
import java.util.UUID;

/**
 * Handles magic, utility and combat related EXP actions:
 * {@code ignite_fire}, {@code consume_potion}, {@code apply_debuff} and {@code cure_zombie_villager}.
 *
 * <p>Effects are matched by their namespaced key so the harmful-effect set stays
 * stable across Minecraft version renames. Fire ignition is rate-limited to stop
 * flint-and-steel spam, and curing requires a weakened zombie villager that is not
 * already converting, which is naturally self-limiting.</p>
 */
public class UtilityAndCombatExpListener implements Listener {

    /**
     * Harmful potion effect keys treated as debuffs.
     */
    private static final Set<String> HARMFUL_EFFECTS = Set.of(
            "slowness",
            "mining_fatigue",
            "instant_damage",
            "nausea",
            "blindness",
            "hunger",
            "weakness",
            "poison",
            "wither",
            "levitation",
            "unluck",
            "darkness"
    );

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;
    /**
     * Cooldown for fire-ignition rewards.
     */
    private final CooldownTracker igniteCooldown = new CooldownTracker(3_000L);

    /**
     * Creates a new UtilityAndCombatExpListener.
     * @param plugin The plugin instance.
     */
    public UtilityAndCombatExpListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Awards EXP when a player ignites a fire with flint and steel.
     * @param event The block ignite event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        Player player = event.getPlayer();
        if (player == null || event.getCause() != BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL) {
            return;
        }
        if (!igniteCooldown.tryAcquire(player.getUniqueId())) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.IGNITE_FIRE);
    }

    /**
     * Awards EXP when a player drinks a potion.
     * @param event The player item consume event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsumePotion(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.POTION) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(event.getPlayer(), ExpAction.CONSUME_POTION);
    }

    /**
     * Awards EXP when a player lands a harmful potion on another entity.
     * @param event The potion splash event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();
        if (!(potion.getShooter() instanceof Player player) || !containsHarmfulEffect(potion)) {
            return;
        }

        // Reward only when an entity other than the thrower is affected.
        boolean affectedOther = false;
        for (LivingEntity affected : event.getAffectedEntities()) {
            if (!affected.getUniqueId().equals(player.getUniqueId())) {
                affectedOther = true;
                break;
            }
        }
        if (!affectedOther) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.APPLY_DEBUFF);
    }

    /**
     * Awards EXP when a player begins curing a weakened zombie villager.
     * @param event The player interact entity event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCureZombieVillager(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !(event.getRightClicked() instanceof ZombieVillager zombie)) {
            return;
        }
        // Skip villagers that are already converting to avoid double rewards.
        if (zombie.isConverting()) {
            return;
        }

        Player player = event.getPlayer();
        Material held = player.getInventory().getItemInMainHand().getType();
        if (held != Material.GOLDEN_APPLE && held != Material.ENCHANTED_GOLDEN_APPLE) {
            return;
        }
        // A cure only starts when the zombie villager is under Weakness.
        if (!hasWeakness(zombie)) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        exp.awardExp(player, ExpAction.CURE_ZOMBIE_VILLAGER);
    }

    /**
     * Clears tracked cooldown state when a player leaves the server.
     * @param event The player quit event.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        igniteCooldown.clear(event.getPlayer().getUniqueId());
    }

    /**
     * Determines whether a thrown potion carries at least one harmful effect.
     * @param potion The thrown potion.
     * @return true if the potion applies a tracked harmful effect.
     */
    private boolean containsHarmfulEffect(ThrownPotion potion) {
        for (PotionEffect effect : potion.getEffects()) {
            if (HARMFUL_EFFECTS.contains(effect.getType().getKey().getKey().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether a living entity currently has the Weakness effect.
     * @param entity The entity to check.
     * @return true if a Weakness effect is active.
     */
    private boolean hasWeakness(LivingEntity entity) {
        for (PotionEffect effect : entity.getActivePotionEffects()) {
            if (effect.getType().getKey().getKey().equalsIgnoreCase("weakness")) {
                return true;
            }
        }
        return false;
    }
}

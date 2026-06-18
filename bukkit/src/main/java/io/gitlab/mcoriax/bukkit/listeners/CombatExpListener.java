package io.gitlab.mcoriax.bukkit.listeners;

import io.gitlab.mcoriax.api.model.ActiveIdentitySession;
import io.gitlab.mcoriax.bukkit.scheduler.Schedulers;
import io.gitlab.mcoriax.bukkit.util.ExpAction;
import io.gitlab.mcoriax.bukkit.util.ExperienceManager;
import io.gitlab.mcoriax.common.MCIdentityManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles combat and defense related EXP actions:
 * {@code deal_ranged_damage}, {@code critical_hit}, {@code shield_block} and {@code dodge_action}.
 *
 * <p>Both handlers listen to {@link EntityDamageByEntityEvent} so that the shield and
 * dodge mechanics only apply to genuine attacks (not fall or fire damage). The victim
 * handler runs at {@code HIGH} priority while the attacker reward runs at {@code MONITOR},
 * ensuring a successful dodge cancels the event before any attacker reward is granted.</p>
 */
public class CombatExpListener implements Listener {

    /** Maximum dodge chance an identity can reach regardless of agility. */
    private static final double MAX_DODGE_CHANCE = 0.30D;
    /** Dodge chance contributed by each point of agility. */
    private static final double DODGE_CHANCE_PER_AGILITY = 0.002D;

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;

    /**
     * Creates a new CombatExpListener.
     * @param plugin The plugin instance.
     */
    public CombatExpListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Resolves shield blocking and agility based dodges for the player being attacked.
     * @param event The entity damage by entity event.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTakeDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        Optional<ActiveIdentitySession> optSession = MCIdentityManager.getSession(player.getUniqueId());
        if (optSession.isEmpty()) {
            return;
        }

        // Shield block: vanilla already mitigates the damage, we only reward it.
        if (player.isBlocking()) {
            exp.awardExp(player, ExpAction.SHIELD_BLOCK, event.getDamage());
            return;
        }

        // Dodge: an agility based chance to fully avoid the incoming attack.
        int agility = optSession.get().stats().getAgility();
        if (agility > 0 && rollDodge(agility)) {
            event.setCancelled(true);
            exp.awardExp(player, ExpAction.DODGE_ACTION);
            notifyDodge(player);
        }
    }

    /**
     * Awards EXP for ranged damage and critical melee hits dealt by a player.
     * @param event The entity damage by entity event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDealDamage(EntityDamageByEntityEvent event) {
        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        // Ranged damage: the damager is a projectile shot by a player.
        if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player shooter) {
                exp.awardExp(shooter, ExpAction.DEAL_RANGED_DAMAGE, event.getFinalDamage());
            }
            return;
        }

        // Critical melee hit: the damager is the player directly.
        if (event.getDamager() instanceof Player player && isCriticalHit(player)) {
            exp.awardExp(player, ExpAction.CRITICAL_HIT, event.getFinalDamage());
        }
    }

    /**
     * Rolls a dodge attempt based on the identity's agility stat.
     * @param agility The agility value.
     * @return true if the dodge succeeds.
     */
    private boolean rollDodge(int agility) {
        double chance = Math.min(MAX_DODGE_CHANCE, agility * DODGE_CHANCE_PER_AGILITY);
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    /**
     * Sends a dodge notification to the player on the correct thread.
     * @param player The player who dodged.
     */
    private void notifyDodge(Player player) {
        Schedulers.forEntity(player).run(plugin, task -> {
            player.sendMessage(ChatColor.AQUA + "You dodged the attack!");
        }, null);
    }

    /**
     * Approximates the vanilla critical-hit condition for a melee attack.
     * @param player The attacking player.
     * @return true if the hit qualifies as a critical hit.
     */
    private boolean isCriticalHit(Player player) {
        return player.getFallDistance() > 0.0F
                && !player.isOnGround()
                && !player.isSprinting()
                && player.getVehicle() == null
                && !player.hasPotionEffect(PotionEffectType.BLINDNESS);
    }
}

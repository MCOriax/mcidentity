package io.gitlab.mcoriax.bukkit.listeners;

import io.gitlab.mcoriax.bukkit.util.ExpAction;
import io.gitlab.mcoriax.bukkit.util.ExperienceManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Set;

/**
 * Handles team and support related EXP actions: {@code apply_buff}.
 *
 * <p>The reward fires when a player lands a beneficial (non-healing) potion on at
 * least one other player. Instant-healing effects are intentionally excluded so
 * this action stays distinct from the existing {@code heal_target} action.</p>
 */
public class SupportExpListener implements Listener {

    /**
     * Beneficial potion effect keys treated as buffs. Effects are matched by their
     * namespaced key so the set stays stable across Minecraft version renames.
     */
    private static final Set<String> BENEFICIAL_EFFECTS = Set.of(
            "speed",
            "haste",
            "strength",
            "jump_boost",
            "resistance",
            "fire_resistance",
            "water_breathing",
            "night_vision",
            "invisibility",
            "health_boost",
            "absorption",
            "luck",
            "slow_falling",
            "dolphins_grace",
            "conduit_power",
            "hero_of_the_village"
    );

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;

    /**
     * Creates a new SupportExpListener.
     * @param plugin The plugin instance.
     */
    public SupportExpListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Awards EXP when a player buffs another player with a thrown potion.
     * @param event The potion splash event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        ThrownPotion potion = event.getPotion();
        ProjectileSource source = potion.getShooter();
        if (!(source instanceof Player thrower) || !containsBeneficialEffect(potion)) {
            return;
        }

        // Reward only when a player other than the thrower receives the buff.
        for (LivingEntity affected : event.getAffectedEntities()) {
            if (affected instanceof Player target && !target.getUniqueId().equals(thrower.getUniqueId())) {
                exp.awardExp(thrower, ExpAction.APPLY_BUFF);
                return;
            }
        }
    }

    /**
     * Determines whether a thrown potion carries at least one beneficial buff effect.
     * @param potion The thrown potion.
     * @return true if the potion applies a tracked beneficial effect.
     */
    private boolean containsBeneficialEffect(ThrownPotion potion) {
        for (PotionEffect effect : potion.getEffects()) {
            if (BENEFICIAL_EFFECTS.contains(effect.getType().getKey().getKey().toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}

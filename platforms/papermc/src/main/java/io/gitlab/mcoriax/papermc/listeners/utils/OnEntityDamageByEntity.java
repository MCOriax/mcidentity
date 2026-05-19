package io.gitlab.mcoriax.papermc.listeners.utils;

import io.gitlab.mcoriax.api.model.ActiveIdentitySession;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.papermc.MCIdentityPlugin;
import io.gitlab.mcoriax.papermc.util.ExperienceManager;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Optional;

public class OnEntityDamageByEntity {
    /**
     * Handle the entity damage by entity event.
     * @param plugin The plugin instance.
     * @param event The entity damage by entity event.
     */
    public static void execute(MCIdentityPlugin plugin, EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            
            // Retrieve the active identity session to get the player's stats
            Optional<ActiveIdentitySession> sessionOpt = MCIdentityManager.getSession(player.getUniqueId());
            if (sessionOpt.isPresent()) {
                int strength = sessionOpt.get().stats().getStrength();
                
                // Increase the damage based on the strength stat
                if (strength > 0) {
                    double currentDamage = event.getDamage();
                    event.setDamage(currentDamage + strength);
                }
            }

            // Award experience based on the final damage dealt
            ((ExperienceManager) MCIdentityManager.getExperienceManager()).awardExp(player, "deal_damage", event.getFinalDamage());
        }
    }
}

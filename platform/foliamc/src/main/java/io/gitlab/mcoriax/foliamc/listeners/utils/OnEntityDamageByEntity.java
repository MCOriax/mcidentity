package io.gitlab.mcoriax.foliamc.listeners.utils;

import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import io.gitlab.mcoriax.foliamc.util.ExperienceManager;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class OnEntityDamageByEntity {
    /**
     * Handle the entity damage by entity event.
     */
    public static void execute(MCIdentityPlugin plugin, EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            ((ExperienceManager) MCIdentityManager.getExperienceManager()).awardExp(player, "deal_damage", event.getFinalDamage());
        }
    }
}

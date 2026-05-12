package io.gitlab.mcoriax.foliamc.listeners.utils;

import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class OnEntityDamageByEntity {
    /**
     * Handle the entity damage by entity event.
     */
    public static void execute(MCIdentityPlugin plugin, EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            plugin.getExperienceManager().awardExp(player, "deal_damage", event.getFinalDamage());
        }
    }
}

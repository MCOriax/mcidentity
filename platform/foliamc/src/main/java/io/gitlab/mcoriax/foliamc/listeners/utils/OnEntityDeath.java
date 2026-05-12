package io.gitlab.mcoriax.foliamc.listeners.utils;

import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public class OnEntityDeath {
    /**
     * Handle the entity death event.
     */
    public static void execute(MCIdentityPlugin plugin, EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player player = event.getEntity().getKiller();
            plugin.getExperienceManager().awardExp(player, "kill_mob", event.getEntity().getType().name().toLowerCase());
        }
    }
}

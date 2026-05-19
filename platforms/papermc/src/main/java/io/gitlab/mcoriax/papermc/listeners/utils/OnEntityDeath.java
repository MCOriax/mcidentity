package io.gitlab.mcoriax.papermc.listeners.utils;

import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.papermc.MCIdentityPlugin;
import io.gitlab.mcoriax.papermc.util.ExperienceManager;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public class OnEntityDeath {
    /**
     * Handle the entity death event.
     * @param plugin The plugin instance.
     * @param event The entity death event.
     */
    public static void execute(MCIdentityPlugin plugin, EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player player = event.getEntity().getKiller();
            ((ExperienceManager) MCIdentityManager.getExperienceManager()).awardExp(player, "kill_mob", event.getEntity().getType().name().toLowerCase());
        }
    }
}

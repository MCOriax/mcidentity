package io.github.mcoriax.mcidentity.bukkit.listeners.utils;

import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.mcidentity.bukkit.util.ExpAction;
import io.github.mcoriax.mcidentity.bukkit.util.ExperienceManager;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public class OnEntityDeath {
    /**
     * Handle the entity death event.
     * @param plugin The plugin instance.
     * @param event The entity death event.
     */
    public static void execute(JavaPlugin plugin, EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player player = event.getEntity().getKiller();
            ((ExperienceManager) MCIdentityManager.getExperienceManager()).awardExp(player, ExpAction.KILL_MOB, event.getEntity().getType().name().toLowerCase());
        }
    }
}

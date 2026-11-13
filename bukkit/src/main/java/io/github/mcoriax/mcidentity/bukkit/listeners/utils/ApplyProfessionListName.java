package io.github.mcoriax.mcidentity.bukkit.listeners.utils;

import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Writes a player's tab list entry from their worn profession.
 *
 * <p>The name is read from the RAM session cache and applied on the thread that
 * owns the player, so the call is safe from a profession event handler on both a
 * single-threaded server and a regionized Folia one. Nothing here touches the
 * database.</p>
 */
public final class ApplyProfessionListName {

    /**
     * Prevents instantiation of this utility holder.
     */
    private ApplyProfessionListName() {
    }

    /**
     * Refreshes the player's tab list name from their cached session.
     * @param plugin The plugin instance.
     * @param player The player whose tab list entry is refreshed.
     */
    public static void execute(JavaPlugin plugin, Player player) {
        Schedulers.forEntity(player).run(plugin, task -> {
            if (!player.isOnline()) {
                return;
            }
            // The tag is rebuilt here rather than at scheduling time, so it
            // reads the profession that actually took effect.
            String listName = BuildProfessionNameTag.execute(player);
            if (!listName.equals(player.getPlayerListName())) {
                player.setPlayerListName(listName);
            }
        }, null);
    }
}

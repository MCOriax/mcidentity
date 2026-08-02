package io.github.mcoriax.mcidentity.bukkit.listeners.utils;

import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Writes a player's tab list entry from their active identity.
 *
 * <p>The name is read from the RAM session cache and applied on the thread that
 * owns the player, so the call is safe from an identity event handler on both a
 * single-threaded server and a regionized Folia one. Nothing here touches the
 * database.</p>
 */
public final class ApplyIdentityListName {

    /**
     * Prevents instantiation of this utility holder.
     */
    private ApplyIdentityListName() {
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
            // The tag is rebuilt here rather than at scheduling time, because
            // the equip and unequip events fire before the session cache has
            // settled; reading it on the owning thread picks up the identity
            // that actually took effect.
            String listName = BuildIdentityNameTag.execute(player);
            if (!listName.equals(player.getPlayerListName())) {
                player.setPlayerListName(listName);
            }
        }, null);
    }
}

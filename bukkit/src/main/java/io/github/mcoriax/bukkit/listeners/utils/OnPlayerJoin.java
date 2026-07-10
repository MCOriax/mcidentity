package io.github.mcoriax.bukkit.listeners.utils;
import org.bukkit.ChatColor;
import io.github.mcoriax.bukkit.scheduler.Schedulers;

import io.github.mcoriax.common.BaseProfession;
import io.github.mcoriax.common.MCIdentityManager;
import io.github.mcoriax.common.MCIdentityProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.bukkit.listeners.MCIdentityStatsListener;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

public class OnPlayerJoin {
    /**
     * Handle the player join event. A player with a stored session resumes it;
     * anyone else — including first-time joiners, for whom the permanent base
     * identity is created in the database — equips the base "player"
     * profession, so no player is ever without a profession.
     * @param plugin The plugin instance.
     * @param event The player join event.
     */
    public static void execute(JavaPlugin plugin, PlayerJoinEvent event) {
        Player player = event.getPlayer();
        MCIdentityProvider.instance.getSession(player.getUniqueId()).thenAccept(optSession -> {
            optSession.ifPresentOrElse(session -> {
                MCIdentityProvider.instance.loadSessionToRam(player.getUniqueId(), session.getIdentityUniqueId(), session.getContentKey()).thenRun(() -> {
                    Schedulers.forEntity(player).run(plugin, st -> {
                        MCIdentityManager.getSession(player.getUniqueId()).ifPresent(s -> {
                            ((MCIdentityStatsListener) MCIdentityManager.getStatsListener()).updateVisuals(player, s);
                        });

                        if (BaseProfession.isBaseIdentity(player.getUniqueId(), session.getIdentityUniqueId())) {
                            player.sendMessage(ChatColor.GREEN + "You are playing as the base Player profession.");
                        } else {
                            player.sendMessage(ChatColor.GREEN + "You are currently wearing an identity.");
                        }
                    }, null);
                });
            }, () -> EquipBaseProfession.execute(plugin, player, null,
                ChatColor.GREEN + "You are playing as the base Player profession.", false));
        });
    }
}

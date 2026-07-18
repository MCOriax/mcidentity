package io.github.mcoriax.mcidentity.bukkit.listeners.utils;
import org.bukkit.ChatColor;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;

import io.github.mcoriax.mcidentity.common.BaseProfession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.mcidentity.bukkit.listeners.MCIdentityStatsListener;
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

        // Cache the unlocked alternative slot count so base identities — the
        // primary and every alternative — can be recognised synchronously by
        // their deterministic UUID rather than by profession.
        MCIdentityProvider.instance.getBaseIdentitySlots(player.getUniqueId())
            .thenAccept(slots -> MCIdentityManager.cacheBaseIdentitySlots(player.getUniqueId(), slots));

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

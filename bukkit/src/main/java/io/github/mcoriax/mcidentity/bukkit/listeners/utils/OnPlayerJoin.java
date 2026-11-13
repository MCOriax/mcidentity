package io.github.mcoriax.mcidentity.bukkit.listeners.utils;

import io.github.mcoriax.mcidentity.common.model.PlayerStatsRecord;
import io.github.mcoriax.mcidentity.bukkit.profession.ProfessionWearing;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;
import io.github.mcoriax.mcidentity.common.BaseProfession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/**
 * Loads a joining player's character: their stats, the profession they wear,
 * and its progress.
 */
public final class OnPlayerJoin {

    private OnPlayerJoin() {
    }

    /**
     * Handles the player join event. A first time player gets a stats row from
     * the base profession's starting stats; every player then loads their
     * session, wearing the base profession when none is stored.
     * @param plugin The plugin instance.
     * @param event The player join event.
     */
    public static void execute(JavaPlugin plugin, PlayerJoinEvent event) {
        Player player = event.getPlayer();
        MCIdentityProvider provider = MCIdentityProvider.instance;

        provider.createPlayer(player.getUniqueId(), startingStats())
            .thenCompose(v -> provider.loadSession(player.getUniqueId()))
            .thenAccept(optSession -> Schedulers.forEntity(player).run(plugin, st -> {
                if (optSession.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Your profession data could not be loaded.");
                    return;
                }
                ProfessionWearing.refresh(plugin, player);
                String code = optSession.get().professionCode();
                player.sendMessage(BaseProfession.isBaseProfession(code)
                    ? ChatColor.GREEN + "You are playing as the base Player profession."
                    : ChatColor.GREEN + "You are wearing " + MCIdentityManager.getProfessionDisplayName(code) + ".");
            }, null))
            .exceptionally(ex -> {
                plugin.getLogger().severe("Failed to load the profession data of " + player.getName() + ": " + ex.getMessage());
                return null;
            });
    }

    /**
     * Builds a new player's stats from the base profession's starting stats.
     * @return The starting stats.
     */
    private static PlayerStatsRecord startingStats() {
        Map<String, Integer> base = MCIdentityManager.getProfessionBaseStats(BaseProfession.CODE);
        return new PlayerStatsRecord(0,
            base.getOrDefault("energy", 100),
            base.getOrDefault("health", 20),
            base.getOrDefault("food", 20),
            base.getOrDefault("strength", 0),
            base.getOrDefault("defense", 0),
            base.getOrDefault("wisdom", 0),
            base.getOrDefault("luck", 0),
            base.getOrDefault("agility", 0),
            base.getOrDefault("resilience", 0));
    }
}

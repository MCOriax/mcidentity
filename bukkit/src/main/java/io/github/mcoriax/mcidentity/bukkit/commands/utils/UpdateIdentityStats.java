package io.github.mcoriax.mcidentity.bukkit.commands.utils;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;

import io.github.mcoriax.mcidentity.api.model.ActiveProfessionSession;
import io.github.mcoriax.mcidentity.api.model.PlayerStats;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import io.github.mcoriax.mcidentity.common.model.PlayerStatsRecord;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.mcidentity.bukkit.util.MessageComponents;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class UpdateIdentityStats {
    /**
     * Spends stat points on one stat, in the session and in storage.
     * @param plugin The plugin instance.
     * @param player The player who triggered the update.
     * @param session The player's cached session.
     * @param statType The type of stat to update.
     * @param amount The amount to add to the stat.
     */
    public static void execute(JavaPlugin plugin, Player player, ActiveProfessionSession session, String statType, int amount) {
        PlayerStats currentStats = session.stats();
        
        int energy = currentStats.getEnergy();
        int health = currentStats.getHealth();
        int strength = currentStats.getStrength();
        int defense = currentStats.getDefense();
        int wisdom = currentStats.getWisdom();
        int luck = currentStats.getLuck();
        int agility = currentStats.getAgility();
        int resilience = currentStats.getResilience();
        int food = currentStats.getFood();

        switch (statType) {
            case "energy" -> energy += amount;
            case "health" -> health += amount;
            case "strength" -> strength += amount;
            case "defense" -> defense += amount;
            case "wisdom" -> wisdom += amount;
            case "luck" -> luck += amount;
            case "agility" -> agility += amount;
            case "resilience" -> resilience += amount;
            case "food" -> food += amount;
            default -> {
                player.sendMessage(ChatColor.RED + MessageComponents.value(
                        "mcidentity.command.error.invalid_stat_type.prefix",
                        "Invalid stat type: ",
                        statType,
                        "mcidentity.command.error.invalid_stat_type.suffix",
                        ""
                ));
                return;
            }
        }

        PlayerStatsRecord newStats = new PlayerStatsRecord(currentStats.getStatPoints() - amount,
            energy, health, food, strength, defense, wisdom, luck, agility, resilience);

        // RAM first so the GUI and action bar move at once; the provider then
        // writes storage and refreshes the cache again on completion.
        MCIdentityManager.updateStats(player.getUniqueId(), newStats);
        MCIdentityProvider.instance.updateStats(player.getUniqueId(), newStats
        ).thenAccept(v -> {
            Schedulers.forEntity(player).run(plugin, st -> {
                player.sendMessage(statsUpdatedMessage(amount, statType));
            }, null);
        }).exceptionally(ex -> {
            Schedulers.forEntity(player).run(plugin, st -> {
                String errorMessage = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
                player.sendMessage(ChatColor.RED + MessageComponents.value(
                        "mcidentity.command.error.stats_update_failed.prefix",
                        "Failed to update stats: ",
                        errorMessage,
                        "mcidentity.command.error.stats_update_failed.suffix",
                        ""
                ));
            }, null);
            plugin.getLogger().log(Level.WARNING,
                    "Failed to update stats for " + player.getName(), ex);
            return null;
        });
    }

    private static String statsUpdatedMessage(int amount, String statType) {
        return ChatColor.GREEN + MessageComponents.values(
                        "mcidentity.command.success.stats_updated.prefix",
                        "Stats updated successfully! Added ",
                        amount,
                        "mcidentity.command.success.stats_updated.middle",
                        " to ",
                        statType,
                        "mcidentity.command.success.stats_updated.suffix",
                        "."
                );
    }
}

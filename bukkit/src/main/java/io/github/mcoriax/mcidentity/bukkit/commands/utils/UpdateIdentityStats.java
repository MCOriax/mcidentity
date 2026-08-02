package io.github.mcoriax.mcidentity.bukkit.commands.utils;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;

import org.bukkit.Bukkit;

import io.github.mcoriax.mcidentity.api.model.ActiveIdentitySession;
import io.github.mcoriax.mcidentity.api.model.IdentityProperties;
import io.github.mcoriax.mcidentity.api.model.IdentityStats;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.mcidentity.bukkit.util.MessageComponents;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UpdateIdentityStats {
    /**
     * Update identity stats in the database and session.
     * @param plugin The plugin instance.
     * @param player The player who triggered the update.
     * @param session The active identity session.
     * @param statType The type of stat to update.
     * @param amount The amount to add to the stat.
     */
    public static void execute(JavaPlugin plugin, Player player, ActiveIdentitySession session, String statType, int amount) {
        IdentityStats currentStats = session.stats();
        
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

        final int finalEnergy = energy;
        final int finalHealth = health;
        final int finalStrength = strength;
        final int finalDefense = defense;
        final int finalWisdom = wisdom;
        final int finalLuck = luck;
        final int finalAgility = agility;
        final int finalResilience = resilience;
        final int finalFood = food;

        IdentityStats newStats = new IdentityStats() {
            @Override public int getEnergy() { return finalEnergy; }
            @Override public int getHealth() { return finalHealth; }
            @Override public int getStrength() { return finalStrength; }
            @Override public int getDefense() { return finalDefense; }
            @Override public int getWisdom() { return finalWisdom; }
            @Override public int getLuck() { return finalLuck; }
            @Override public int getAgility() { return finalAgility; }
            @Override public int getResilience() { return finalResilience; }
            @Override public int getFood() { return finalFood; }
        };

        IdentityProperties oldProps = session.properties();
        IdentityProperties newProps = new IdentityProperties() {
            private String inventoryContents = oldProps.getInventoryContents();
            private String enderchestContents = oldProps.getEnderchestContents();

            @Override public String getAlias() { return oldProps.getAlias(); }
            @Override public int getLevel() { return oldProps.getLevel(); }
            @Override public int getExp() { return oldProps.getExp(); }
            @Override public int getStatPoints() { return oldProps.getStatPoints() - amount; }
            @Override public String getHeadText() { return oldProps.getHeadText(); }
            @Override public String getInventoryContents() { return inventoryContents; }
            @Override public void setInventoryContents(String inventoryContents) { this.inventoryContents = inventoryContents; }
            @Override public String getEnderchestContents() { return enderchestContents; }
            @Override public void setEnderchestContents(String enderchestContents) { this.enderchestContents = enderchestContents; }
        };

        UUID identityUuid = session.identityUuid();
        
        CompletableFuture.allOf(
            MCIdentityProvider.instance.updateStats(identityUuid, newStats),
            MCIdentityProvider.instance.updateProperties(identityUuid, newProps)
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
            ex.printStackTrace();
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

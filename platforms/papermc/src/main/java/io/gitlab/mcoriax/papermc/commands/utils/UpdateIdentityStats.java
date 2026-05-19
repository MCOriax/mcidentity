package io.gitlab.mcoriax.papermc.commands.utils;

import org.bukkit.Bukkit;

import io.gitlab.mcoriax.api.model.ActiveIdentitySession;
import io.gitlab.mcoriax.api.model.IdentityProperties;
import io.gitlab.mcoriax.api.model.IdentityStats;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.papermc.MCIdentityPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    public static void execute(MCIdentityPlugin plugin, Player player, ActiveIdentitySession session, String statType, int amount) {
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
                player.sendMessage(Component.translatable("mcidentity.command.error.invalid_stat_type", "Invalid stat type: " + statType, Component.text(statType)).color(NamedTextColor.RED));
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
            private String skin = oldProps.getSkin();
            private String aura = oldProps.getAura();
            private String inventoryContents = oldProps.getInventoryContents();
            private String enderchestContents = oldProps.getEnderchestContents();

            @Override public int getLevel() { return oldProps.getLevel(); }
            @Override public int getExp() { return oldProps.getExp(); }
            @Override public int getStatPoints() { return oldProps.getStatPoints() - amount; }
            @Override public String getHeadText() { return oldProps.getHeadText(); }
            @Override public String getSkin() { return skin; }
            @Override public void setSkin(String skin) { this.skin = skin; }
            @Override public String getAura() { return aura; }
            @Override public void setAura(String aura) { this.aura = aura; }
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
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!plugin.isEnabled() || !player.isOnline()) return;
                player.sendMessage(Component.translatable("mcidentity.command.success.stats_updated", "Stats updated successfully! Added " + amount + " to " + statType + ".", Component.text(amount), Component.text(statType)).color(NamedTextColor.GREEN));
            });
        }).exceptionally(ex -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!plugin.isEnabled() || !player.isOnline()) return;
                String errorMessage = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
                player.sendMessage(Component.translatable("mcidentity.command.error.stats_update_failed", "Failed to update stats: " + errorMessage, Component.text(errorMessage)).color(NamedTextColor.RED));
            });
            ex.printStackTrace();
            return null;
        });
    }
}



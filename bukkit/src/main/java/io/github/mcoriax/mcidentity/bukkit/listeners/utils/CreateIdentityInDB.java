package io.github.mcoriax.mcidentity.bukkit.listeners.utils;

import io.github.mcoriax.mcidentity.api.model.IdentityKey;
import io.github.mcoriax.mcidentity.api.model.IdentityProperties;
import io.github.mcoriax.mcidentity.api.model.IdentityStats;
import io.github.mcoriax.mcidentity.api.model.Profession;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class CreateIdentityInDB {
    /**
     * Create an identity record in the database.
     * @param plugin The plugin instance.
     * @param identityUuid The identity UUID.
     * @param profession The profession for the identity.
     * @param headTexture The head texture for the identity.
     * @return A CompletableFuture representing the creation process.
     */
    public static CompletableFuture<Void> execute(JavaPlugin plugin, UUID identityUuid, Profession profession, String headTexture) {
        String professionCode = profession.getSystemCode();
        IdentityKey key = new IdentityKey() {
            @Override public UUID getUniqueId() { return identityUuid; }
            @Override public String getProfessionCode() { return professionCode; }
        };

        IdentityProperties props = new IdentityProperties() {
            private String skin;
            private String aura;
            private String inventoryContents;
            private String enderchestContents;

            @Override public int getLevel() { return 1; }
            @Override public int getExp() { return 0; }
            @Override public int getStatPoints() { return 0; }
            @Override public String getHeadText() { return headTexture; }
            @Override public String getSkin() { return skin; }
            @Override public void setSkin(String skin) { this.skin = skin; }
            @Override public String getAura() { return aura; }
            @Override public void setAura(String aura) { this.aura = aura; }
            @Override public String getInventoryContents() { return inventoryContents; }
            @Override public void setInventoryContents(String inventoryContents) { this.inventoryContents = inventoryContents; }
            @Override public String getEnderchestContents() { return enderchestContents; }
            @Override public void setEnderchestContents(String enderchestContents) { this.enderchestContents = enderchestContents; }
        };

        Map<String, Integer> baseStats = profession.getBaseStats();
        String statsRandomConfig = profession.getStatsRandom();

        int pointsToDistribute = 0;
        try {
            if (statsRandomConfig.contains("~")) {
                String[] parts = statsRandomConfig.split("~");
                if (parts.length == 2) {
                    int min = Integer.parseInt(parts[0].trim());
                    int max = Integer.parseInt(parts[1].trim());
                    pointsToDistribute = ThreadLocalRandom.current().nextInt((max - min) + 1) + min;
                }
            } else {
                pointsToDistribute = Integer.parseInt(statsRandomConfig.trim());
            }
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid stats_random configuration for " + professionCode + ": " + statsRandomConfig);
        }

        int[] points = new int[5];
        if (pointsToDistribute > 0) {
            for (int i = 0; i < 5; i++) {
                points[i] = ThreadLocalRandom.current().nextInt(pointsToDistribute + 1);
            }
            Arrays.sort(points);
        }

        final int s1 = points[0];
        final int s2 = points[1] - points[0];
        final int s3 = points[2] - points[1];
        final int s4 = points[3] - points[2];
        final int s5 = points[4] - points[3];
        final int s6 = pointsToDistribute - points[4];

        final int energyVal = baseStats.getOrDefault("energy", 100);
        final int healthVal = baseStats.getOrDefault("health", 20);
        final int foodVal = baseStats.getOrDefault("food", 20);
        final int strengthVal = baseStats.getOrDefault("strength", 0) + s1;
        final int defenseVal = baseStats.getOrDefault("defense", 0) + s2;
        final int wisdomVal = baseStats.getOrDefault("wisdom", 0) + s3;
        final int luckVal = baseStats.getOrDefault("luck", 0) + s4;
        final int agilityVal = baseStats.getOrDefault("agility", 0) + s5;
        final int resilienceVal = baseStats.getOrDefault("resilience", 0) + s6;

        IdentityStats stats = new IdentityStats() {
            @Override public int getEnergy() { return energyVal; }
            @Override public int getHealth() { return healthVal; }
            @Override public int getFood() { return foodVal; }
            @Override public int getStrength() { return strengthVal; }
            @Override public int getDefense() { return defenseVal; }
            @Override public int getWisdom() { return wisdomVal; }
            @Override public int getLuck() { return luckVal; }
            @Override public int getAgility() { return agilityVal; }
            @Override public int getResilience() { return resilienceVal; }
        };

        return MCIdentityProvider.instance.createIdentity(key, props, stats);
    }
}

package io.gitlab.mcoriax.bukkit.listeners.utils;
import org.bukkit.ChatColor;
import io.gitlab.mcoriax.bukkit.scheduler.Schedulers;

import io.gitlab.mcoriax.api.model.IdentityKey;
import io.gitlab.mcoriax.api.model.IdentityProperties;
import io.gitlab.mcoriax.api.model.IdentityStats;
import io.gitlab.mcoriax.api.model.Profession;
import io.gitlab.mcoriax.bukkit.event.MCIdentityCreationEvent;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.gitlab.mcoriax.bukkit.util.HeadTexture;
import io.gitlab.mcoriax.bukkit.util.ItemStackSerializer;
import io.gitlab.mcoriax.bukkit.util.SkullTextures;
import io.gitlab.mcoriax.bukkit.util.MessageComponents;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class FinalizeUnboundIdentity {
    /**
     * Finalize an unbound identity for a player.
     * @param plugin The plugin instance.
     * @param player The player finalizing the identity.
     * @param itemToEquip The item stack to update and equip.
     * @param professionCode The profession code for the identity.
     * @param reopenGui Whether to reopen the GUI after finalization.
     * @param textureKey The NamespacedKey for texture.
     * @param professionKey The NamespacedKey for profession code.
     * @param unidentifiedKey The NamespacedKey for identification status.
     * @param identityKey The NamespacedKey for identity UUID.
     */
    public static void execute(JavaPlugin plugin, Player player, ItemStack itemToEquip, String professionCode, boolean reopenGui, NamespacedKey textureKey, NamespacedKey professionKey, NamespacedKey unidentifiedKey, NamespacedKey identityKey) {
        MCIdentityProvider.instance.getProfessions().thenAccept(professions -> {
            Profession profession;
            if (professionCode.equalsIgnoreCase("random")) {
                List<String> loadedProfCodes = new ArrayList<>(MCIdentityManager.getRegisteredProfessions().keySet());
                if (loadedProfCodes.isEmpty()) {
                    RefundItemSafely.execute(plugin, player, itemToEquip, ChatColor.RED + "No professions loaded in memory!");
                    return;
                }
                
                String pickedCode = loadedProfCodes.get(ThreadLocalRandom.current().nextInt(loadedProfCodes.size()));
                Optional<Profession> optProf = professions.stream()
                        .filter(p -> p.getSystemCode().equalsIgnoreCase(pickedCode))
                        .findFirst();
                        
                if (optProf.isEmpty()) {
                    if (professions.isEmpty()) {
                        RefundItemSafely.execute(plugin, player, itemToEquip, ChatColor.RED + "No professions available!");
                        return;
                    }
                    List<Profession> profList = new ArrayList<>(professions);
                    profession = profList.get(ThreadLocalRandom.current().nextInt(profList.size()));
                } else {
                    profession = optProf.get();
                }
            } else {
                Optional<Profession> optProf = professions.stream().filter(p -> p.getSystemCode().equalsIgnoreCase(professionCode)).findFirst();
                if (optProf.isEmpty()) {
                    RefundItemSafely.execute(plugin, player, itemToEquip, ChatColor.RED + MessageComponents.value(
                            "mcidentity.gui.error.profession_not_found.prefix",
                            "Error: Profession '",
                            professionCode,
                            "mcidentity.gui.error.profession_not_found.suffix",
                            "' no longer exists!"
                    ));
                    return;
                }
                profession = optProf.get();
            }

            String actualProfCode = profession.getSystemCode();
            UUID identityUuid = UUID.randomUUID();
            SkullMeta meta = (SkullMeta) itemToEquip.getItemMeta();
            
            String resolvedTexture = null;
            if (meta != null) {
                resolvedTexture = meta.getPersistentDataContainer().get(textureKey, PersistentDataType.STRING);
                
                boolean isUnidentified = meta.getPersistentDataContainer().has(unidentifiedKey, PersistentDataType.BYTE);

                if (resolvedTexture == null || resolvedTexture.equals(HeadTexture.getUnidentified(plugin)) || isUnidentified) {
                    List<String> textures = new ArrayList<>(profession.getSkins().values());
                    if (!textures.isEmpty()) {
                        resolvedTexture = textures.get(ThreadLocalRandom.current().nextInt(textures.size()));
                    }
                }
            }
            final String finalTexture = resolvedTexture;

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
                plugin.getLogger().warning("Invalid stats_random configuration for " + actualProfCode + ": " + statsRandomConfig);
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

            IdentityKey key = new IdentityKey() {
                @Override public UUID getUniqueId() { return identityUuid; }
                @Override public String getProfessionCode() { return actualProfCode; }
            };

            IdentityProperties props = new IdentityProperties() {
                private String skin;
                private String aura;
                private String inventoryContents;
                private String enderchestContents;
                
                @Override public int getLevel() { return 1; }
                @Override public int getExp() { return 0; }
                @Override public int getStatPoints() { return 0; }
                @Override public String getHeadText() { return finalTexture; }
                @Override public String getSkin() { return skin; }
                @Override public void setSkin(String skin) { this.skin = skin; }
                @Override public String getAura() { return aura; }
                @Override public void setAura(String aura) { this.aura = aura; }
                @Override public String getInventoryContents() { return inventoryContents; }
                @Override public void setInventoryContents(String inventoryContents) { this.inventoryContents = inventoryContents; }
                @Override public String getEnderchestContents() { return enderchestContents; }
                @Override public void setEnderchestContents(String enderchestContents) { this.enderchestContents = enderchestContents; }
            };

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

            MCIdentityProvider.instance.createIdentity(key, props, stats).thenAccept(v -> {
                Schedulers.forEntity(player).run(plugin, st1 -> {
                    MCIdentityCreationEvent event = new MCIdentityCreationEvent(key, props, stats);
                    Bukkit.getPluginManager().callEvent(event);
                }, null);

                Schedulers.forEntity(player).run(plugin, st2 -> {
                    SkullMeta updatedMeta = (SkullMeta) itemToEquip.getItemMeta();
                    if (updatedMeta != null) {
                        updatedMeta.getPersistentDataContainer().set(identityKey, PersistentDataType.STRING, identityUuid.toString());
                        
                        if (finalTexture != null) {
                            SkullTextures.apply(updatedMeta, finalTexture);
                            updatedMeta.getPersistentDataContainer().set(textureKey, PersistentDataType.STRING, finalTexture);
                        } else {
                            updatedMeta.getPersistentDataContainer().remove(textureKey);
                        }

                        if (updatedMeta.getPersistentDataContainer().has(unidentifiedKey, PersistentDataType.BYTE)) {
                            ChatColor[] suitableColors = {
                                ChatColor.AQUA, ChatColor.BLUE, ChatColor.DARK_AQUA,
                                ChatColor.DARK_BLUE, ChatColor.DARK_GREEN, ChatColor.DARK_PURPLE,
                                ChatColor.DARK_RED, ChatColor.GOLD, ChatColor.GREEN,
                                ChatColor.LIGHT_PURPLE, ChatColor.RED, ChatColor.WHITE,
                                ChatColor.YELLOW
                            };
                            ChatColor randomNameColor = suitableColors[ThreadLocalRandom.current().nextInt(suitableColors.length)];
                            ChatColor randomLevelColor = suitableColors[ThreadLocalRandom.current().nextInt(suitableColors.length)];

                            updatedMeta.setDisplayName(ChatColor.GRAY + "[" + randomNameColor + profession.getDisplayName()
                                    + ChatColor.GRAY + "] [" + randomLevelColor + "1" + ChatColor.GRAY + "]");
                            updatedMeta.getPersistentDataContainer().remove(unidentifiedKey);
                        }
                        
                        updatedMeta.getPersistentDataContainer().set(professionKey, PersistentDataType.STRING, actualProfCode);
                        itemToEquip.setItemMeta(updatedMeta);
                    }
                    
                    Schedulers.async().runNow(plugin, st -> {
                        String updatedBase64 = ItemStackSerializer.serialize(itemToEquip);
                        ProcessEquip.execute(plugin, player, identityUuid, itemToEquip, updatedBase64, reopenGui);
                    });
                }, null);
            }).exceptionally(ex -> {
                String errorMessage = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
                RefundItemSafely.execute(plugin, player, itemToEquip, ChatColor.RED + MessageComponents.value(
                        "mcidentity.gui.error.registration_failed.prefix",
                        "Failed to register identity: ",
                        errorMessage,
                        "mcidentity.gui.error.registration_failed.suffix",
                        ""
                ));
                return null;
            });
        });
    }
}

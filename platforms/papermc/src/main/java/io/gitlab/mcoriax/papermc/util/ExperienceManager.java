package io.gitlab.mcoriax.papermc.util;

import io.gitlab.mcoriax.api.model.ActiveIdentitySession;
import io.gitlab.mcoriax.api.model.IdentityProperties;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.papermc.MCIdentityPlugin;
import io.gitlab.mcoriax.papermc.commands.MCIdentityCommand;
import io.gitlab.mcoriax.papermc.util.ItemStackSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles experience gain and leveling logic.
 */
public class ExperienceManager {

    /**
     * The plugin instance.
     */
    private final MCIdentityPlugin plugin;

    /**
     * Creates a new ExperienceManager.
     * @param plugin The plugin instance.
     */
    public ExperienceManager(MCIdentityPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Awards EXP to a player based on an action type and numerical amount (e.g., damage).
     * @param player The player to award EXP to.
     * @param action The type of action performed.
     * @param amount The numerical amount associated with the action.
     */
    public void awardExp(Player player, String action, double amount) {
        Optional<ActiveIdentitySession> optSession = MCIdentityManager.getSession(player.getUniqueId());
        if (optSession.isEmpty()) return;

        ActiveIdentitySession session = optSession.get();
        String professionCode = session.identityKey().getProfessionCode();
        Map<String, Object> expConfig = MCIdentityManager.getProfessionExpConfig(professionCode);
        Object configValue = expConfig.get(action);

        if (configValue instanceof Map) {
            Map<String, Object> nested = (Map<String, Object>) configValue;
            for (Map.Entry<String, Object> entry : nested.entrySet()) {
                String rangeKey = entry.getKey();
                if (isAmountInRange(amount, rangeKey)) {
                    processExpReward(player, session, entry.getValue().toString());
                    return;
                }
            }
        } else if (configValue != null) {
            processExpReward(player, session, configValue.toString());
        }
    }

    /**
     * Awards EXP to a player based on an action type and string target (e.g., mob type).
     * @param player The player to award EXP to.
     * @param action The type of action performed.
     * @param targetType The string target type (e.g., "zombie").
     */
    public void awardExp(Player player, String action, String targetType) {
        Optional<ActiveIdentitySession> optSession = MCIdentityManager.getSession(player.getUniqueId());
        if (optSession.isEmpty()) return;

        ActiveIdentitySession session = optSession.get();
        String professionCode = session.identityKey().getProfessionCode();
        Map<String, Object> expConfig = MCIdentityManager.getProfessionExpConfig(professionCode);
        Object configValue = expConfig.get(action);

        if (configValue instanceof Map) {
            Map<String, Object> nested = (Map<String, Object>) configValue;
            Object reward = nested.get(targetType.toLowerCase());
            if (reward != null) {
                processExpReward(player, session, reward.toString());
            }
        } else if (configValue != null) {
            processExpReward(player, session, configValue.toString());
        }
    }

    /**
     * Checks if a numerical amount falls within a specified range string.
     * @param amount The numerical amount.
     * @param rangeKey The range string (e.g., "1~10" or "5").
     * @return true if the amount is in range, false otherwise.
     */
    private boolean isAmountInRange(double amount, String rangeKey) {
        try {
            if (rangeKey.contains("~") || rangeKey.contains("-")) {
                String separator = rangeKey.contains("~") ? "~" : "-";
                String[] parts = rangeKey.split(separator);
                if (parts.length == 2) {
                    double min = Double.parseDouble(parts[0].trim());
                    double max = Double.parseDouble(parts[1].trim());
                    return amount >= min && amount <= max;
                }
            } else {
                double fixed = Double.parseDouble(rangeKey.trim());
                return amount == fixed;
            }
        } catch (NumberFormatException ignored) {}
        return false;
    }

    /**
     * Processes and awards an experience reward based on a configuration string.
     * @param player The player.
     * @param session The active identity session.
     * @param rewardStr The reward configuration string.
     */
    private void processExpReward(Player player, ActiveIdentitySession session, String rewardStr) {
        int expGained = calculateDynamicExp(rewardStr);
        if (expGained > 0) {
            calculateExpAndLevel(player, session, expGained);
        }
    }

    /**
     * Awards EXP to a player based on an action type.
     * @param player The player to award EXP to.
     * @param actionType The type of action performed.
     */
    public void awardExp(Player player, String actionType) {
        Optional<ActiveIdentitySession> optSession = MCIdentityManager.getSession(player.getUniqueId());
        if (optSession.isEmpty()) {
            return;
        }

        ActiveIdentitySession session = optSession.get();
        String professionCode = session.identityKey().getProfessionCode();
        Map<String, Object> expConfig = MCIdentityManager.getProfessionExpConfig(professionCode);
        Object configValue = expConfig.get(actionType);

        if (configValue != null && !(configValue instanceof Map)) {
            processExpReward(player, session, configValue.toString());
        }
    }

    /**
     * Calculates dynamic experience gain from a potential range string.
     * @param configValue The configuration value string.
     * @return The calculated experience amount.
     */
    private int calculateDynamicExp(String configValue) {
        try {
            if (configValue.contains("~")) {
                String[] parts = configValue.split("~");
                if (parts.length == 2) {
                    int min = Integer.parseInt(parts[0].trim());
                    int max = Integer.parseInt(parts[1].trim());
                    if (min <= max) {
                        return ThreadLocalRandom.current().nextInt((max - min) + 1) + min;
                    }
                }
            } else {
                return Integer.parseInt(configValue.trim());
            }
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid EXP configuration value: " + configValue);
        }
        return 0;
    }

    /**
     * Calculates experience and handles leveling up for an identity.
     * @param player The player.
     * @param session The active identity session.
     * @param expGained The amount of experience gained.
     */
    public void calculateExpAndLevel(Player player, ActiveIdentitySession session, int expGained) {
        String profession = session.identityKey().getProfessionCode().toLowerCase();
        MCIdentityManager.ProfessionLevelConfig levelConfig = MCIdentityManager.getProfessionLevelConfig(profession);
        int maxLevel = levelConfig.maxLevel();
        int expUp = levelConfig.expUp();

        IdentityProperties props = session.properties();
        int currentLevel = props.getLevel();
        int currentExp = props.getExp() + expGained;
        int currentStatPoints = props.getStatPoints();

        if (currentLevel >= maxLevel) {
            currentExp = 0;
        } else {
            while (currentExp >= expUp && currentLevel < maxLevel) {
                currentExp -= expUp;
                currentLevel++;
                currentStatPoints += 100;
                
                final int finalLevel = currentLevel;
                player.getScheduler().run(plugin, st -> {
                    String levelUpMessage = "Level Up! You are now level " + finalLevel + "!";
                    player.sendMessage(Component.translatable("mcidentity.experience.level_up", 
                        levelUpMessage,
                        Component.text(finalLevel)).color(NamedTextColor.GREEN));
                    
                    // Re-open GUI if currently open to refresh stats/level display
                    Component title = player.getOpenInventory().title();
                    if (title.equals(Component.translatable("mcidentity.gui.title", "Identity GUI"))) {
                        MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
                        if (cmd != null) cmd.openIdentityGui(player);
                    }
                }, null);
            }
            if (currentLevel >= maxLevel) {
                currentExp = 0;
            }
        }

        final int finalLevel = currentLevel;
        final int finalExp = currentExp;
        final int finalStatPoints = currentStatPoints;

        if (finalLevel > props.getLevel()) {
            String base64 = session.contentKey();
            if (base64 != null) {
                plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> {
                    try {
                        ItemStack item = ItemStackSerializer.deserialize(base64);
                        SkullMeta meta = (SkullMeta) item.getItemMeta();
                        if (meta != null && meta.hasDisplayName()) {
                            Component currentName = meta.displayName();
                            if (currentName != null) {
                                // The structure from InitializeIdentity is:
                                // Base: "[" (GRAY)
                                // Child 0: Profession (Random Color)
                                // Child 1: "] [" (GRAY)
                                // Child 2: Level (Random Color)
                                // Child 3: "]" (GRAY)
                                
                                List<Component> children = new ArrayList<>(currentName.children());
                                if (children.size() >= 3) {
                                    Component levelComponent = children.get(2);
                                    TextColor levelColor = levelComponent.color();
                                    
                                    children.set(2, Component.text(finalLevel, levelColor));
                                    
                                    // Rebuild component with updated level child
                                    Component newName = currentName.children(children);
                                    meta.displayName(newName);
                                    item.setItemMeta(meta);
                                    
                                    String updatedBase64 = ItemStackSerializer.serialize(item);
                                    MCIdentityProvider.instance.updateSessionContentKey(player.getUniqueId(), updatedBase64);
                                }
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("Failed to update identity item name on level up for " + player.getName());
                        e.printStackTrace();
                    }
                });
            }
        }

        IdentityProperties newProps = new IdentityProperties() {
            private String skin = props.getSkin();
            private String aura = props.getAura();
            private String inventoryContents = props.getInventoryContents();
            private String enderchestContents = props.getEnderchestContents();

            @Override public int getLevel() { return finalLevel; }
            @Override public int getExp() { return finalExp; }
            @Override public int getStatPoints() { return finalStatPoints; }
            @Override public String getHeadText() { return props.getHeadText(); }
            @Override public String getSkin() { return skin; }
            @Override public void setSkin(String skin) { this.skin = skin; }
            @Override public String getAura() { return aura; }
            @Override public void setAura(String aura) { this.aura = aura; }
            @Override public String getInventoryContents() { return inventoryContents; }
            @Override public void setInventoryContents(String inventoryContents) { this.inventoryContents = inventoryContents; }
            @Override public String getEnderchestContents() { return enderchestContents; }
            @Override public void setEnderchestContents(String enderchestContents) { this.enderchestContents = enderchestContents; }
        };

        // Optimistic Update (Update RAM immediately)
        MCIdentityManager.updateProperties(player.getUniqueId(), newProps);

        // Async Database Update
        MCIdentityProvider.instance.updateProperties(session.identityUuid(), newProps);
    }
}

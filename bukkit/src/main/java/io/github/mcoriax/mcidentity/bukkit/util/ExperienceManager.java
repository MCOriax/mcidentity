package io.github.mcoriax.mcidentity.bukkit.util;
import org.bukkit.ChatColor;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;

import io.github.mcoriax.mcidentity.api.model.ActiveIdentitySession;
import io.github.mcoriax.mcidentity.api.model.IdentityProperties;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.mcidentity.bukkit.commands.MCIdentityCommand;
import io.github.mcoriax.mcidentity.bukkit.util.ItemStackSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Handles experience gain and leveling logic.
 */
public class ExperienceManager {

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;

    /**
     * Creates a new ExperienceManager.
     * @param plugin The plugin instance.
     */
    public ExperienceManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Convenience accessor for the globally registered ExperienceManager.
     * @return The active ExperienceManager, or null if none has been registered yet.
     */
    public static ExperienceManager get() {
        Object manager = MCIdentityManager.getExperienceManager();
        return (manager instanceof ExperienceManager) ? (ExperienceManager) manager : null;
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

        IdentityProperties props = session.properties();
        Progress progress = advance(player, props, expGained, maxLevel, levelConfig.expUp());

        if (progress.level() > props.getLevel()) {
            retitleIdentityItem(player, session, props, progress.level());
        }

        IdentityProperties newProps = withProgress(props, progress);

        // Optimistic Update (Update RAM immediately)
        MCIdentityManager.updateProperties(player.getUniqueId(), newProps);

        // Async Database Update
        MCIdentityProvider.instance.updateProperties(session.identityUuid(), newProps);
    }

    /**
     * Where an identity stands after the EXP it just earned.
     *
     * @param level The identity's level.
     * @param exp The EXP left over towards the next level.
     * @param statPoints The unspent stat points.
     */
    private record Progress(int level, int exp, int statPoints) {
    }

    /**
     * Applies the earned EXP, announcing every level crossed along the way.
     * @param player The player earning the EXP.
     * @param props The identity's current properties.
     * @param expGained The EXP just earned.
     * @param maxLevel The profession's level cap.
     * @param expUp The EXP one level costs.
     * @return The identity's new level, EXP, and stat points.
     */
    private Progress advance(Player player, IdentityProperties props, int expGained, int maxLevel, int expUp) {
        int currentLevel = props.getLevel();
        int currentStatPoints = props.getStatPoints();

        if (currentLevel >= maxLevel) {
            return new Progress(currentLevel, 0, currentStatPoints);
        }

        int currentExp = props.getExp() + expGained;
        while (currentExp >= expUp && currentLevel < maxLevel) {
            currentExp -= expUp;
            currentLevel++;
            currentStatPoints += 100;
            announceLevelUp(player, currentLevel);
        }
        if (currentLevel >= maxLevel) {
            currentExp = 0;
        }
        return new Progress(currentLevel, currentExp, currentStatPoints);
    }

    /**
     * Tells the player they levelled, and refreshes the GUI if it is open.
     * @param player The player who levelled.
     * @param level The level just reached.
     */
    private void announceLevelUp(Player player, int level) {
        Schedulers.forEntity(player).run(plugin, st -> {
            player.sendMessage(levelUpMessage(level));

            // Re-open GUI if currently open to refresh stats/level display
            String title = player.getOpenInventory().getTitle();
            if (title.equals("Identity GUI")) {
                MCIdentityCommand cmd = (MCIdentityCommand) plugin.getCommand("identity").getExecutor();
                if (cmd != null) cmd.openIdentityGui(player);
            }
        }, null);
    }

    /**
     * Rewrites the stored identity item so its name and lore show the new level.
     * @param player The player who levelled.
     * @param session The active identity session.
     * @param props The identity's properties before the level up.
     * @param finalLevel The level just reached.
     */
    private void retitleIdentityItem(Player player, ActiveIdentitySession session, IdentityProperties props, int finalLevel) {
        String base64 = session.contentKey();
        if (base64 == null) {
            return;
        }

        Schedulers.async().runNow(plugin, scheduledTask -> {
            try {
                ItemStack item = ItemStackSerializer.deserialize(base64);
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                if (meta != null) {
                    // The name and lore are rebuilt from the session
                    // rather than edited in place. The old code parsed
                    // the level back out of the display name, so a player
                    // who renamed their identity item silently stopped
                    // receiving level updates on it; nothing here reads
                    // the item's text any more.
                    IdentityItemRenderer.apply(meta, session.identityUuid(), props.getAlias(),
                        session.identityKey().getProfessionCode(), finalLevel);
                    item.setItemMeta(meta);

                    String updatedBase64 = ItemStackSerializer.serialize(item);
                    MCIdentityProvider.instance.updateSessionContentKey(player.getUniqueId(), updatedBase64);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE,
                    "Failed to update identity item name on level up for " + player.getName(), e);
            }
        });
    }

    /**
     * Copies the identity's properties with the new level, EXP, and stat points.
     * @param props The identity's current properties.
     * @param progress Where the identity now stands.
     * @return The updated properties.
     */
    private static IdentityProperties withProgress(IdentityProperties props, Progress progress) {
        return new IdentityProperties() {
            private String inventoryContents = props.getInventoryContents();
            private String enderchestContents = props.getEnderchestContents();

            @Override public String getAlias() { return props.getAlias(); }
            @Override public int getLevel() { return progress.level(); }
            @Override public int getExp() { return progress.exp(); }
            @Override public int getStatPoints() { return progress.statPoints(); }
            @Override public String getHeadText() { return props.getHeadText(); }
            @Override public String getInventoryContents() { return inventoryContents; }
            @Override public void setInventoryContents(String inventoryContents) { this.inventoryContents = inventoryContents; }
            @Override public String getEnderchestContents() { return enderchestContents; }
            @Override public void setEnderchestContents(String enderchestContents) { this.enderchestContents = enderchestContents; }
        };
    }

    private static String levelUpMessage(int level) {
        return ChatColor.GREEN + MessageComponents.value(
                        "mcidentity.experience.level_up.prefix",
                        "Level Up! You are now level ",
                        level,
                        "mcidentity.experience.level_up.suffix",
                        "!"
                );
    }
}

package io.github.mcoriax.mcidentity.bukkit.util;
import org.bukkit.ChatColor;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;

import io.github.mcoriax.mcidentity.api.model.ActiveProfessionSession;
import io.github.mcoriax.mcidentity.api.model.PlayerStats;
import io.github.mcoriax.mcidentity.api.model.ProfessionProgress;
import io.github.mcoriax.mcidentity.common.BaseProfession;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import io.github.mcoriax.mcidentity.common.model.PlayerStatsRecord;
import io.github.mcoriax.mcidentity.common.model.ProfessionProgressRecord;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.mcidentity.bukkit.commands.MCIdentityCommand;
import org.bukkit.entity.Player;

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
        Optional<ActiveProfessionSession> optSession = MCIdentityManager.getSession(player.getUniqueId());
        if (optSession.isEmpty()) return;

        ActiveProfessionSession session = optSession.get();
        String professionCode = session.professionCode();
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
        Optional<ActiveProfessionSession> optSession = MCIdentityManager.getSession(player.getUniqueId());
        if (optSession.isEmpty()) return;

        ActiveProfessionSession session = optSession.get();
        String professionCode = session.professionCode();
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
     * @param session The player's cached session.
     * @param rewardStr The reward configuration string.
     */
    private void processExpReward(Player player, ActiveProfessionSession session, String rewardStr) {
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
        Optional<ActiveProfessionSession> optSession = MCIdentityManager.getSession(player.getUniqueId());
        if (optSession.isEmpty()) {
            return;
        }

        ActiveProfessionSession session = optSession.get();
        String professionCode = session.professionCode();
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
     * Adds EXP to the worn profession and handles levelling up.
     *
     * <p>Level and EXP belong to the worn profession; the stat points each level
     * grants go to the player's own stats, so they follow the player whichever
     * profession they wear next.</p>
     * @param player The player.
     * @param session The player's cached session.
     * @param expGained The amount of experience gained.
     */
    public void calculateExpAndLevel(Player player, ActiveProfessionSession session, int expGained) {
        String professionCode = session.professionCode();
        MCIdentityManager.ProfessionLevelConfig levelConfig = MCIdentityManager.getProfessionLevelConfig(professionCode);

        ProfessionProgress before = session.progress();
        ProfessionProgressRecord after = advance(player, before, expGained, levelConfig.maxLevel(), levelConfig.expUp());
        int levelsGained = after.level() - before.getLevel();

        // Optimistic update first, so the action bar moves at once; the
        // provider writes storage and refreshes the cache again on completion.
        MCIdentityManager.updateProgress(player.getUniqueId(), professionCode, after);
        MCIdentityProvider.instance.updateProgress(player.getUniqueId(), professionCode, after);

        if (levelsGained > 0) {
            PlayerStats stats = session.stats();
            PlayerStatsRecord newStats = PlayerStatsRecord.copyOf(stats)
                .withStatPoints(stats.getStatPoints() + levelsGained * BaseProfession.STAT_POINTS_PER_LEVEL);
            MCIdentityManager.updateStats(player.getUniqueId(), newStats);
            MCIdentityProvider.instance.updateStats(player.getUniqueId(), newStats);
        }
    }

    /**
     * Applies the earned EXP, announcing every level crossed along the way.
     * @param player The player earning the EXP.
     * @param progress The profession's current progress.
     * @param expGained The EXP just earned.
     * @param maxLevel The profession's level cap.
     * @param expUp The EXP one level costs.
     * @return The profession's new level and EXP.
     */
    private ProfessionProgressRecord advance(Player player, ProfessionProgress progress, int expGained, int maxLevel, int expUp) {
        int currentLevel = progress.getLevel();
        if (currentLevel >= maxLevel) {
            return new ProfessionProgressRecord(currentLevel, 0);
        }

        int currentExp = progress.getExp() + expGained;
        while (expUp > 0 && currentExp >= expUp && currentLevel < maxLevel) {
            currentExp -= expUp;
            currentLevel++;
            announceLevelUp(player, currentLevel);
        }
        if (currentLevel >= maxLevel) {
            currentExp = 0;
        }
        return new ProfessionProgressRecord(currentLevel, currentExp);
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
            if (title.equals("Identity GUI") && plugin.getCommand("identity") != null
                    && plugin.getCommand("identity").getExecutor() instanceof MCIdentityCommand cmd) {
                cmd.openIdentityGui(player);
            }
        }, null);
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

package io.gitlab.mcoriax.foliamc.util;

import io.gitlab.mcoriax.api.model.ActiveIdentitySession;
import io.gitlab.mcoriax.api.model.IdentityProperties;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles experience gain and leveling logic.
 */
public class ExperienceManager {

    private final MCIdentityPlugin plugin;

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
                player.getScheduler().run(plugin, task -> {
                    if (!plugin.isEnabled() || !player.isOnline()) {
                        return;
                    }
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Level Up! You are now level " + finalLevel + "!</green>"));
                }, null);
            }
            if (currentLevel >= maxLevel) {
                currentExp = 0;
            }
        }

        final int finalLevel = currentLevel;
        final int finalExp = currentExp;
        final int finalStatPoints = currentStatPoints;

        IdentityProperties newProps = new IdentityProperties() {
            private String skin = props.getSkin();
            private String aura = props.getAura();
            @Override public int getLevel() { return finalLevel; }
            @Override public int getExp() { return finalExp; }
            @Override public int getStatPoints() { return finalStatPoints; }
            @Override public String getHeadText() { return props.getHeadText(); }
            @Override public String getSkin() { return skin; }
            @Override public void setSkin(String skin) { this.skin = skin; }
            @Override public String getAura() { return aura; }
            @Override public void setAura(String aura) { this.aura = aura; }
        };

        // Optimistic Update (Update RAM immediately)
        MCIdentityManager.updateProperties(player.getUniqueId(), newProps);

        // Async Database Update
        MCIdentityProvider.instance.updateProperties(session.identityUuid(), newProps);
    }
}

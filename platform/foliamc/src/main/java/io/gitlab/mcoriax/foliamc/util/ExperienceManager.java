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
import java.util.Random;

/**
 * Handles experience gain and leveling logic.
 */
public class ExperienceManager {

    private final MCIdentityPlugin plugin;
    private final Random random = new Random();

    public ExperienceManager(MCIdentityPlugin plugin) {
        this.plugin = plugin;
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
        Map<String, String> expConfig = MCIdentityManager.getProfessionExpConfig(professionCode);
        String configValue = expConfig.get(actionType);

        if (configValue != null) {
            int expGained = calculateDynamicExp(configValue);
            if (expGained > 0) {
                calculateExpAndLevel(player, session, expGained);
            }
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
                        return random.nextInt((max - min) + 1) + min;
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
                    if (player.isOnline()) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Level Up! You are now level " + finalLevel + "!</green>"));
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

        IdentityProperties newProps = new IdentityProperties() {
            @Override public int getLevel() { return finalLevel; }
            @Override public int getExp() { return finalExp; }
            @Override public int getStatPoints() { return finalStatPoints; }
            @Override public String getHeadText() { return props.getHeadText(); }
            @Override public String getModel() { return props.getModel(); }
        };

        // Optimistic Update (Update RAM immediately)
        MCIdentityManager.updateProperties(player.getUniqueId(), newProps);

        // Async Database Update
        MCIdentityProvider.instance.updateProperties(session.identityUuid(), newProps);
    }
}

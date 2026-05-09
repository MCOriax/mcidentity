package io.gitlab.mcoriax.foliamc.listeners;

import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Optional;
import java.util.UUID;

public class MCIdentityStatsListener implements Listener {

    private final MCIdentityPlugin plugin;

    public MCIdentityStatsListener(MCIdentityPlugin plugin) {
        this.plugin = plugin;
        startVisualTask();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Optional<ActiveIdentitySession> optSession = MCIdentityManager.getSession(player.getUniqueId());
        if (optSession.isEmpty()) {
            return;
        }

        ActiveIdentitySession session = optSession.get();
        double damage = event.getFinalDamage();
        double currentHealth = session.currentHealth();

        // 1. Absorption (Must execute FIRST logic-wise)
        if (currentHealth >= damage) {
            MCIdentityManager.updateHealth(player.getUniqueId(), currentHealth - damage);
            event.setDamage(0);
        } else {
            double remainder = damage - currentHealth;
            MCIdentityManager.updateHealth(player.getUniqueId(), 0);
            event.setDamage(remainder);
        }

        // 2. EXP Gain
        double expGained = (damage / 10.0) * 30.0;
        calculateExpAndLevel(player, session, (int) expGained);
    }

    private void calculateExpAndLevel(Player player, ActiveIdentitySession session, int expGained) {
        String profession = session.identityKey().getProfessionCode().toLowerCase();
        int maxLevel = plugin.getConfig().getInt("professions." + profession + ".level.max", 100);
        int expUp = plugin.getConfig().getInt("professions." + profession + ".level.exp_up", 10000);

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

    private void startVisualTask() {
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                MCIdentityManager.getSession(player.getUniqueId()).ifPresent(session -> {
                    updateVisuals(player, session);
                });
            }
        }, 1, 1, java.util.concurrent.TimeUnit.SECONDS); // Update every second or faster? Requirement says "Continuously".
    }

    private void updateVisuals(Player player, ActiveIdentitySession session) {
        // 1. Action Bar
        double hpPercent = (session.currentHealth() / session.stats().getHealth()) * 100.0;
        double energyPercent = (session.currentEnergy() / session.stats().getEnergy()) * 100.0;
        
        String actionBarMsg = String.format("<red>HP: %.0f%%</red> | <blue>Energy: %.0f%%</blue>", hpPercent, energyPercent);
        player.sendActionBar(MiniMessage.miniMessage().deserialize(actionBarMsg));

        // 2. Scale Health Bar and Hunger Bar
        // Must run on player's region thread
        player.getScheduler().run(plugin, task -> {
            if (!player.isOnline() || player.isDead()) {
                return;
            }

            // Health Hijack Fix
            if (session.currentHealth() > 0) {
                double vanillaHealth = (hpPercent / 100.0) * 20.0;
                player.setHealth(Math.max(0.1, Math.min(20.0, vanillaHealth)));
            }

            double vanillaFood = (energyPercent / 100.0) * 20.0;
            player.setFoodLevel((int) Math.max(0, Math.min(20, vanillaFood)));
        }, null);
    }
}

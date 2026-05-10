package io.gitlab.mcoriax.foliamc.listeners;

import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.foliamc.MCIdentityPlugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
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
    private ScheduledTask visualTask;

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

        // 2. EXP Gain (Customizable via config)
        plugin.getExperienceManager().awardExp(player, "receive_damage", damage);
    }

    private void calculateExpAndLevel(Player player, ActiveIdentitySession session, int expGained) {
        plugin.getExperienceManager().calculateExpAndLevel(player, session, expGained);
    }

    private void startVisualTask() {
        visualTask = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {
            if (!plugin.isEnabled()) {
                task.cancel();
                return;
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                MCIdentityManager.getSession(player.getUniqueId()).ifPresent(session -> {
                    updateVisuals(player, session);
                });
            }
        }, 1, 1, java.util.concurrent.TimeUnit.SECONDS);
    }

    public void cancelTasks() {
        if (visualTask != null) {
            visualTask.cancel();
        }
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
            if (!plugin.isEnabled() || !player.isOnline() || player.isDead()) {
                return;
            }

            // Health Hijack Fix
            if (session.currentHealth() > 0) {
                double vanillaHealth = (hpPercent / 100.0) * 20.0;
                player.setHealth(Math.max(0.1, Math.min(20.0, vanillaHealth)));
            }
        }, null);
    }
}

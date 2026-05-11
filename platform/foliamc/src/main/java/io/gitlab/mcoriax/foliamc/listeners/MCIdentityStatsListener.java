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
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MCIdentityStatsListener implements Listener {

    private final MCIdentityPlugin plugin;
    private ScheduledTask visualTask;
    private final Map<UUID, Double> accumulatedRegenPercent = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastRegenTime = new ConcurrentHashMap<>();

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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Cancel vanilla regeneration if player has an active identity
        if (MCIdentityManager.getSession(player.getUniqueId()).isPresent()) {
            EntityRegainHealthEvent.RegainReason reason = event.getRegainReason();
            if (reason == EntityRegainHealthEvent.RegainReason.SATIATED || 
                reason == EntityRegainHealthEvent.RegainReason.REGEN) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        MCIdentityManager.getSession(player.getUniqueId()).ifPresent(session -> {
            int oldLevel = player.getFoodLevel();
            int newLevel = event.getFoodLevel();
            int delta = newLevel - oldLevel;

            double maxFood = session.stats().getFood();
            double currentFood = session.currentFood();
            double updatedFood = Math.max(0, Math.min(maxFood, currentFood + delta));

            MCIdentityManager.updateFood(player.getUniqueId(), updatedFood);
            event.setCancelled(true);
        });
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
        }, 500, 500, TimeUnit.MILLISECONDS);
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
        
        // Calculate food percentage relative to identity's food capacity
        double maxFood = session.stats().getFood();
        double currentFood = session.currentFood();
        double foodPercent = (currentFood / maxFood) * 100.0;
        
        String actionBarMsg = String.format("<red>HP: %.0f%%</red> | <gold>Food: %.0f%%</gold> | <blue>Energy: %.0f%%</blue>", 
                hpPercent, foodPercent, energyPercent);
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

            // Hunger Hijack Fix
            double vanillaFood = (foodPercent / 100.0) * 20.0;
            player.setFoodLevel((int) Math.max(0, Math.min(20, vanillaFood)));

            // Custom Health Regeneration Logic
            long currentTime = System.currentTimeMillis();
            long lastRegen = lastRegenTime.getOrDefault(player.getUniqueId(), 0L);

            if (currentTime - lastRegen >= 1500) {
                if (currentFood >= (maxFood * 0.10)) {
                    double maxHealth = session.stats().getHealth();
                    double currentHealth = session.currentHealth();

                    if (currentHealth < maxHealth) {
                        double healAmount = maxHealth * 0.01;
                        double actualHeal = Math.min(healAmount, maxHealth - currentHealth);
                        double healedPercent = actualHeal / maxHealth;

                        MCIdentityManager.updateHealth(player.getUniqueId(), currentHealth + actualHeal);

                        // Track accumulated regen percentage to consume food (10 points per 1% healed)
                        double accumulated = accumulatedRegenPercent.getOrDefault(player.getUniqueId(), 0.0) + healedPercent;
                        if (accumulated >= 0.01) {
                            MCIdentityManager.updateFood(player.getUniqueId(), Math.max(0, currentFood - 10));
                            accumulated -= 0.01;
                        }
                        accumulatedRegenPercent.put(player.getUniqueId(), accumulated);
                        lastRegenTime.put(player.getUniqueId(), currentTime);
                    }
                }
            }
        }, null);
    }
}

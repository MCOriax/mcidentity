package io.gitlab.mcoriax.papermc.listeners;

import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.papermc.MCIdentityPlugin;
import io.gitlab.mcoriax.papermc.util.ExperienceManager;
import io.papermc.paper.threadedregionscheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MCIdentityStatsListener implements Listener {

    /**
     * The plugin instance.
     */
    private final MCIdentityPlugin plugin;
    /**
     * The task that updates player visuals at a fixed rate.
     */
    private ScheduledTask visualTask;
    /**
     * Tracks accumulated health regeneration percentage per player for food consumption.
     */
    private final Map<UUID, Double> accumulatedRegenPercent = new ConcurrentHashMap<>();
    /**
     * Tracks the last time a player received a health regeneration tick.
     */
    private final Map<UUID, Long> lastRegenTime = new ConcurrentHashMap<>();

    /**
     * Creates a new MCIdentityStatsListener.
     * @param plugin The plugin instance.
     */
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
        ((ExperienceManager) MCIdentityManager.getExperienceManager()).awardExp(player, "receive_damage", damage);
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

    /**
     * Calculates experience gained and updates the identity level if necessary.
     * @param player The player.
     * @param session The active identity session.
     * @param expGained The amount of experience gained.
     */
    private void calculateExpAndLevel(Player player, ActiveIdentitySession session, int expGained) {
        ((ExperienceManager) MCIdentityManager.getExperienceManager()).calculateExpAndLevel(player, session, expGained);
    }

    /**
     * Starts the periodic visual update task.
     */
    private void startVisualTask() {
        visualTask = Bukkit.getServer().getAsyncScheduler().runAtFixedRate(plugin, st -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                MCIdentityManager.getSession(player.getUniqueId()).ifPresent(session -> {
                    updateVisuals(player, session);
                });
            }
        }, 500L, 500L, TimeUnit.MILLISECONDS);
    }

    /**
     * Cancels any active visual update tasks.
     */
    public void cancelTasks() {
        if (visualTask != null) {
            visualTask.cancel();
        }
    }

    /**
     * Restores the player's vanilla visual state.
     * @param player The player.
     */
    public void restoreVisuals(Player player) {
        player.getScheduler().run(plugin, st -> {
            player.displayName(Component.text(player.getName()));
            player.playerListName(Component.text(player.getName()));
            player.sendActionBar(Component.empty());
        }, null);
    }

    /**
     * Updates the player's custom identity visuals (Action Bar, Name Tag, Health/Hunger Bars).
     * @param player The player.
     * @param session The active identity session.
     */
    public void updateVisuals(Player player, ActiveIdentitySession session) {
        // 1. Action Bar
        double hpPercent = (session.currentHealth() / session.stats().getHealth()) * 100.0;
        double energyPercent = (session.currentEnergy() / session.stats().getEnergy()) * 100.0;
        
        // Calculate food percentage relative to identity's food capacity
        double maxFood = session.stats().getFood();
        double currentFood = session.currentFood();
        double foodPercent = (currentFood / maxFood) * 100.0;
        
        String professionName = MCIdentityManager.getProfessionDisplayName(session.identityKey().getProfessionCode());
        
        int currentLevel = session.properties().getLevel();
        int maxLevel = MCIdentityManager.getProfessionLevelConfig(session.identityKey().getProfessionCode().toLowerCase()).maxLevel();
        String levelDisplayString = (currentLevel >= maxLevel) ? "MAX" : String.valueOf(currentLevel);

        String healthDisplay = String.format("%.0f%%", hpPercent);
        String foodDisplay = String.format("%.0f%%", foodPercent);
        String energyDisplay = String.format("%.0f%%", energyPercent);
        Component actionBar = Component.translatable("mcidentity.action_bar.level", levelDisplayString, Component.text(levelDisplayString)).color(NamedTextColor.GREEN)
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(Component.translatable("mcidentity.action_bar.profession", "[" + professionName + "]", Component.text(professionName)).color(NamedTextColor.WHITE))
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(Component.translatable("mcidentity.action_bar.health", "HP: " + healthDisplay, Component.text(healthDisplay)).color(NamedTextColor.RED))
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(Component.translatable("mcidentity.action_bar.food", "Food: " + foodDisplay, Component.text(foodDisplay)).color(NamedTextColor.GOLD))
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(Component.translatable("mcidentity.action_bar.energy", "Energy: " + energyDisplay, Component.text(energyDisplay)).color(NamedTextColor.BLUE));
        player.sendActionBar(actionBar);

        // 2. Name Tag & Tab List
        Component nameTag = Component.text("[", NamedTextColor.GRAY)
                .append(Component.text(professionName, NamedTextColor.WHITE))
                .append(Component.text("] ", NamedTextColor.GRAY))
                .append(Component.text(player.getName()));
        
        if (!player.displayName().equals(nameTag)) {
            player.displayName(nameTag);
        }
        if (!player.playerListName().equals(nameTag)) {
            player.playerListName(nameTag);
        }

        // 3. Scale Health Bar and Hunger Bar
        // Must run on main thread
        player.getScheduler().run(plugin, st -> {
            if (player.isDead()) {
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
        });
    }
}

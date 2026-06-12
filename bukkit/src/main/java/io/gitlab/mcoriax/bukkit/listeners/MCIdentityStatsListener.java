package io.gitlab.mcoriax.bukkit.listeners;

import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.common.MCIdentityManager;
import io.gitlab.mcoriax.common.MCIdentityProvider;
import io.gitlab.mcoriax.bukkit.scheduler.PlatformTask;
import io.gitlab.mcoriax.bukkit.scheduler.Schedulers;
import org.bukkit.plugin.java.JavaPlugin;
import io.gitlab.mcoriax.bukkit.util.ExperienceManager;
import io.gitlab.mcoriax.bukkit.util.MessageComponents;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
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

public class MCIdentityStatsListener implements Listener {

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;
    /**
     * The task that updates player visuals at a fixed rate.
     */
    private PlatformTask visualTask;
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
    public MCIdentityStatsListener(JavaPlugin plugin) {
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
        visualTask = Schedulers.runAsyncAtFixedRate(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                MCIdentityManager.getSession(player.getUniqueId()).ifPresent(session -> {
                    updateVisuals(player, session);
                });
            }
        }, 500L, 500L);
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
        Schedulers.runForEntity(player, () -> {
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(""));
        });
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
        String sep = ChatColor.GRAY + " | ";
        String actionBar = ChatColor.GREEN + MessageComponents.value("mcidentity.action_bar.level.prefix", "", levelDisplayString, "mcidentity.action_bar.level.suffix", "")
                + sep
                + ChatColor.WHITE + MessageComponents.value("mcidentity.action_bar.profession.prefix", "[", professionName, "mcidentity.action_bar.profession.suffix", "]")
                + sep
                + ChatColor.RED + MessageComponents.value("mcidentity.action_bar.health.prefix", "HP: ", healthDisplay, "mcidentity.action_bar.health.suffix", "")
                + sep
                + ChatColor.GOLD + MessageComponents.value("mcidentity.action_bar.food.prefix", "Food: ", foodDisplay, "mcidentity.action_bar.food.suffix", "")
                + sep
                + ChatColor.BLUE + MessageComponents.value("mcidentity.action_bar.energy.prefix", "Energy: ", energyDisplay, "mcidentity.action_bar.energy.suffix", "");
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionBar));

        // 2. Name Tag & Tab List
        String nameTag = ChatColor.GRAY + "[" + ChatColor.WHITE + professionName + ChatColor.GRAY + "] " + ChatColor.RESET + player.getName();

        // 3. Scale Health Bar and Hunger Bar
        // Must run on main thread
        Schedulers.runForEntity(player, () -> {
            if (player.isDead()) {
                return;
            }

            if (!nameTag.equals(player.getDisplayName())) {
                player.setDisplayName(nameTag);
            }
            if (!nameTag.equals(player.getPlayerListName())) {
                player.setPlayerListName(nameTag);
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
                    double maxHealthParam = 20.0;
                    if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
                        maxHealthParam = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                    }
                    
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

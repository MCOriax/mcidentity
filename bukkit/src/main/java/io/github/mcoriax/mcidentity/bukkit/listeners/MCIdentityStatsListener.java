package io.github.mcoriax.mcidentity.bukkit.listeners;

import io.github.mcoriax.mcidentity.api.model.*;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;
import io.github.mcoriax.mcidentity.bukkit.scheduler.PlatformTask;
import io.github.mcoriax.mcidentity.bukkit.scheduler.Schedulers;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.mcoriax.mcidentity.bukkit.util.ExpAction;
import io.github.mcoriax.mcidentity.bukkit.util.ExperienceManager;
import io.github.mcoriax.mcidentity.bukkit.util.MessageComponents;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
        ((ExperienceManager) MCIdentityManager.getExperienceManager()).awardExp(player, ExpAction.RECEIVE_DAMAGE, damage);
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
        // Percentages are clamped to [0, 100] and guard against a zero max, so
        // the action bar never renders NaN, a negative value, or above 100%.
        double hpPercent = percent(session.currentHealth(), session.stats().getHealth());
        double energyPercent = percent(session.currentEnergy(), session.stats().getEnergy());
        double foodPercent = percent(session.currentFood(), session.stats().getFood());

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

            // Re-read the live session on the main thread. updateVisuals is
            // often scheduled from an async tick, and a profession swap or a
            // death drop can replace the session in between. Writing the values
            // captured at method entry back here would corrupt the freshly
            // loaded session — leaving current health/food above the new max
            // (>100%) or stuck at the fatal-blow 0%. Every write below is based
            // on the live session and clamped to its current maximums.
            ActiveIdentitySession live = MCIdentityManager.getSession(player.getUniqueId()).orElse(null);
            if (live == null) {
                return;
            }

            double liveMaxHealth = live.stats().getHealth();
            double liveMaxFood = live.stats().getFood();
            double liveCurrentHealth = clamp(live.currentHealth(), 0.0, liveMaxHealth);
            double liveCurrentFood = clamp(live.currentFood(), 0.0, liveMaxFood);
            double liveHpPercent = percent(liveCurrentHealth, liveMaxHealth);
            double liveFoodPercent = percent(liveCurrentFood, liveMaxFood);

            // Health Hijack Fix
            if (liveCurrentHealth > 0) {
                double vanillaHealth = (liveHpPercent / 100.0) * 20.0;
                player.setHealth(Math.max(0.1, Math.min(20.0, vanillaHealth)));
            }

            // Hunger Hijack Fix
            double vanillaFood = (liveFoodPercent / 100.0) * 20.0;
            player.setFoodLevel((int) Math.max(0, Math.min(20, vanillaFood)));

            // Custom Health Regeneration: heal 1% of maximum health every 5
            // seconds. This is independent of food — food is only the hunger
            // bar now and is never consumed to heal.
            long currentTime = System.currentTimeMillis();
            long lastRegen = lastRegenTime.getOrDefault(player.getUniqueId(), 0L);

            if (currentTime - lastRegen >= 5000) {
                if (liveCurrentHealth < liveMaxHealth) {
                    double newHealth = clamp(liveCurrentHealth + liveMaxHealth * 0.01, 0.0, liveMaxHealth);
                    MCIdentityManager.updateHealth(player.getUniqueId(), newHealth);
                }
                lastRegenTime.put(player.getUniqueId(), currentTime);
            }
        });
    }

    /**
     * Computes {@code current / max} as a percentage clamped to [0, 100].
     * Returns 0 when the maximum is not positive so the result is never NaN,
     * infinite, negative, or above 100%.
     * @param current The current value.
     * @param max The maximum value.
     * @return The clamped percentage.
     */
    private static double percent(double current, double max) {
        if (max <= 0) {
            return 0.0;
        }
        return clamp((current / max) * 100.0, 0.0, 100.0);
    }

    /**
     * Clamps a value into the inclusive range [min, max].
     * @param value The value to clamp.
     * @param min The lower bound.
     * @param max The upper bound.
     * @return The clamped value.
     */
    private static double clamp(double value, double min, double max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}

package io.github.mcoriax.bukkit.listeners;

import io.github.mcoriax.bukkit.util.ExpAction;
import io.github.mcoriax.bukkit.util.ExperienceManager;
import io.github.mcoriax.common.MCIdentityManager;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles interaction and exploration related EXP actions:
 * {@code tame_animal}, {@code breed_animal}, {@code explore_biome} and {@code heal_target}.
 */
public class InteractionExpListener implements Listener {

    /**
     * The plugin instance.
     */
    private final JavaPlugin plugin;
    /**
     * Tracks biomes already rewarded per player for the current server session.
     */
    private final Map<UUID, Set<String>> visitedBiomes = new ConcurrentHashMap<>();

    /**
     * Creates a new InteractionExpListener.
     * @param plugin The plugin instance.
     */
    public InteractionExpListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Awards EXP when a player tames an animal.
     * @param event The entity tame event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityTame(EntityTameEvent event) {
        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        if (event.getOwner() instanceof Player player) {
            exp.awardExp(player, ExpAction.TAME_ANIMAL, event.getEntity().getType().name().toLowerCase());
        }
    }

    /**
     * Awards EXP when a player breeds two animals.
     * @param event The entity breed event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent event) {
        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        if (event.getBreeder() instanceof Player player) {
            exp.awardExp(player, ExpAction.BREED_ANIMAL, event.getEntity().getType().name().toLowerCase());
        }
    }

    /**
     * Awards EXP when a player heals another entity with a splash potion.
     * @param event The potion splash event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        ThrownPotion potion = event.getPotion();
        ProjectileSource source = potion.getShooter();
        if (!(source instanceof Player player) || !containsHealingEffect(potion)) {
            return;
        }

        // Only reward if at least one entity other than the thrower is affected.
        boolean healedOther = false;
        for (LivingEntity affected : event.getAffectedEntities()) {
            if (!affected.getUniqueId().equals(player.getUniqueId())) {
                healedOther = true;
                break;
            }
        }

        if (healedOther) {
            exp.awardExp(player, ExpAction.HEAL_TARGET);
        }
    }

    /**
     * Awards EXP the first time a player enters a given biome during this session.
     * @param event The player move event.
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        // Cheap short-circuit: only react when the player crosses a block boundary.
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (MCIdentityManager.getSession(player.getUniqueId()).isEmpty()) {
            return;
        }

        ExperienceManager exp = ExperienceManager.get();
        if (exp == null) {
            return;
        }

        String biome = biomeName(to);
        Set<String> visited = visitedBiomes.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.<String>newKeySet());
        if (visited.add(biome)) {
            exp.awardExp(player, ExpAction.EXPLORE_BIOME, biome);
        }
    }

    /**
     * Clears tracked biome state when a player leaves the server.
     * @param event The player quit event.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        visitedBiomes.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Determines whether a thrown potion carries a healing or regeneration effect.
     * @param potion The thrown potion.
     * @return true if the potion can heal entities.
     */
    private boolean containsHealingEffect(ThrownPotion potion) {
        for (PotionEffect effect : potion.getEffects()) {
            String key = effect.getType().getKey().getKey().toLowerCase();
            if (key.contains("heal") || key.equals("regeneration")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves the namespaced biome name for a location.
     * @param location The location.
     * @return The lowercase biome key (e.g. "plains").
     */
    private String biomeName(Location location) {
        Biome biome = location.getBlock().getBiome();
        return biome.getKey().getKey().toLowerCase();
    }
}

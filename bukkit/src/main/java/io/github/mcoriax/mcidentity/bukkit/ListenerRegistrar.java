package io.github.mcoriax.mcidentity.bukkit;

import io.github.mcoriax.mcidentity.bukkit.commands.MCIdentityCommand;
import io.github.mcoriax.mcidentity.bukkit.listeners.AdvancedExplorationExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.BuildingAndSurvivalExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.CombatExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.CraftingExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.DimensionsExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.EngineeringExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.ExplorationAndTravelExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.FarmingAndRanchingExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.GatheringExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.IdentityAliasNameListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.InteractionAndArtExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.InteractionExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.MCIdentityInventoryListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.MCIdentityListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.MCIdentityStatsListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.MagicAndEconomyExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.PlayerRespawnListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.RanchingAndWildlifeExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.SpecialEventsExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.SpecialtyFarmingExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.SupportExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.SurvivalExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.UtilityAndCombatExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.WorkstationExpListener;
import io.github.mcoriax.mcidentity.bukkit.listeners.WorldInteractionExpListener;
import io.github.mcoriax.mcidentity.bukkit.tabcompleter.MCIdentityTabCompleter;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Wires the {@code /identity} command and every event listener on enable.
 *
 * <p><strong>Internal.</strong> Package-private, called once by
 * {@link AbstractMCIdentityPlugin}.</p>
 */
final class ListenerRegistrar {

    private ListenerRegistrar() {
    }

    /**
     * Sets the {@code /identity} executor and tab completer.
     * @param plugin The plugin declaring the command.
     */
    static void registerCommand(JavaPlugin plugin) {
        plugin.getCommand("identity").setExecutor(new MCIdentityCommand(plugin));
        plugin.getCommand("identity").setTabCompleter(new MCIdentityTabCompleter());
    }

    /**
     * Registers the identity, inventory, stats, respawn and alias listeners,
     * then the EXP listeners grouped by category.
     * @param plugin The plugin the listeners belong to.
     */
    static void registerListeners(JavaPlugin plugin) {
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        pluginManager.registerEvents(new MCIdentityListener(plugin), plugin);
        pluginManager.registerEvents(new MCIdentityInventoryListener(plugin), plugin);
        MCIdentityManager.setStatsListener(new MCIdentityStatsListener(plugin));
        pluginManager.registerEvents((MCIdentityStatsListener) MCIdentityManager.getStatsListener(), plugin);
        pluginManager.registerEvents(new PlayerRespawnListener(plugin), plugin);
        pluginManager.registerEvents(new IdentityAliasNameListener(plugin), plugin);

        // Modular EXP gain listeners grouped by category.
        for (Listener listener : expListeners(plugin)) {
            pluginManager.registerEvents(listener, plugin);
        }
    }

    /**
     * Builds the EXP listeners, in registration order.
     * @param plugin The plugin the listeners belong to.
     * @return The listeners.
     */
    private static List<Listener> expListeners(JavaPlugin plugin) {
        return List.of(
            new CombatExpListener(plugin),
            new GatheringExpListener(plugin),
            new CraftingExpListener(plugin),
            new InteractionExpListener(plugin),
            new MagicAndEconomyExpListener(plugin),
            new BuildingAndSurvivalExpListener(plugin),
            new AdvancedExplorationExpListener(plugin),
            new SupportExpListener(plugin),
            new FarmingAndRanchingExpListener(plugin),
            new ExplorationAndTravelExpListener(plugin),
            new InteractionAndArtExpListener(plugin),
            new UtilityAndCombatExpListener(plugin),
            new EngineeringExpListener(plugin),
            new DimensionsExpListener(plugin),
            new SurvivalExpListener(plugin),
            new SpecialEventsExpListener(plugin),
            new SpecialtyFarmingExpListener(plugin),
            new RanchingAndWildlifeExpListener(plugin),
            new WorkstationExpListener(plugin),
            new WorldInteractionExpListener(plugin));
    }
}

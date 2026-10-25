package io.github.mcoriax.mcidentity.bukkit.config;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Reads MCAgents' own {@code agents.model} and {@code agents.token} from the
 * running MCAgents plugin, by reflection.
 *
 * <p>This plugin is not compiled against MCAgents, so it calls
 * {@code getAgentsModel()} and {@code getAgentsToken()} — the public getters
 * MCAgents 6.2.0 added on its plugin class — by name. Each call looks MCAgents
 * up again, so the answer follows MCAgents enabling, disabling, or reloading
 * after this plugin. An MCAgents older than 6.2.0 answers through its
 * provider's {@code getDefaultModel()} and {@code getServerApiKey()}, which hold
 * the same values; without MCAgents both answer blank.</p>
 */
public final class MCAgentsGlobal implements AgentsSettings.Global {

    /**
     * The plugin name to look for on the running server.
     */
    public static final String PLUGIN_NAME = "MCAgents";

    /**
     * {@inheritDoc}
     */
    @Override
    public String model() {
        return call("getAgentsModel", "getDefaultModel");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String token() {
        return call("getAgentsToken", "getServerApiKey");
    }

    /**
     * Calls one no-argument getter on the running MCAgents plugin, or the
     * matching getter on its provider when the plugin predates it.
     *
     * @param method   The plugin getter's name.
     * @param fallback The provider getter's name.
     * @return The value as text, blank when MCAgents or both getters are missing.
     */
    private static String call(String method, String fallback) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (plugin == null || !plugin.isEnabled()) {
            return "";
        }
        try {
            return text(plugin.getClass().getMethod(method).invoke(plugin));
        } catch (NoSuchMethodException e) {
            try {
                Object provider = plugin.getClass().getMethod("getProvider").invoke(plugin);
                return (provider == null) ? "" : text(provider.getClass().getMethod(fallback).invoke(provider));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError inner) {
                return "";
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return "";
        }
    }

    /**
     * Turns a getter's result into text.
     *
     * @param value The result.
     * @return The text, blank for null.
     */
    private static String text(Object value) {
        return (value == null) ? "" : value.toString();
    }
}

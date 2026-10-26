package io.github.mcoriax.mcidentity.bukkit.config;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

/**
 * Reads MCAgents' own {@code agents} block from the running MCAgents plugin,
 * by reflection.
 *
 * <p>This plugin is not compiled against MCAgents. A modality's model is read,
 * first match wins, from:</p>
 * <ol>
 *   <li>a public getter named for it on MCAgents' plugin class, such as
 *       {@code getAgentsText()} or {@code getAgentsEmbeddings()}, for an
 *       MCAgents that exposes one;</li>
 *   <li>{@code agents.{modality}} in MCAgents' own {@code config.yml}, read
 *       through Bukkit's {@code Plugin#getConfig()};</li>
 *   <li>for {@code text} only, {@code getAgentsModel()} (MCAgents 6.2.0) or the
 *       provider's {@code getDefaultModel()} on older builds.</li>
 * </ol>
 *
 * <p>The key comes from {@code getAgentsToken()}, or the provider's
 * {@code getServerApiKey()} on MCAgents older than 6.2.0. Each call looks
 * MCAgents up again, so the answer follows MCAgents enabling, disabling, or
 * reloading after this plugin; without MCAgents everything answers blank.</p>
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
    public String model(AgentsSettings.Modality modality) {
        Plugin plugin = mcagents();
        if (plugin == null) {
            return "";
        }
        String key = modality.key();
        String getter = "getAgents" + key.substring(0, 1).toUpperCase(Locale.ROOT) + key.substring(1);
        String value = invoke(plugin, getter);
        if (value == null) {
            value = configured(plugin, "agents." + key);
        }
        if ((value == null || value.isBlank()) && modality == AgentsSettings.Modality.TEXT) {
            value = call(plugin, "getAgentsModel", "getDefaultModel");
        }
        return (value == null) ? "" : value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String token() {
        Plugin plugin = mcagents();
        return (plugin == null) ? "" : call(plugin, "getAgentsToken", "getServerApiKey");
    }

    /**
     * Finds the running MCAgents.
     *
     * @return The plugin, or null when it is missing or disabled.
     */
    private static Plugin mcagents() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        return (plugin == null || !plugin.isEnabled()) ? null : plugin;
    }

    /**
     * Reads a value from MCAgents' own {@code config.yml}.
     *
     * @param plugin The MCAgents plugin.
     * @param path   The configuration path.
     * @return The value, or null when it is not set.
     */
    private static String configured(Plugin plugin, String path) {
        try {
            String value = plugin.getConfig().getString(path);
            return (value == null || value.isBlank()) ? null : value;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Calls one no-argument getter on the running MCAgents plugin, or the
     * matching getter on its provider when the plugin predates it.
     *
     * @param plugin   The MCAgents plugin.
     * @param method   The plugin getter's name.
     * @param fallback The provider getter's name.
     * @return The value as text, blank when both getters are missing.
     */
    private static String call(Plugin plugin, String method, String fallback) {
        String value = invoke(plugin, method);
        if (value != null) {
            return value;
        }
        try {
            Object provider = plugin.getClass().getMethod("getProvider").invoke(plugin);
            String answer = (provider == null) ? null : invoke(provider, fallback);
            return (answer == null) ? "" : answer;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return "";
        }
    }

    /**
     * Calls a public no-argument method by name.
     *
     * @param target The object.
     * @param method The method name.
     * @return The result as text, blank for null, or null when there is no
     *         such method or the call failed.
     */
    private static String invoke(Object target, String method) {
        try {
            Object value = target.getClass().getMethod(method).invoke(target);
            return (value == null) ? "" : value.toString();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }
}

package io.github.mcoriax.mcidentity.bukkit.bridge.mcagents;

import io.github.mcoriax.mcidentity.tools.MCAgentsToolAdapter;
import io.github.mcoriax.mcidentity.tools.MyIdentityStatsTool;
import io.github.mcoriax.mcidentity.tools.MyIdentityTool;
import io.github.mcoriax.mcidentity.tools.PluginTool;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Registers the MCIdentity tools with MCAgents when it is installed.
 *
 * <p>MCIdentity has no compile dependency on MCAgents. The registrar finds the
 * enabled {@value #MCAGENTS_PLUGIN} plugin, asks it for its provider through
 * {@code getProvider()}, loads the {@code AITool} interface from its class
 * loader, and calls {@code registerTool(AITool)} once per tool proxy. MCAgents
 * is a soft dependency, so it enables first when present.</p>
 *
 * <p>Safe degradation: without MCAgents one info line is logged; any reflective
 * failure logs one warning; MCIdentity carries on either way.</p>
 */
public final class MCAgentsToolRegistrar {

    /**
     * The plugin name MCAgents registers under.
     */
    public static final String MCAGENTS_PLUGIN = "MCAgents";

    /**
     * Prevents instantiation: this class holds a static entry point only.
     */
    private MCAgentsToolRegistrar() {
    }

    /**
     * Registers the built-in identity tools with MCAgents, if it is enabled.
     * @param plugin The MCIdentity plugin.
     */
    public static void registerAll(Plugin plugin) {
        BukkitIdentityToolPort port = new BukkitIdentityToolPort(plugin);
        register(plugin, List.of(new MyIdentityTool(port), new MyIdentityStatsTool(port)));
    }

    /**
     * Registers the given tools with MCAgents, if it is enabled.
     * @param plugin The MCIdentity plugin.
     * @param tools The tools to register.
     */
    public static void register(Plugin plugin, List<PluginTool> tools) {
        Plugin agents = plugin.getServer().getPluginManager().getPlugin(MCAGENTS_PLUGIN);
        if (agents == null || !agents.isEnabled()) {
            plugin.getLogger().info(MCAGENTS_PLUGIN + " not found; its AI tools were not registered.");
            return;
        }
        try {
            Object provider = agents.getClass().getMethod("getProvider").invoke(agents);
            if (provider == null) {
                throw new IllegalStateException(MCAGENTS_PLUGIN + " returned no provider");
            }
            Class<?> toolInterface = MCAgentsToolAdapter.loadToolInterface(agents.getClass().getClassLoader());
            Method registerTool = provider.getClass().getMethod("registerTool", toolInterface);
            for (PluginTool tool : tools) {
                registerTool.invoke(provider, MCAgentsToolAdapter.create(toolInterface, tool));
            }
            plugin.getLogger().info("Registered " + tools.size() + " " + MCAGENTS_PLUGIN + " tools: "
                + tools.stream().map(PluginTool::getName).collect(Collectors.joining(", ")));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            Throwable cause = e instanceof InvocationTargetException wrapped && wrapped.getCause() != null
                ? wrapped.getCause() : e;
            plugin.getLogger().warning("Could not register the " + MCAGENTS_PLUGIN + " tools ("
                + cause.getClass().getSimpleName() + ": " + cause.getMessage() + "); continuing without them.");
        }
    }
}

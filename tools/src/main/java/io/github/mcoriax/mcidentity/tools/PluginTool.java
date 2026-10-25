package io.github.mcoriax.mcidentity.tools;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * A tool an AI can call, mirroring the MCAgents {@code AITool} contract.
 *
 * <p>MCIdentity does not compile against MCAgents. A {@code PluginTool} is
 * exposed to MCAgents through {@link MCAgentsToolAdapter}, which wraps it in a
 * proxy implementing {@code io.github.mcpaimon.mcagents.tools.AITool} and turns
 * the MCAgents account into a {@link ToolCaller}.</p>
 */
public interface PluginTool {

    /**
     * Gets the unique snake_case name the AI calls the tool by.
     * @return The tool name.
     */
    String getName();

    /**
     * Gets what the tool does, written for the AI model.
     * @return The tool description.
     */
    String getDescription();

    /**
     * Gets the JSON schema of the arguments the tool accepts.
     * @return A valid JSON schema string.
     */
    String getParametersJsonSchema();

    /**
     * Gets the MCAgents category ids the tool belongs to.
     * @return The category ids.
     */
    List<String> getCategories();

    /**
     * Runs the tool.
     * @param arguments The arguments the AI passed; never null.
     * @param caller The account the AI is acting for; never null.
     * @return A future holding the text sent back to the AI.
     */
    CompletableFuture<String> execute(Map<String, Object> arguments, ToolCaller caller);
}

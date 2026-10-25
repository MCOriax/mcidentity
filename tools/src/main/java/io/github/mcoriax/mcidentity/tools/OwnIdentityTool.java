package io.github.mcoriax.mcidentity.tools;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Base for the tools that take no arguments and answer about the calling
 * player's own active identity. Subclasses only supply their name, description,
 * and how an identity is formatted for the AI.
 */
abstract class OwnIdentityTool implements PluginTool {

    /**
     * The schema of a tool that takes no arguments.
     */
    private static final String NO_ARGUMENTS_SCHEMA =
        "{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}";

    /**
     * The categories every identity tool is listed under.
     */
    private static final List<String> CATEGORIES = List.of("identity", "player_info");

    /**
     * The server port the identity is read through.
     */
    private final IdentityToolPort port;

    /**
     * The name the AI calls this tool by.
     */
    private final String name;

    /**
     * What the tool does, as shown to the AI.
     */
    private final String description;

    /**
     * Turns the caller's identity into the answer text.
     */
    private final Function<IdentitySnapshot, String> formatter;

    /**
     * Creates the tool.
     * @param port        The server port the identity is read through.
     * @param name        The name the AI calls this tool by.
     * @param description What the tool does, as shown to the AI.
     * @param formatter   Turns the caller's identity into the answer text.
     */
    OwnIdentityTool(IdentityToolPort port, String name, String description,
                    Function<IdentitySnapshot, String> formatter) {
        this.port = Objects.requireNonNull(port, "port");
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final String getDescription() {
        return description;
    }

    @Override
    public final String getParametersJsonSchema() {
        return NO_ARGUMENTS_SCHEMA;
    }

    @Override
    public final List<String> getCategories() {
        return CATEGORIES;
    }

    @Override
    public final CompletableFuture<String> execute(Map<String, Object> arguments, ToolCaller caller) {
        return PlayerToolSupport.withOwnIdentity(port, caller, formatter);
    }
}

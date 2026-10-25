package io.github.mcoriax.mcidentity.tools;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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
     * Creates the tool.
     * @param port The server port the identity is read through.
     */
    OwnIdentityTool(IdentityToolPort port) {
        this.port = Objects.requireNonNull(port, "port");
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
        return PlayerToolSupport.withOwnIdentity(port, caller, this::answer);
    }

    /**
     * Formats the caller's identity for the AI.
     * @param identity The caller's active identity.
     * @return The answer text.
     */
    abstract String answer(IdentitySnapshot identity);
}

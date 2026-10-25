package io.github.mcoriax.mcidentity.tools;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Reports the stats of the calling player's active identity, with its live
 * health, energy, and food. Read-only, and limited to the caller's own data.
 */
public final class MyIdentityStatsTool implements PluginTool {

    /**
     * The name the AI calls this tool by.
     */
    public static final String NAME = "mcidentity_get_my_stats";

    /**
     * The server port the identity is read through.
     */
    private final IdentityToolPort port;

    /**
     * Creates the tool.
     * @param port The server port the identity is read through.
     */
    public MyIdentityStatsTool(IdentityToolPort port) {
        this.port = Objects.requireNonNull(port, "port");
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Get the stats of the calling player's own active MCIdentity identity (health, energy, "
            + "strength, defense, wisdom, luck, agility, resilience, food), its live health, energy and "
            + "food, and its unspent stat points. Takes no arguments and only ever reads the caller's own data.";
    }

    @Override
    public String getParametersJsonSchema() {
        return "{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}";
    }

    @Override
    public List<String> getCategories() {
        return List.of("identity", "player_info");
    }

    @Override
    public CompletableFuture<String> execute(Map<String, Object> arguments, ToolCaller caller) {
        return PlayerToolSupport.withOwnIdentity(port, caller, MyIdentityStatsTool::format);
    }

    /**
     * Formats an identity's stats for the AI.
     * @param identity The identity.
     * @return The answer text.
     */
    static String format(IdentitySnapshot identity) {
        StringBuilder out = new StringBuilder("Stats of the active ")
            .append(identity.professionName()).append(" identity (level ").append(identity.level()).append("):\n");
        identity.stats().forEach((name, value) ->
            out.append("- ").append(name).append(": ").append(value).append('\n'));
        out.append("- Current health: ").append(PlayerToolSupport.formatValue(identity.currentHealth())).append('\n');
        out.append("- Current energy: ").append(PlayerToolSupport.formatValue(identity.currentEnergy())).append('\n');
        out.append("- Current food: ").append(PlayerToolSupport.formatValue(identity.currentFood())).append('\n');
        out.append("- Unspent stat points: ").append(identity.statPoints());
        return out.toString();
    }
}

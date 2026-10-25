package io.github.mcoriax.mcidentity.tools;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Reports the calling player's active identity: profession, alias, level, EXP,
 * and unspent stat points. Read-only, and limited to the caller's own data.
 */
public final class MyIdentityTool implements PluginTool {

    /**
     * The name the AI calls this tool by.
     */
    public static final String NAME = "mcidentity_get_my_identity";

    /**
     * The server port the identity is read through.
     */
    private final IdentityToolPort port;

    /**
     * Creates the tool.
     * @param port The server port the identity is read through.
     */
    public MyIdentityTool(IdentityToolPort port) {
        this.port = Objects.requireNonNull(port, "port");
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Get the calling player's own active MCIdentity identity: its profession, alias, "
            + "level and level cap, EXP towards the next level, and unspent stat points. "
            + "Takes no arguments and only ever reads the caller's own data.";
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
        return PlayerToolSupport.withOwnIdentity(port, caller, MyIdentityTool::format);
    }

    /**
     * Formats an identity for the AI.
     * @param identity The identity.
     * @return The answer text.
     */
    static String format(IdentitySnapshot identity) {
        StringBuilder out = new StringBuilder("Active identity:\n");
        out.append("- Profession: ").append(identity.professionName())
            .append(" (").append(identity.professionCode()).append(")\n");
        out.append("- Alias: ").append(identity.alias() == null || identity.alias().isBlank()
            ? "none" : identity.alias()).append('\n');
        out.append("- Base identity: ").append(identity.baseIdentity() ? "yes" : "no").append('\n');
        out.append("- Level: ").append(identity.level())
            .append(" (cap: ").append(PlayerToolSupport.formatCap(identity.maxLevel())).append(")\n");
        out.append("- EXP: ").append(identity.exp()).append(" / ").append(identity.expPerLevel())
            .append(" towards the next level\n");
        out.append("- Unspent stat points: ").append(identity.statPoints());
        return out.toString();
    }
}

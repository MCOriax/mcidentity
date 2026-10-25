package io.github.mcoriax.mcidentity.tools;

/**
 * Reports the calling player's active identity: profession, alias, level, EXP,
 * and unspent stat points. Read-only, and limited to the caller's own data.
 */
public final class MyIdentityTool extends OwnIdentityTool {

    /**
     * The name the AI calls this tool by.
     */
    public static final String NAME = "mcidentity_get_my_identity";

    /**
     * What the tool does, as shown to the AI.
     */
    static final String DESCRIPTION = "Get the calling player's own active MCIdentity identity: its profession, alias, "
        + "level and level cap, EXP towards the next level, and unspent stat points. "
        + "Takes no arguments and only ever reads the caller's own data.";

    /**
     * Creates the tool.
     * @param port The server port the identity is read through.
     */
    public MyIdentityTool(IdentityToolPort port) {
        super(port, NAME, DESCRIPTION, MyIdentityTool::format);
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

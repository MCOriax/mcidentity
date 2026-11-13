package io.github.mcoriax.mcidentity.tools;

/**
 * Reports the calling player's stats, with their live health, energy, and
 * food. Read-only, and limited to the caller's own data.
 */
public final class MyIdentityStatsTool extends OwnIdentityTool {

    /**
     * The name the AI calls this tool by.
     */
    public static final String NAME = "mcidentity_get_my_stats";

    /**
     * What the tool does, as shown to the AI.
     */
    static final String DESCRIPTION = "Get the calling player's own MCIdentity stats (health, energy, "
        + "strength, defense, wisdom, luck, agility, resilience, food), their live health, energy and "
        + "food, and their unspent stat points. Takes no arguments and only ever reads the caller's own data.";

    /**
     * Creates the tool.
     * @param port The server port the stats are read through.
     */
    public MyIdentityStatsTool(IdentityToolPort port) {
        super(port, NAME, DESCRIPTION, MyIdentityStatsTool::format);
    }

    /**
     * Formats the player's stats for the AI.
     * @param identity The player's snapshot.
     * @return The answer text.
     */
    static String format(IdentitySnapshot identity) {
        StringBuilder out = new StringBuilder("Stats (wearing ")
            .append(identity.professionName()).append(", level ").append(identity.level()).append("):\n");
        identity.stats().forEach((name, value) ->
            out.append("- ").append(name).append(": ").append(value).append('\n'));
        out.append("- Current health: ").append(PlayerToolSupport.formatValue(identity.currentHealth())).append('\n');
        out.append("- Current energy: ").append(PlayerToolSupport.formatValue(identity.currentEnergy())).append('\n');
        out.append("- Current food: ").append(PlayerToolSupport.formatValue(identity.currentFood())).append('\n');
        out.append("- Unspent stat points: ").append(identity.statPoints());
        return out.toString();
    }
}

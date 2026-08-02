package io.github.mcoriax.mcidentity.bukkit.permission;

/**
 * Every permission node MCIdentity declares, in one place.
 *
 * <p>The nodes are namespaced under {@code mcoriax.mcidentity} so all of the
 * MCIdentity plugins sit under one prefix a server owner can grant or revoke as
 * a group. Each subcommand has its own node, an {@code .admin} parent grants
 * every administrative child at once, and {@code .*} grants everything.</p>
 *
 * <p>Player-facing nodes default to {@code true} in {@code plugin.yml}, so a
 * server that never touches permissions behaves exactly as it did before these
 * nodes existed. Administrative nodes default to operators only.</p>
 *
 * <p>Declared here rather than as string literals at each call site: a typo in a
 * literal fails open or closed silently, whereas a typo in a constant does not
 * compile.</p>
 */
public final class Permissions {

    /**
     * The prefix every node in this plugin shares.
     */
    public static final String ROOT = "mcoriax.mcidentity";

    /**
     * Grants every administrative command in this plugin.
     */
    public static final String ADMIN = ROOT + ".admin";

    /**
     * Grants every command in this plugin, player and administrative alike.
     */
    public static final String ALL = ROOT + ".*";

    /**
     * Permission for {@code command.use}. Granted to every player by default.
     */
    public static final String USE = ROOT + ".command.use";

    /**
     * Permission for {@code command.help}. Granted to every player by default.
     */
    public static final String HELP = ROOT + ".command.help";

    /**
     * Permission for {@code command.wiki}. Granted to every player by default.
     */
    public static final String WIKI = ROOT + ".command.wiki";

    /**
     * Permission for {@code command.check}. Granted to every player by default.
     */
    public static final String CHECK = ROOT + ".command.check";

    /**
     * Permission for {@code command.unuse}. Granted to every player by default.
     */
    public static final String UNUSE = ROOT + ".command.unuse";

    /**
     * Permission for {@code command.stats}. Granted to every player by default.
     */
    public static final String STATS = ROOT + ".command.stats";

    /**
     * Permission for {@code command.alias}. Granted to every player by default.
     */
    public static final String ALIAS = ROOT + ".command.alias";

    /**
     * Permission for {@code command.get}. Operators only by default.
     */
    public static final String GET = ROOT + ".command.get";

    /**
     * Permission for {@code command.base}. Operators only by default.
     */
    public static final String BASE = ROOT + ".command.base";

    private Permissions() {
    }
}

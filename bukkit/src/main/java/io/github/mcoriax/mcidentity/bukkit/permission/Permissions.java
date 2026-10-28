package io.github.mcoriax.mcidentity.bukkit.permission;

import org.bukkit.permissions.Permissible;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
     * Permission for {@code command.unequip}. Granted to every player by default.
     */
    public static final String UNEQUIP = ROOT + ".command.unequip";

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

    /**
     * The single administrative node this plugin checked before the nodes were
     * namespaced. Still honoured, and declared in {@code plugin.yml} as a parent
     * of {@link #ADMIN}, so a server that granted it keeps its admins.
     */
    public static final String LEGACY_ADMIN = "mcidentity.admin";

    /**
     * The nodes that default to operators only, each also granted by
     * {@link #ADMIN} and {@link #LEGACY_ADMIN}.
     */
    private static final Set<String> ADMINISTRATIVE = Set.of(GET, BASE);

    /**
     * Every {@code /identity} sub-command, in help order, mapped to the node it
     * requires. {@code /identity} with no sub-command requires {@link #USE}.
     */
    public static final Map<String, String> SUB_COMMANDS = subCommands();

    private Permissions() {
    }

    /**
     * Builds the sub-command table.
     * @return An unmodifiable, insertion-ordered map of sub-command to node.
     */
    private static Map<String, String> subCommands() {
        Map<String, String> nodes = new LinkedHashMap<>();
        nodes.put("help", HELP);
        nodes.put("wiki", WIKI);
        nodes.put("check", CHECK);
        nodes.put("unequip", UNEQUIP);
        nodes.put("stats", STATS);
        nodes.put("alias", ALIAS);
        nodes.put("get", GET);
        nodes.put("base", BASE);
        return Collections.unmodifiableMap(nodes);
    }

    /**
     * Reports whether a sender may use the command a node guards.
     *
     * <p>An administrative node is also granted by the {@link #ADMIN} parent and
     * the {@link #LEGACY_ADMIN} node, checked directly so a permissions plugin
     * that does not expand {@code plugin.yml} children still lets them through.</p>
     * @param subject The sender being checked.
     * @param node The node the command requires.
     * @return true when the sender holds the node.
     */
    public static boolean grants(Permissible subject, String node) {
        if (subject.hasPermission(node)) {
            return true;
        }
        return ADMINISTRATIVE.contains(node)
                && (subject.hasPermission(ADMIN) || subject.hasPermission(LEGACY_ADMIN));
    }

    /**
     * Reports whether a sender holds any administrative node.
     * @param subject The sender being checked.
     * @return true when at least one administrative command is open to the sender.
     */
    public static boolean grantsAnyAdministrative(Permissible subject) {
        return ADMINISTRATIVE.stream().anyMatch(node -> grants(subject, node));
    }
}

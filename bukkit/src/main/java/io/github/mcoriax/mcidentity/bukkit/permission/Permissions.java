package io.github.mcoriax.mcidentity.bukkit.permission;

import io.github.mcoriax.mcidentity.common.BaseProfession;
import org.bukkit.permissions.Permissible;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every permission node MCIdentity declares, in one place.
 *
 * <p>The nodes are namespaced under {@code mcoriax.mcidentity} so all of the
 * MCIdentity plugins sit under one prefix a server owner can grant or revoke as
 * a group. Each subcommand has its own node, each profession has its own
 * wearing node, and {@code .*} grants everything.</p>
 *
 * <p>Every node defaults to {@code true} in {@code plugin.yml}, so a server
 * that never touches permissions lets every player use every command and wear
 * every profession.</p>
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
     * Grants every command and every profession in this plugin.
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
     * Permission for {@code command.stats}. Granted to every player by default.
     */
    public static final String STATS = ROOT + ".command.stats";

    /**
     * Permission for {@code command.wear}. Granted to every player by default.
     */
    public static final String WEAR = ROOT + ".command.wear";

    /**
     * Permission for {@code command.unwear}. Granted to every player by default.
     */
    public static final String UNWEAR = ROOT + ".command.unwear";

    /**
     * Prefix of the per profession wearing nodes,
     * {@code mcoriax.mcidentity.profession.<code>}.
     */
    private static final String PROFESSION_PREFIX = ROOT + ".profession.";

    /**
     * Grants wearing every profession. Granted to every player by default.
     */
    public static final String PROFESSION_ALL = PROFESSION_PREFIX + "*";

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
        nodes.put("stats", STATS);
        nodes.put("wear", WEAR);
        nodes.put("unwear", UNWEAR);
        return Collections.unmodifiableMap(nodes);
    }

    /**
     * Reports whether a sender may use the command a node guards.
     * @param subject The sender being checked.
     * @param node The node the command requires.
     * @return true when the sender holds the node.
     */
    public static boolean grants(Permissible subject, String node) {
        return subject.hasPermission(node);
    }

    /**
     * Reports whether a player may wear a profession.
     *
     * <p>The base profession is always allowed, since it is what a player wears
     * when they wear nothing. Any other profession needs
     * {@link #PROFESSION_ALL} or its own node,
     * {@code mcoriax.mcidentity.profession.<code>}.</p>
     * @param subject The player being checked.
     * @param professionCode The normalized profession code.
     * @return true when the player may wear it.
     */
    public static boolean canWear(Permissible subject, String professionCode) {
        if (BaseProfession.isBaseProfession(professionCode)) {
            return true;
        }
        return subject.hasPermission(PROFESSION_ALL)
                || subject.hasPermission(PROFESSION_PREFIX + professionCode);
    }
}

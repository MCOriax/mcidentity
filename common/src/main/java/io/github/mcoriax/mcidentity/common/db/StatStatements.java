package io.github.mcoriax.mcidentity.common.db;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * The finished per-stat statements of the relational backends.
 *
 * <p><strong>Internal.</strong> Package-private, used by {@link AbstractSqlDB}.</p>
 *
 * <p>A stat is a column, and a column cannot be bound as a prepared-statement
 * parameter, so its name has to reach the statement as text. Building every
 * statement here — once, at class-initialization time, from
 * {@link StatKeys#all()} — means the only strings ever concatenated into SQL
 * are this class's own literals and the allow-list's own literals. A caller's
 * stat key is used solely as a lookup key: an unknown key misses the map and
 * no statement is produced, so caller input cannot reach statement text even
 * if a guard elsewhere were removed.</p>
 */
final class StatStatements {

    /**
     * Reads one stat column, one finished statement per allow-listed stat key.
     */
    private static final Map<String, String> SELECT_STAT_SQL = statSqlByKey(
        column -> "SELECT " + column + " FROM identity_stats WHERE identity_uuid = ?");

    /**
     * Increments one stat column, built on the same terms as {@link #SELECT_STAT_SQL}.
     */
    private static final Map<String, String> ADD_STAT_SQL = statSqlByKey(
        column -> "UPDATE identity_stats SET " + column + " = " + column
            + " + ? WHERE identity_uuid = ?");

    private StatStatements() {
    }

    /**
     * Looks up the statement reading one stat.
     * @param statKey The stat key.
     * @return The statement, or null when the key is not allow-listed.
     */
    static String select(String statKey) {
        return SELECT_STAT_SQL.get(statKey);
    }

    /**
     * Looks up the statement incrementing one stat.
     * @param statKey The stat key.
     * @return The statement, or null when the key is not allow-listed.
     */
    static String add(String statKey) {
        return ADD_STAT_SQL.get(statKey);
    }

    /**
     * Renders one statement per allow-listed stat key.
     * @param template Produces the statement text for a single stat column.
     * @return An immutable map from stat key to its finished statement.
     */
    private static Map<String, String> statSqlByKey(UnaryOperator<String> template) {
        Map<String, String> byKey = new HashMap<>();
        for (String column : StatKeys.all()) {
            byKey.put(column, template.apply(column));
        }
        return Map.copyOf(byKey);
    }
}

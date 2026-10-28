package io.github.mcoriax.mcidentity.common.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Brings a relational database's schema up to date on start.
 *
 * <p><strong>Internal.</strong> Package-private, used by {@link AbstractSqlDB}.
 * Runs in three passes: drop the tables this plugin no longer owns, attempt
 * every additive migration, then run the dialect's schema DDL.</p>
 */
final class SqlSchema {

    /**
     * Logger for skipped migrations.
     */
    private static final Logger LOGGER = Logger.getLogger(SqlSchema.class.getName());

    /**
     * Schema statements dropping tables this plugin no longer owns.
     *
     * <p>Three systems have moved out of the core plugin into standalone
     * add-ons: skills to MCIdentitySkill, which stores them on the item rather
     * than in a table, and runes and cosmetics to MCIdentityRune and
     * MCIdentityCosmetic, which own their own databases. These tables are
     * therefore dropped on the first start after upgrading, and their rows are
     * <strong>not</strong> recoverable afterwards.</p>
     *
     * <p>Child tables go first so their foreign keys onto a parent are gone
     * before the parent is dropped.</p>
     */
    private static final String[] RETIRED_STATEMENTS = {
        "DROP TABLE IF EXISTS identity_equipped_abilities",
        "DROP TABLE IF EXISTS identity_abilities",
        "DROP TABLE IF EXISTS identity_equipped_runes",
        "DROP TABLE IF EXISTS identity_runes",
        "DROP TABLE IF EXISTS identity_equipped_cosmetics"
    };

    private SqlSchema() {
    }

    /**
     * Runs the three passes in order.
     * @param conn The open connection.
     * @param migrations The dialect's additive migrations, none required to succeed.
     * @param schema The dialect's schema DDL.
     * @param ignorable Decides which schema failures mean the change is already in place.
     * @throws SQLException When a retired-table drop or a schema statement fails otherwise.
     */
    static void apply(Connection conn, String[] migrations, String[] schema,
                      Predicate<SQLException> ignorable) throws SQLException {
        runSchemaStatements(conn, RETIRED_STATEMENTS, ignorable);
        runMigrations(conn, migrations);
        runSchemaStatements(conn, schema, ignorable);
    }

    /**
     * Runs schema statements, tolerating only the errors that mean the change
     * is already in place.
     * @param conn The open connection.
     * @param statements The statements to run, in order.
     * @param ignorable Decides which failures may be skipped.
     * @throws SQLException When a statement fails for any other reason.
     */
    private static void runSchemaStatements(Connection conn, String[] statements,
                                            Predicate<SQLException> ignorable) throws SQLException {
        for (String sql : statements) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.executeUpdate();
            } catch (SQLException e) {
                if (!ignorable.test(e)) {
                    throw e;
                }
            }
        }
    }

    /**
     * Runs migration statements, none of which is required to succeed.
     * @param conn The open connection.
     * @param statements The migrations to attempt, in order.
     */
    private static void runMigrations(Connection conn, String[] statements) {
        for (String sql : statements) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.executeUpdate();
            } catch (SQLException e) {
                // Expected whenever the change is already in place, or the
                // table does not exist yet on a fresh database.
                LOGGER.log(Level.FINE, e, () -> "Skipped schema migration: " + sql);
            }
        }
    }
}

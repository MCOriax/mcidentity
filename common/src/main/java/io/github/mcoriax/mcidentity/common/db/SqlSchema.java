package io.github.mcoriax.mcidentity.common.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Predicate;

/**
 * Applies the dialect's schema statements.
 */
final class SqlSchema {

    private SqlSchema() {
    }

    /**
     * Creates the schema.
     * @param conn The open connection.
     * @param schema The dialect's ordered schema statements.
     * @param ignorable Decides which schema failures may be skipped.
     * @throws SQLException When a schema statement fails and may not be skipped.
     */
    static void apply(Connection conn, String[] schema, Predicate<SQLException> ignorable) throws SQLException {
        for (String sql : schema) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.executeUpdate();
            } catch (SQLException e) {
                if (!ignorable.test(e)) {
                    throw e;
                }
            }
        }
    }
}

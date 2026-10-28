package io.github.mcoriax.mcidentity.common.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Runs work inside one JDBC transaction.
 *
 * <p><strong>Internal.</strong> Package-private, used by {@link AbstractSqlDB},
 * whose identity creation, alias claim and slot grant each carried their own
 * copy of this commit, rollback and restore block.</p>
 */
final class SqlTransactions {

    private SqlTransactions() {
    }

    /**
     * Runs work with auto-commit off, commits it, and rolls it back when it
     * fails. Auto-commit is restored either way, so the connection is left as
     * it was found.
     * @param conn The open connection.
     * @param work The work to run.
     * @param <T> The result type.
     * @return The work's result.
     * @throws SQLException When the work or the commit fails; a rollback
     *                      failure is attached as suppressed.
     */
    static <T> T inTransaction(Connection conn, AbstractSqlDB.SqlFunction<T> work) throws SQLException {
        try {
            conn.setAutoCommit(false);
            T result = work.apply(conn);
            conn.commit();
            return result;
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                e.addSuppressed(rollbackEx);
            }
            throw e;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {
                // The connection is closed right after; nothing reuses it.
            }
        }
    }
}

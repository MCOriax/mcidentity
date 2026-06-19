package io.github.mcoriax.common.db.sql;

import com.zaxxer.hikari.HikariDataSource;
import io.github.mcoriax.api.db.IMCIdentityDB;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

/**
 * Base class for JDBC-backed {@link IMCIdentityDB} implementations.
 *
 * <p>Centralises the Hikari connection handling and the asynchronous
 * execution and error-handling boilerplate shared by the SQLite and PostgreSQL
 * providers. Concrete providers only declare their dialect-specific
 * configuration and the utility call for each operation.</p>
 */
public abstract class AbstractSqlIdentityDB implements IMCIdentityDB {

    /**
     * Hikari data source for connection pooling.
     */
    private final HikariDataSource dataSource;

    /**
     * Creates a provider backed by the given pooled data source.
     * @param dataSource The Hikari data source (may be null for tests that stub {@link #getConnection()}).
     */
    protected AbstractSqlIdentityDB(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Gets a connection from the Hikari connection pool.
     * @return The database connection.
     * @throws SQLException If a connection error occurs.
     */
    protected Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Closes the Hikari data source.
     */
    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * Runs a connection-scoped write asynchronously, logging any SQL failure.
     * @param action The action to run with an open connection.
     * @return A future that completes once the write finishes.
     */
    protected CompletableFuture<Void> runAsync(SqlConsumer action) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                action.accept(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Runs a connection-scoped write asynchronously, wrapping any SQL failure in an
     * unchecked exception so the returned future completes exceptionally.
     * @param action The action to run with an open connection.
     * @param failureMessage The message for the wrapping exception.
     * @return A future that completes once the write finishes.
     */
    protected CompletableFuture<Void> runAsyncOrThrow(SqlConsumer action, String failureMessage) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                action.accept(conn);
            } catch (SQLException e) {
                throw new RuntimeException(failureMessage, e);
            }
        });
    }

    /**
     * Supplies a connection-scoped read asynchronously, returning a fallback on failure.
     * @param fallback The value to return if the read fails.
     * @param action The action that produces the result from an open connection.
     * @param <T> The result type.
     * @return A future that completes with the result, or the fallback on failure.
     */
    protected <T> CompletableFuture<T> supplyAsync(T fallback, SqlFunction<T> action) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                return action.apply(conn);
            } catch (SQLException e) {
                e.printStackTrace();
                return fallback;
            }
        });
    }

    /**
     * A connection-scoped action that may throw {@link SQLException}.
     */
    @FunctionalInterface
    protected interface SqlConsumer {
        void accept(Connection conn) throws SQLException;
    }

    /**
     * A connection-scoped function that may throw {@link SQLException}.
     * @param <T> The result type.
     */
    @FunctionalInterface
    protected interface SqlFunction<T> {
        T apply(Connection conn) throws SQLException;
    }
}

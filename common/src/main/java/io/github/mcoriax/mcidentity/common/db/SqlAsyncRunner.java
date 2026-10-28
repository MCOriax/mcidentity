package io.github.mcoriax.mcidentity.common.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs connection-scoped work on a backend's own executor.
 *
 * <p><strong>Internal.</strong> Package-private, used by {@link AbstractSqlDB}.
 * Every call opens a short-lived connection and closes it when the work ends,
 * so no external connection pool is needed and the game thread never waits.
 * Each method states what a failure does to the returned future.</p>
 */
final class SqlAsyncRunner {

    /**
     * Logger for connection and statement failures.
     */
    private static final Logger LOGGER = Logger.getLogger(AbstractSqlDB.class.getName());

    /**
     * Opens a connection for one unit of work.
     */
    @FunctionalInterface
    interface ConnectionSource {
        /**
         * Opens a connection.
         * @return The open connection.
         * @throws SQLException If it cannot be established.
         */
        Connection open() throws SQLException;
    }

    /**
     * The executor every unit of work runs on.
     */
    private final ExecutorService executor;

    /**
     * Where each unit of work gets its connection.
     */
    private final ConnectionSource connections;

    /**
     * Creates a runner.
     * @param executor The executor every unit of work runs on.
     * @param connections Where each unit of work gets its connection.
     */
    SqlAsyncRunner(ExecutorService executor, ConnectionSource connections) {
        this.executor = executor;
        this.connections = connections;
    }

    /**
     * Runs a write, logging any SQL failure without failing the future.
     * @param action The write.
     * @return A future that completes once the write finishes.
     */
    CompletableFuture<Void> run(AbstractSqlDB.SqlConsumer action) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = connections.open()) {
                action.accept(conn);
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Database write failed", e);
            }
        }, executor);
    }

    /**
     * Runs a write, failing the future with the given message on any SQL failure.
     * @param action The write.
     * @param failureMessage The message for the wrapping exception.
     * @return A future that completes once the write finishes.
     */
    CompletableFuture<Void> runOrThrow(AbstractSqlDB.SqlConsumer action, String failureMessage) {
        return supplyOrThrow(failureMessage, conn -> {
            action.accept(conn);
            return null;
        });
    }

    /**
     * Runs a read, completing with the fallback on any SQL failure.
     * @param fallback The value to return if the read fails.
     * @param action The read.
     * @param <T> The result type.
     * @return A future that completes with the result, or the fallback.
     */
    @SuppressWarnings("unchecked")
    <T> CompletableFuture<T> supply(Object fallback, AbstractSqlDB.SqlFunction<T> action) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = connections.open()) {
                return action.apply(conn);
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Database read failed", e);
                return (T) fallback;
            }
        }, executor);
    }

    /**
     * Runs a read or write, failing the future with the given message on any
     * SQL failure so the caller can react.
     * @param failureMessage The message for the wrapping exception.
     * @param action The work.
     * @param <T> The result type.
     * @return A future that completes with the result, or exceptionally.
     */
    <T> CompletableFuture<T> supplyOrThrow(String failureMessage, AbstractSqlDB.SqlFunction<T> action) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = connections.open()) {
                return action.apply(conn);
            } catch (SQLException e) {
                throw new IllegalStateException(failureMessage, e);
            }
        }, executor);
    }

    /**
     * Stops accepting work.
     */
    void shutdown() {
        executor.shutdown();
    }
}

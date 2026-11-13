package io.github.mcoriax.mcidentity.common.db;

import io.github.mcoriax.mcidentity.api.db.IMCIdentityDB;
import io.github.mcoriax.mcidentity.api.model.ActionType;
import io.github.mcoriax.mcidentity.api.model.PlayerStats;
import io.github.mcoriax.mcidentity.api.model.Profession;
import io.github.mcoriax.mcidentity.api.model.ProfessionProgress;
import io.github.mcoriax.mcidentity.common.model.ConfiguredProfession;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Base class for JDBC-backed {@link IMCIdentityDB} implementations.
 *
 * <p>All query logic shared by the SQLite, MySQL, and PostgreSQL backends
 * lives here; concrete subclasses only supply their connection and the few
 * dialect-specific statements (schema DDL, insert-ignore, and upserts).
 * Every operation runs asynchronously on a dedicated daemon executor and
 * opens a short-lived connection through {@link #openConnection()}, so no
 * external connection pool is required and the game thread is never
 * blocked.</p>
 *
 * <p>Failure semantics mirror the central server client: reads resolve to
 * their neutral fallback (empty optional, empty collection, or zero) and
 * writes complete normally after logging, while {@link #createPlayer} and
 * {@link #initializeSchema} complete exceptionally so callers can react.</p>
 */
public abstract class AbstractSqlDB implements IMCIdentityDB {

    /**
     * Logger for driver lookup failures.
     */
    private static final Logger LOGGER = Logger.getLogger(AbstractSqlDB.class.getName());

    /**
     * Runs every database operation off the game thread on this backend's own executor.
     */
    private final SqlAsyncRunner runner;

    /**
     * Creates the backend with its own asynchronous executor.
     * @param poolSize The number of worker threads. SQLite must use 1 so file
     *                 writes never contend; client-server databases may use more.
     * @param poolName The base name given to the worker threads.
     */
    protected AbstractSqlDB(int poolSize, String poolName) {
        this.runner = new SqlAsyncRunner(DatabaseExecutors.daemonPool(poolSize, poolName), this::openConnection);
    }

    /**
     * Opens a new connection to the database.
     * @return The open connection; the caller closes it after the operation.
     * @throws SQLException If the connection cannot be established.
     */
    protected abstract Connection openConnection() throws SQLException;

    /**
     * Registers a JDBC driver class, logging a warning when it is missing so
     * misconfigured servers get an actionable message instead of a later
     * "no suitable driver" failure.
     * @param driverClassName The fully qualified driver class name.
     */
    protected static void loadDriver(String driverClassName) {
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            LOGGER.warning("JDBC driver " + driverClassName + " is not on the classpath; database calls will fail until it is provided");
        }
    }

    // --- Dialect hooks ---

    /**
     * Supplies the ordered DDL statements creating the schema.
     * @return The dialect-specific schema statements.
     */
    protected abstract String[] schemaStatements();

    /**
     * Decides whether a schema statement failure may be ignored, e.g. a
     * "type already exists" error on re-run.
     * @param e The schema failure.
     * @return true when initialization should continue past the failure.
     */
    protected boolean isIgnorableSchemaError(SQLException e) {
        return false;
    }

    /**
     * Supplies the start of the dialect's insert-if-absent statement, such as
     * {@code INSERT OR IGNORE INTO}.
     * @return The prefix, followed by the table name.
     */
    protected abstract String insertIgnorePrefix();

    /**
     * Supplies the end of the dialect's insert-if-absent statement, such as
     * {@code ON CONFLICT DO NOTHING}.
     * @return The suffix, empty when the prefix alone is enough.
     */
    protected String insertIgnoreSuffix() {
        return "";
    }

    /**
     * Supplies the upsert of the worn profession with (player_uuid,
     * active_profession) parameters.
     * @return The dialect-specific SQL.
     */
    protected abstract String upsertActiveProfessionSql();

    /**
     * Supplies the upsert of one profession's progress with (player_uuid,
     * profession_code, current_level, exp) parameters.
     * @return The dialect-specific SQL.
     */
    protected abstract String upsertProgressSql();

    /**
     * Builds an insert-if-absent statement in this dialect.
     * @param tableAndColumns The table and its column list.
     * @param values The values clause.
     * @return The statement.
     */
    private String insertIgnore(String tableAndColumns, String values) {
        String suffix = insertIgnoreSuffix();
        return insertIgnorePrefix() + " " + tableAndColumns + " " + values
            + (suffix.isEmpty() ? "" : " " + suffix);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> initializeSchema() {
        return runAsyncOrThrow(conn -> SqlSchema.apply(conn, schemaStatements(), this::isIgnorableSchemaError),
            "Failed to initialize the database schema");
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> createProfession(String systemCode, String displayName) {
        String sql = insertIgnore("identity_professions (system_code, display_name)", "VALUES (?, ?)");
        return runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, systemCode);
                ps.setString(2, displayName);
                ps.executeUpdate();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Collection<Profession>> getProfessions() {
        return supplyAsync(Collections.emptyList(), conn -> {
            List<Profession> professions = new ArrayList<>();
            String sql = "SELECT system_code, display_name FROM identity_professions";
            try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    professions.add(new ConfiguredProfession(rs.getString("system_code"), rs.getString("display_name")));
                }
            }
            return professions;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> createPlayer(UUID playerUuid, PlayerStats stats) {
        String sql = insertIgnore("player_stats (player_uuid, " + SqlPlayerRows.STAT_COLUMNS + ")",
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        return runAsyncOrThrow(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                SqlPlayerRows.bindStats(ps, 2, stats);
                ps.executeUpdate();
            }
        }, "Failed to create the player's stats");
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<ProfessionProgress>> getProgress(UUID playerUuid, String professionCode) {
        return supplyAsync(Optional.empty(), conn -> {
            String sql = "SELECT current_level, exp FROM player_profession_progress WHERE player_uuid = ? AND profession_code = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, professionCode);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(SqlPlayerRows.readProgress(rs));
                    }
                }
            }
            return Optional.empty();
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateProgress(UUID playerUuid, String professionCode, ProfessionProgress progress) {
        return runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(upsertProgressSql())) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, professionCode);
                ps.setInt(3, progress.getLevel());
                ps.setInt(4, progress.getExp());
                ps.executeUpdate();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<PlayerStats>> getStats(UUID playerUuid) {
        return supplyAsync(Optional.empty(), conn -> {
            String sql = "SELECT " + SqlPlayerRows.STAT_COLUMNS + " FROM player_stats WHERE player_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(SqlPlayerRows.readStats(rs));
                    }
                }
            }
            return Optional.empty();
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Integer> getStat(UUID playerUuid, String statKey) {
        String sql = StatStatements.select(statKey);
        if (sql == null) {
            return CompletableFuture.completedFuture(0);
        }
        return supplyAsync(0, conn -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(statKey);
                    }
                }
            }
            return 0;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<String>> getActiveProfession(UUID playerUuid) {
        return supplyAsync(Optional.empty(), conn -> {
            String sql = "SELECT active_profession FROM player_professions WHERE player_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.ofNullable(rs.getString("active_profession"));
                    }
                }
            }
            return Optional.empty();
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> setActiveProfession(UUID playerUuid, String professionCode) {
        return runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(upsertActiveProfessionSql())) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, professionCode);
                ps.executeUpdate();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> logAction(UUID playerUuid, String professionCode, ActionType actionType) {
        return runAsync(conn -> {
            String sql = "INSERT INTO profession_logs (player_uuid, profession_code, action_type) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, professionCode);
                ps.setString(3, actionType.name());
                ps.executeUpdate();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateStats(UUID playerUuid, PlayerStats stats) {
        return runAsync(conn -> {
            String sql = "UPDATE player_stats SET stat_points = ?, energy = ?, health = ?, food = ?, strength = ?, defense = ?, wisdom = ?, luck = ?, agility = ?, resilience = ? WHERE player_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                SqlPlayerRows.bindStats(ps, 1, stats);
                ps.setString(11, playerUuid.toString());
                ps.executeUpdate();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> addStat(UUID playerUuid, String statKey, int amount) {
        String sql = StatStatements.add(statKey);
        if (sql == null) {
            return CompletableFuture.completedFuture(null);
        }
        return runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, amount);
                ps.setString(2, playerUuid.toString());
                ps.executeUpdate();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown() {
        runner.shutdown();
    }

    // --- Internal helpers ---

    /**
     * Runs a connection-scoped write asynchronously, logging any SQL failure
     * without failing the returned future.
     * @param action The action to run with an open connection.
     * @return A future that completes once the write finishes.
     */
    protected CompletableFuture<Void> runAsync(SqlConsumer action) {
        return runner.run(action);
    }

    /**
     * Runs a connection-scoped write asynchronously, wrapping any SQL failure
     * in an unchecked exception so the returned future completes exceptionally.
     * @param action The action to run with an open connection.
     * @param failureMessage The message for the wrapping exception.
     * @return A future that completes once the write finishes.
     */
    protected CompletableFuture<Void> runAsyncOrThrow(SqlConsumer action, String failureMessage) {
        return runner.runOrThrow(action, failureMessage);
    }

    /**
     * Supplies a connection-scoped read asynchronously, returning a fallback
     * on failure.
     * @param fallback The value to return if the read fails.
     * @param action The action that produces the result from an open connection.
     * @param <T> The result type.
     * @return A future that completes with the result, or the fallback on failure.
     */
    protected <T> CompletableFuture<T> supplyAsync(Object fallback, SqlFunction<T> action) {
        return runner.supply(fallback, action);
    }

    /**
     * Supplies a connection-scoped result asynchronously, wrapping any SQL
     * failure in an unchecked exception so the returned future completes
     * exceptionally and the caller can react.
     * @param failureMessage The message for the wrapping exception.
     * @param action The action that produces the result from an open connection.
     * @param <T> The result type.
     * @return A future that completes with the result, or exceptionally on failure.
     */
    protected <T> CompletableFuture<T> supplyAsyncOrThrow(String failureMessage, SqlFunction<T> action) {
        return runner.supplyOrThrow(failureMessage, action);
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

    /**
     * Opens a connection through {@link DriverManager}.
     * @param jdbcUrl The JDBC URL.
     * @return The open connection.
     * @throws SQLException If the connection cannot be established.
     */
    protected static Connection connect(String jdbcUrl) throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    /**
     * Opens an authenticated connection through {@link DriverManager}.
     * @param jdbcUrl The JDBC URL.
     * @param username The database user.
     * @param password The database password.
     * @return The open connection.
     * @throws SQLException If the connection cannot be established.
     */
    protected static Connection connect(String jdbcUrl, String username, String password) throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }
}

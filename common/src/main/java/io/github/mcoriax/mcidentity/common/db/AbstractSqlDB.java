package io.github.mcoriax.mcidentity.common.db;

import io.github.mcoriax.mcidentity.api.db.IMCIdentityDB;
import io.github.mcoriax.mcidentity.api.model.*;
import io.github.mcoriax.mcidentity.common.IdentityAlias;
import io.github.mcoriax.mcidentity.common.model.ConfiguredProfession;
import io.github.mcoriax.mcidentity.common.model.IdentityKeyRecord;
import io.github.mcoriax.mcidentity.common.model.IdentitySessionRecord;

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
 * writes complete normally after logging, while {@link #createIdentity} and
 * {@link #initializeSchema} complete exceptionally so callers can react.</p>
 */
public abstract class AbstractSqlDB implements IMCIdentityDB {

    /**
     * Logger for driver lookup failures.
     */
    private static final Logger LOGGER = Logger.getLogger(AbstractSqlDB.class.getName());

    /**
     * Column carrying the identity UUID, read back from result sets by label.
     */
    private static final String COL_IDENTITY_UUID = "identity_uuid";

    /**
     * Resolves the identity holding an alias, matched on the folded form so the
     * comparison ignores case on every dialect regardless of its collation.
     */
    private static final String SELECT_BY_ALIAS_SQL =
        "SELECT identity_uuid FROM identity_properties WHERE LOWER(alias) = ?";

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
     * Supplies the additive statements that bring an already existing schema up
     * to date, such as a column added after the table was first created.
     *
     * <p>They run before {@link #schemaStatements()} — so a later
     * {@code CREATE INDEX IF NOT EXISTS} always sees the column it indexes —
     * and every failure is ignored: on a fresh database the table they alter
     * does not exist yet, and on an upgraded one they have already been
     * applied. Only additive statements belong here; nothing that could destroy
     * player data.</p>
     * @return The dialect-specific migration statements, empty by default.
     */
    protected String[] migrationStatements() {
        return new String[0];
    }

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
     * Supplies the insert-if-absent statement for professions with
     * (system_code, display_name) parameters.
     * @return The dialect-specific SQL.
     */
    protected abstract String insertProfessionIgnoreSql();

    /**
     * Supplies the audit log insert with (player_uuid, identity_uuid,
     * action_type) parameters.
     * @return The dialect-specific SQL.
     */
    protected String insertLogSql() {
        return "INSERT INTO identity_logs (player_uuid, identity_uuid, action_type) VALUES (?, ?, ?)";
    }

    /**
     * Supplies the session upsert with (player_uuid, identity_uuid,
     * content_key) parameters.
     * @return The dialect-specific SQL.
     */
    protected abstract String upsertSessionSql();

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> initializeSchema() {
        return runAsyncOrThrow(conn -> SqlSchema.apply(conn, migrationStatements(), schemaStatements(),
            this::isIgnorableSchemaError), "Failed to initialize the database schema");
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> createProfession(String systemCode, String displayName) {
        return runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(insertProfessionIgnoreSql())) {
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
    public CompletableFuture<Void> createIdentity(IdentityKey key, IdentityProperties properties, IdentityStats stats) {
        if (!StorableProfession.isStorable(key.getProfessionCode())) {
            return StorableProfession.rejected(key.getProfessionCode());
        }
        return runAsyncOrThrow(conn -> SqlTransactions.inTransaction(conn, tx -> {
            SqlIdentityRows.insert(tx, insertProfessionIgnoreSql(), key, properties, stats);
            return null;
        }), "Failed to create identity");
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<IdentityKey>> getIdentityKey(UUID uuid) {
        return supplyAsync(Optional.empty(), conn -> {
            String sql = "SELECT p.system_code AS profession FROM identity_keys k " +
                         "JOIN identity_professions p ON k.profession_id = p.id WHERE k.uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(new IdentityKeyRecord(uuid, rs.getString("profession")));
                    }
                }
            }
            return Optional.empty();
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<IdentityProperties>> getProperties(UUID identityUuid) {
        return supplyAsync(Optional.empty(), conn -> {
            String sql = "SELECT alias, current_level, exp, stat_points, head_text, inventory_contents, enderchest_contents FROM identity_properties WHERE identity_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, identityUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(SqlIdentityRows.readProperties(rs));
                    }
                }
            }
            return Optional.empty();
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<UUID>> getIdentityByAlias(String alias) {
        if (!IdentityAlias.isValid(alias)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return supplyAsync(Optional.empty(), conn -> {
            try (PreparedStatement ps = conn.prepareStatement(SELECT_BY_ALIAS_SQL)) {
                ps.setString(1, IdentityAlias.comparable(alias));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(UUID.fromString(rs.getString(COL_IDENTITY_UUID)));
                    }
                }
            }
            return Optional.empty();
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Boolean> setAlias(UUID identityUuid, String alias) {
        if (alias != null && !IdentityAlias.isValid(alias)) {
            return CompletableFuture.completedFuture(false);
        }
        // The lookup and the write share one transaction so a second identity
        // cannot slip in between them; the unique index is the backstop that
        // makes a lost race fail rather than duplicate the alias.
        return supplyAsync(Boolean.FALSE, conn -> SqlTransactions.inTransaction(conn, tx -> {
            if (alias != null && aliasHeldByAnother(tx, identityUuid, alias)) {
                return Boolean.FALSE;
            }
            try (PreparedStatement ps = tx.prepareStatement(
                    "UPDATE identity_properties SET alias = ? WHERE identity_uuid = ?")) {
                ps.setString(1, alias);
                ps.setString(2, identityUuid.toString());
                return ps.executeUpdate() > 0;
            }
        }));
    }

    /**
     * Reports whether an alias is already claimed by a different identity.
     * @param conn The open connection, inside the claim's transaction.
     * @param identityUuid The identity claiming the alias.
     * @param alias The alias being claimed.
     * @return true when another identity holds the alias.
     * @throws SQLException When the lookup fails.
     */
    private static boolean aliasHeldByAnother(Connection conn, UUID identityUuid, String alias) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_BY_ALIAS_SQL)) {
            ps.setString(1, IdentityAlias.comparable(alias));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && !identityUuid.toString().equals(rs.getString(COL_IDENTITY_UUID));
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<IdentityStats>> getStats(UUID identityUuid) {
        return supplyAsync(Optional.empty(), conn -> {
            String sql = "SELECT " + SqlIdentityRows.STAT_COLUMNS + " FROM identity_stats WHERE identity_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, identityUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(SqlIdentityRows.readStats(rs));
                    }
                }
            }
            return Optional.empty();
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Integer> getStat(UUID identityUuid, String statKey) {
        String sql = StatStatements.select(statKey);
        if (sql == null) {
            return CompletableFuture.completedFuture(0);
        }
        return supplyAsync(0, conn -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, identityUuid.toString());
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
    public CompletableFuture<Optional<UUID>> getActiveIdentity(UUID playerUuid) {
        return supplyAsync(Optional.empty(), conn -> {
            String sql = "SELECT identity_uuid FROM identity_sessions WHERE player_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String id = rs.getString(COL_IDENTITY_UUID);
                        return id != null ? Optional.of(UUID.fromString(id)) : Optional.empty();
                    }
                }
            }
            return Optional.empty();
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<IdentitySession>> getSession(UUID playerUuid) {
        return supplyAsync(Optional.empty(), conn -> {
            String sql = "SELECT identity_uuid, content_key FROM identity_sessions WHERE player_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String idStr = rs.getString(COL_IDENTITY_UUID);
                        if (idStr == null) {
                            return Optional.empty();
                        }
                        return Optional.of(new IdentitySessionRecord(
                            playerUuid, UUID.fromString(idStr), rs.getString("content_key")));
                    }
                }
            }
            return Optional.empty();
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> setActiveIdentity(UUID playerUuid, UUID identityUuid, String contentKey) {
        return runAsync(conn -> {
            if (identityUuid == null) {
                String sql = "DELETE FROM identity_sessions WHERE player_uuid = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, playerUuid.toString());
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(upsertSessionSql())) {
                    ps.setString(1, playerUuid.toString());
                    ps.setString(2, identityUuid.toString());
                    ps.setString(3, contentKey);
                    ps.executeUpdate();
                }
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> logAction(UUID playerUuid, UUID identityUuid, ActionType actionType) {
        return runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(insertLogSql())) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, identityUuid.toString());
                ps.setString(3, actionType.name());
                ps.executeUpdate();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateProperties(UUID identityUuid, IdentityProperties properties) {
        return runAsync(conn -> {
            // The alias is deliberately absent: it is claimed through
            // setAlias, which checks uniqueness in the same transaction, so a
            // blanket property write can never bypass that check.
            String sql = "UPDATE identity_properties SET current_level = ?, exp = ?, stat_points = ?, head_text = ?, inventory_contents = ?, enderchest_contents = ? WHERE identity_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, properties.getLevel());
                ps.setInt(2, properties.getExp());
                ps.setInt(3, properties.getStatPoints());
                ps.setString(4, properties.getHeadText());
                ps.setString(5, properties.getInventoryContents());
                ps.setString(6, properties.getEnderchestContents());
                ps.setString(7, identityUuid.toString());
                ps.executeUpdate();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateStats(UUID identityUuid, IdentityStats stats) {
        return runAsync(conn -> {
            String sql = "UPDATE identity_stats SET energy = ?, health = ?, food = ?, strength = ?, defense = ?, wisdom = ?, luck = ?, agility = ?, resilience = ? WHERE identity_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                SqlIdentityRows.bindStats(ps, 1, stats);
                ps.setString(10, identityUuid.toString());
                ps.executeUpdate();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> addStat(UUID identityUuid, String statKey, int amount) {
        String sql = StatStatements.add(statKey);
        if (sql == null) {
            return CompletableFuture.completedFuture(null);
        }
        return runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, amount);
                ps.setString(2, identityUuid.toString());
                ps.executeUpdate();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateSessionContentKey(UUID playerUuid, String contentKey) {
        return runAsync(conn -> {
            String sql = "UPDATE identity_sessions SET content_key = ? WHERE player_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, contentKey);
                ps.setString(2, playerUuid.toString());
                ps.executeUpdate();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Integer> getBaseIdentitySlots(UUID playerUuid) {
        return supplyAsync(0, conn -> {
            String sql = "SELECT slots FROM player_base_slots WHERE player_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("slots");
                    }
                }
            }
            return 0;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Integer> addBaseIdentitySlots(UUID playerUuid, int amount) {
        // Read-modify-write inside one transaction keeps the statement
        // dialect-free, so all three SQL backends share this implementation.
        return supplyAsyncOrThrow("Failed to update base identity slots",
            conn -> SqlTransactions.inTransaction(conn, tx -> {
                Integer current = null;
                String select = "SELECT slots FROM player_base_slots WHERE player_uuid = ?";
                try (PreparedStatement ps = tx.prepareStatement(select)) {
                    ps.setString(1, playerUuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            current = rs.getInt("slots");
                        }
                    }
                }
                int next = Math.max(0, (current != null ? current : 0) + amount);
                String write = current != null
                    ? "UPDATE player_base_slots SET slots = ? WHERE player_uuid = ?"
                    : "INSERT INTO player_base_slots (slots, player_uuid) VALUES (?, ?)";
                try (PreparedStatement ps = tx.prepareStatement(write)) {
                    ps.setInt(1, next);
                    ps.setString(2, playerUuid.toString());
                    ps.executeUpdate();
                }
                return next;
            }));
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

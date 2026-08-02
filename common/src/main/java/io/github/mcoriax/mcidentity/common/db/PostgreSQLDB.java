package io.github.mcoriax.mcidentity.common.db;

import io.github.mcoriax.mcidentity.api.db.IMCIdentityDB;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * PostgreSQL implementation of {@link IMCIdentityDB}.
 *
 * <p>Connects to a PostgreSQL server using {@code ON CONFLICT} upserts and an
 * enum type for the audit log action kinds.</p>
 */
public class PostgreSQLDB extends AbstractSqlDB {

    /**
     * SQLState reported by PostgreSQL when a CREATE TYPE target already exists.
     */
    private static final String DUPLICATE_OBJECT_SQLSTATE = "42710";

    /**
     * JDBC URL pointing at the PostgreSQL database.
     */
    private final String jdbcUrl;

    /**
     * Database user.
     */
    private final String username;

    /**
     * Database password.
     */
    private final String password;

    /**
     * Creates a PostgreSQL backend for the given server and credentials.
     * @param host The server host name or address.
     * @param port The server port, usually 5432.
     * @param database The database name.
     * @param username The database user.
     * @param password The database password.
     */
    public PostgreSQLDB(String host, int port, String database, String username, String password) {
        super(4, "MCIdentity-PostgreSQL");
        this.jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
        this.username = username;
        this.password = password;
        loadDriver("org.postgresql.Driver");
    }

    /** {@inheritDoc} */
    @Override
    protected Connection openConnection() throws SQLException {
        return connect(jdbcUrl, username, password);
    }

    /** {@inheritDoc} */
    @Override
    protected String[] schemaStatements() {
        return new String[] {
            "CREATE TYPE action_type_enum AS ENUM ('EQUIP', 'UNEQUIP', 'DEATH_DROP', 'ADMIN_REMOVE')",
            "CREATE TABLE IF NOT EXISTS identity_professions (id SERIAL PRIMARY KEY, system_code VARCHAR(255) UNIQUE NOT NULL, display_name VARCHAR(255) NOT NULL)",
            "CREATE TABLE IF NOT EXISTS identity_keys (uuid VARCHAR(36) PRIMARY KEY, profession_id INT NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (profession_id) REFERENCES identity_professions(id))",
            "CREATE TABLE IF NOT EXISTS identity_properties (identity_uuid VARCHAR(36) PRIMARY KEY, alias VARCHAR(32) DEFAULT NULL, current_level INT DEFAULT 1, exp INT DEFAULT 0, stat_points INT DEFAULT 0, head_text TEXT DEFAULT NULL, inventory_contents TEXT DEFAULT NULL, enderchest_contents TEXT DEFAULT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_stats (identity_uuid VARCHAR(36) PRIMARY KEY, energy INT DEFAULT 100, health INT DEFAULT 20, food INT DEFAULT 20, strength INT DEFAULT 0, defense INT DEFAULT 0, wisdom INT DEFAULT 0, luck INT DEFAULT 0, agility INT DEFAULT 0, resilience INT DEFAULT 0, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_sessions (player_uuid VARCHAR(36) PRIMARY KEY, identity_uuid VARCHAR(36) UNIQUE, content_key TEXT, equipped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_logs (id SERIAL PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, identity_uuid VARCHAR(36) NOT NULL, action_type action_type_enum NOT NULL, action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS player_base_slots (player_uuid VARCHAR(36) PRIMARY KEY, slots INT NOT NULL DEFAULT 0, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            // Indexing the folded alias makes uniqueness case-insensitive;
            // PostgreSQL treats NULLs as distinct, so unnamed identities never
            // collide.
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_identity_properties_alias ON identity_properties (LOWER(alias))"
        };
    }

    /** {@inheritDoc} */
    @Override
    protected String[] migrationStatements() {
        // PostgreSQL can only append a column, so databases created before
        // aliases existed carry it in last position rather than after
        // identity_uuid.
        return new String[] {
            "ALTER TABLE identity_properties ADD COLUMN IF NOT EXISTS alias VARCHAR(32) DEFAULT NULL"
        };
    }

    /** {@inheritDoc} */
    @Override
    protected boolean isIgnorableSchemaError(SQLException e) {
        // CREATE TYPE has no IF NOT EXISTS form, so re-runs report 42710.
        return DUPLICATE_OBJECT_SQLSTATE.equals(e.getSQLState());
    }

    /** {@inheritDoc} */
    @Override
    protected String insertProfessionIgnoreSql() {
        return "INSERT INTO identity_professions (system_code, display_name) VALUES (?, ?) ON CONFLICT (system_code) DO NOTHING";
    }

    /** {@inheritDoc} */
    @Override
    protected String insertLogSql() {
        return "INSERT INTO identity_logs (player_uuid, identity_uuid, action_type) VALUES (?, ?, ?::action_type_enum)";
    }

    /** {@inheritDoc} */
    @Override
    protected String upsertSessionSql() {
        return "INSERT INTO identity_sessions (player_uuid, identity_uuid, content_key) VALUES (?, ?, ?) " +
               "ON CONFLICT (player_uuid) DO UPDATE SET identity_uuid = EXCLUDED.identity_uuid, content_key = EXCLUDED.content_key";
    }

    /** {@inheritDoc} */
    /** {@inheritDoc} */
}

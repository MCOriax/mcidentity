package io.github.mcoriax.mcidentity.common.db;

import io.github.mcoriax.mcidentity.api.db.IMCIdentityDB;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite implementation of {@link IMCIdentityDB}.
 *
 * <p>Stores everything in a single local database file, which makes it the
 * zero-configuration default backend. A single worker thread serializes all
 * access so file writes never contend, and foreign keys are enabled on every
 * connection. The schema matches the MCIdentity central server's SQLite
 * schema, so a database file can be moved between the two.</p>
 */
public class SQLiteDB extends AbstractSqlDB {

    /**
     * JDBC URL pointing at the database file.
     */
    private final String jdbcUrl;

    /**
     * Creates a SQLite backend for the given database file.
     * @param dbPath The file path of the SQLite database.
     */
    public SQLiteDB(String dbPath) {
        super(1, "MCIdentity-SQLite");
        this.jdbcUrl = "jdbc:sqlite:" + dbPath;
        loadDriver("org.sqlite.JDBC");
    }

    /** {@inheritDoc} */
    @Override
    protected Connection openConnection() throws SQLException {
        Connection conn = connect(jdbcUrl);
        try (Statement statement = conn.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            conn.close();
            throw e;
        }
        return conn;
    }

    /** {@inheritDoc} */
    @Override
    protected String[] schemaStatements() {
        return new String[] {
            "CREATE TABLE IF NOT EXISTS identity_professions (id INTEGER PRIMARY KEY AUTOINCREMENT, system_code VARCHAR(255) UNIQUE NOT NULL, display_name VARCHAR(255) NOT NULL)",
            "CREATE TABLE IF NOT EXISTS identity_keys (uuid VARCHAR(36) PRIMARY KEY, profession_id INT NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (profession_id) REFERENCES identity_professions(id))",
            "CREATE TABLE IF NOT EXISTS identity_properties (identity_uuid VARCHAR(36) PRIMARY KEY, alias VARCHAR(32) DEFAULT NULL, current_level INT DEFAULT 1, exp INT DEFAULT 0, stat_points INT DEFAULT 0, head_text TEXT DEFAULT NULL, inventory_contents TEXT DEFAULT NULL, enderchest_contents TEXT DEFAULT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_stats (identity_uuid VARCHAR(36) PRIMARY KEY, energy INT DEFAULT 100, health INT DEFAULT 20, food INT DEFAULT 20, strength INT DEFAULT 0, defense INT DEFAULT 0, wisdom INT DEFAULT 0, luck INT DEFAULT 0, agility INT DEFAULT 0, resilience INT DEFAULT 0, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_sessions (player_uuid VARCHAR(36) PRIMARY KEY, identity_uuid VARCHAR(36) UNIQUE, content_key TEXT, equipped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid VARCHAR(36) NOT NULL, identity_uuid VARCHAR(36) NOT NULL, action_type VARCHAR(50) NOT NULL, action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS player_base_slots (player_uuid VARCHAR(36) PRIMARY KEY, slots INT NOT NULL DEFAULT 0, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            // Indexing the folded alias makes uniqueness case-insensitive; SQLite
            // treats NULLs as distinct, so unnamed identities never collide.
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_identity_properties_alias ON identity_properties (LOWER(alias))"
        };
    }

    /** {@inheritDoc} */
    @Override
    protected String[] migrationStatements() {
        // SQLite can only append a column, so databases created before aliases
        // existed carry it in last position rather than after identity_uuid.
        return new String[] {
            "ALTER TABLE identity_properties ADD COLUMN alias VARCHAR(32) DEFAULT NULL"
        };
    }

    /** {@inheritDoc} */
    @Override
    protected String insertProfessionIgnoreSql() {
        return "INSERT OR IGNORE INTO identity_professions (system_code, display_name) VALUES (?, ?)";
    }

    /** {@inheritDoc} */
    @Override
    protected String upsertSessionSql() {
        return "INSERT OR REPLACE INTO identity_sessions (player_uuid, identity_uuid, content_key) VALUES (?, ?, ?)";
    }

    /** {@inheritDoc} */
    /** {@inheritDoc} */
}

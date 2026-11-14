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
            "CREATE TABLE IF NOT EXISTS player_stats (player_uuid VARCHAR(36) PRIMARY KEY, stat_points INT DEFAULT 0, energy INT DEFAULT 100, health INT DEFAULT 20, food INT DEFAULT 20, strength INT DEFAULT 0, defense INT DEFAULT 0, wisdom INT DEFAULT 0, luck INT DEFAULT 0, agility INT DEFAULT 0, resilience INT DEFAULT 0, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS player_professions (player_uuid VARCHAR(36) PRIMARY KEY, active_profession VARCHAR(255) NOT NULL, equipped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS player_profession_progress (player_uuid VARCHAR(36) NOT NULL, profession_code VARCHAR(255) NOT NULL, current_level INT DEFAULT 1, exp INT DEFAULT 0, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (player_uuid, profession_code))",
            "CREATE TABLE IF NOT EXISTS profession_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid VARCHAR(36) NOT NULL, profession_code VARCHAR(255) NOT NULL, action_type VARCHAR(50) NOT NULL, action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
        };
    }

    /** {@inheritDoc} */
    @Override
    protected String insertProfessionSql() {
        return "INSERT OR IGNORE INTO identity_professions (system_code, display_name) VALUES (?, ?)";
    }

    /** {@inheritDoc} */
    @Override
    protected String insertPlayerSql() {
        return "INSERT OR IGNORE INTO player_stats (player_uuid, stat_points, energy, health, food, strength, defense, wisdom, luck, agility, resilience) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    /** {@inheritDoc} */
    @Override
    protected String upsertActiveProfessionSql() {
        return "INSERT OR REPLACE INTO player_professions (player_uuid, active_profession) VALUES (?, ?)";
    }

    /** {@inheritDoc} */
    @Override
    protected String upsertProgressSql() {
        return "INSERT OR REPLACE INTO player_profession_progress (player_uuid, profession_code, current_level, exp) VALUES (?, ?, ?, ?)";
    }
}

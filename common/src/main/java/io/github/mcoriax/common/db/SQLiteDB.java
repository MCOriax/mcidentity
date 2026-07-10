package io.github.mcoriax.common.db;

import io.github.mcoriax.api.db.IMCIdentityDB;

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
            "CREATE TABLE IF NOT EXISTS identity_abilities (id INTEGER PRIMARY KEY AUTOINCREMENT, system_code VARCHAR(255) UNIQUE NOT NULL, display_name VARCHAR(255) NOT NULL, ability_skin VARCHAR(255) DEFAULT NULL)",
            "CREATE TABLE IF NOT EXISTS identity_runes (id INTEGER PRIMARY KEY AUTOINCREMENT, system_code VARCHAR(255) UNIQUE NOT NULL, display_name VARCHAR(255) NOT NULL)",
            "CREATE TABLE IF NOT EXISTS identity_keys (uuid VARCHAR(36) PRIMARY KEY, profession_id INT NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (profession_id) REFERENCES identity_professions(id))",
            "CREATE TABLE IF NOT EXISTS identity_properties (identity_uuid VARCHAR(36) PRIMARY KEY, current_level INT DEFAULT 1, exp INT DEFAULT 0, stat_points INT DEFAULT 0, head_text TEXT DEFAULT NULL, inventory_contents TEXT DEFAULT NULL, enderchest_contents TEXT DEFAULT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_stats (identity_uuid VARCHAR(36) PRIMARY KEY, energy INT DEFAULT 100, health INT DEFAULT 20, food INT DEFAULT 20, strength INT DEFAULT 0, defense INT DEFAULT 0, wisdom INT DEFAULT 0, luck INT DEFAULT 0, agility INT DEFAULT 0, resilience INT DEFAULT 0, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_equipped_abilities (identity_uuid VARCHAR(36) NOT NULL, ability_id INT NOT NULL, slot_number INT NOT NULL, ability_skin VARCHAR(255) DEFAULT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (identity_uuid, slot_number), UNIQUE (identity_uuid, ability_id), FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE, FOREIGN KEY (ability_id) REFERENCES identity_abilities(id))",
            "CREATE TABLE IF NOT EXISTS identity_equipped_runes (identity_uuid VARCHAR(36) NOT NULL, rune_id INT NOT NULL, slot_number INT NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (identity_uuid, slot_number), FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE, FOREIGN KEY (rune_id) REFERENCES identity_runes(id))",
            "CREATE TABLE IF NOT EXISTS identity_sessions (player_uuid VARCHAR(36) PRIMARY KEY, identity_uuid VARCHAR(36) UNIQUE, content_key TEXT, equipped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_equipped_cosmetics (identity_uuid VARCHAR(36) PRIMARY KEY, skin TEXT DEFAULT NULL, aura VARCHAR(255) DEFAULT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid VARCHAR(36) NOT NULL, identity_uuid VARCHAR(36) NOT NULL, action_type VARCHAR(50) NOT NULL, action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
        };
    }

    /** {@inheritDoc} */
    @Override
    protected String insertProfessionIgnoreSql() {
        return "INSERT OR IGNORE INTO identity_professions (system_code, display_name) VALUES (?, ?)";
    }

    /** {@inheritDoc} */
    @Override
    protected String insertAbilityIgnoreSql() {
        return "INSERT OR IGNORE INTO identity_abilities (system_code, display_name, ability_skin) VALUES (?, ?, ?)";
    }

    /** {@inheritDoc} */
    @Override
    protected String upsertSessionSql() {
        return "INSERT OR REPLACE INTO identity_sessions (player_uuid, identity_uuid, content_key) VALUES (?, ?, ?)";
    }

    /** {@inheritDoc} */
    @Override
    protected String upsertEquippedAbilitySql() {
        return "INSERT OR REPLACE INTO identity_equipped_abilities (identity_uuid, ability_id, slot_number, ability_skin) " +
               "VALUES (?, (SELECT id FROM identity_abilities WHERE system_code = ?), ?, ?)";
    }

    /** {@inheritDoc} */
    @Override
    protected String upsertEquippedRuneSql() {
        return "INSERT OR REPLACE INTO identity_equipped_runes (identity_uuid, rune_id, slot_number) " +
               "VALUES (?, (SELECT id FROM identity_runes WHERE system_code = ?), ?)";
    }

    /** {@inheritDoc} */
    @Override
    protected String upsertCosmeticSql(String column) {
        return "INSERT INTO identity_equipped_cosmetics (identity_uuid, " + column + ") VALUES (?, ?) " +
               "ON CONFLICT (identity_uuid) DO UPDATE SET " + column + " = EXCLUDED." + column;
    }
}

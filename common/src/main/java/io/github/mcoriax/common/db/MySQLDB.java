package io.github.mcoriax.common.db;

import io.github.mcoriax.api.db.IMCIdentityDB;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * MySQL (and MariaDB) implementation of {@link IMCIdentityDB}.
 *
 * <p>Connects to a MySQL-compatible server, which suits networks that already
 * run one for other plugins. Upserts use {@code INSERT ... ON DUPLICATE KEY
 * UPDATE} and the schema uses InnoDB-style foreign keys, so both MySQL and
 * MariaDB are supported.</p>
 */
public class MySQLDB extends AbstractSqlDB {

    /**
     * JDBC URL pointing at the MySQL database.
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
     * Creates a MySQL backend for the given server and credentials.
     * @param host The server host name or address.
     * @param port The server port, usually 3306.
     * @param database The database name.
     * @param username The database user.
     * @param password The database password.
     */
    public MySQLDB(String host, int port, String database, String username, String password) {
        super(4, "MCIdentity-MySQL");
        this.jdbcUrl = String.format("jdbc:mysql://%s:%d/%s", host, port, database);
        this.username = username;
        this.password = password;
        loadDriver("com.mysql.cj.jdbc.Driver");
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
            "CREATE TABLE IF NOT EXISTS identity_professions (id INT PRIMARY KEY AUTO_INCREMENT, system_code VARCHAR(255) UNIQUE NOT NULL, display_name VARCHAR(255) NOT NULL)",
            "CREATE TABLE IF NOT EXISTS identity_abilities (id INT PRIMARY KEY AUTO_INCREMENT, system_code VARCHAR(255) UNIQUE NOT NULL, display_name VARCHAR(255) NOT NULL, ability_type VARCHAR(50) NOT NULL, is_global BOOLEAN DEFAULT FALSE)",
            "CREATE TABLE IF NOT EXISTS identity_runes (id INT PRIMARY KEY AUTO_INCREMENT, system_code VARCHAR(255) UNIQUE NOT NULL, display_name VARCHAR(255) NOT NULL)",
            "CREATE TABLE IF NOT EXISTS identity_keys (uuid VARCHAR(36) PRIMARY KEY, profession_id INT NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (profession_id) REFERENCES identity_professions(id))",
            "CREATE TABLE IF NOT EXISTS identity_properties (identity_uuid VARCHAR(36) PRIMARY KEY, current_level INT DEFAULT 1, exp INT DEFAULT 0, stat_points INT DEFAULT 0, head_text TEXT DEFAULT NULL, inventory_contents MEDIUMTEXT DEFAULT NULL, enderchest_contents MEDIUMTEXT DEFAULT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_stats (identity_uuid VARCHAR(36) PRIMARY KEY, energy INT DEFAULT 100, health INT DEFAULT 20, food INT DEFAULT 20, strength INT DEFAULT 0, defense INT DEFAULT 0, wisdom INT DEFAULT 0, luck INT DEFAULT 0, agility INT DEFAULT 0, resilience INT DEFAULT 0, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_equipped_abilities (identity_uuid VARCHAR(36) NOT NULL, ability_id INT NOT NULL, slot_number INT NOT NULL, model_id VARCHAR(255) DEFAULT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (identity_uuid, slot_number), UNIQUE (identity_uuid, ability_id), FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE, FOREIGN KEY (ability_id) REFERENCES identity_abilities(id))",
            "CREATE TABLE IF NOT EXISTS identity_equipped_runes (identity_uuid VARCHAR(36) NOT NULL, rune_id INT NOT NULL, slot_number INT NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (identity_uuid, slot_number), FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE, FOREIGN KEY (rune_id) REFERENCES identity_runes(id))",
            "CREATE TABLE IF NOT EXISTS identity_sessions (player_uuid VARCHAR(36) PRIMARY KEY, identity_uuid VARCHAR(36) UNIQUE, content_key TEXT, equipped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_equipped_cosmetics (identity_uuid VARCHAR(36) PRIMARY KEY, skin TEXT DEFAULT NULL, aura VARCHAR(255) DEFAULT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_logs (id INT PRIMARY KEY AUTO_INCREMENT, player_uuid VARCHAR(36) NOT NULL, identity_uuid VARCHAR(36) NOT NULL, action_type VARCHAR(50) NOT NULL, action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
        };
    }

    /** {@inheritDoc} */
    @Override
    protected String insertProfessionIgnoreSql() {
        return "INSERT IGNORE INTO identity_professions (system_code, display_name) VALUES (?, ?)";
    }

    /** {@inheritDoc} */
    @Override
    protected String insertAbilityIgnoreSql() {
        return "INSERT IGNORE INTO identity_abilities (system_code, display_name, ability_type, is_global) VALUES (?, ?, ?, ?)";
    }

    /** {@inheritDoc} */
    @Override
    protected String upsertSessionSql() {
        return "INSERT INTO identity_sessions (player_uuid, identity_uuid, content_key) VALUES (?, ?, ?) " +
               "ON DUPLICATE KEY UPDATE identity_uuid = VALUES(identity_uuid), content_key = VALUES(content_key)";
    }

    /** {@inheritDoc} */
    @Override
    protected String upsertEquippedAbilitySql() {
        return "INSERT INTO identity_equipped_abilities (identity_uuid, ability_id, slot_number, model_id) " +
               "VALUES (?, (SELECT id FROM identity_abilities WHERE system_code = ?), ?, ?) " +
               "ON DUPLICATE KEY UPDATE ability_id = VALUES(ability_id), model_id = VALUES(model_id)";
    }

    /** {@inheritDoc} */
    @Override
    protected String upsertEquippedRuneSql() {
        return "INSERT INTO identity_equipped_runes (identity_uuid, rune_id, slot_number) " +
               "VALUES (?, (SELECT id FROM identity_runes WHERE system_code = ?), ?) " +
               "ON DUPLICATE KEY UPDATE rune_id = VALUES(rune_id)";
    }

    /** {@inheritDoc} */
    @Override
    protected String upsertCosmeticSql(String column) {
        return "INSERT INTO identity_equipped_cosmetics (identity_uuid, " + column + ") VALUES (?, ?) " +
               "ON DUPLICATE KEY UPDATE " + column + " = VALUES(" + column + ")";
    }
}

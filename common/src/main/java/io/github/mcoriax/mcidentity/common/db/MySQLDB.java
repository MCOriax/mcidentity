package io.github.mcoriax.mcidentity.common.db;

import io.github.mcoriax.mcidentity.api.db.IMCIdentityDB;

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
            "CREATE TABLE IF NOT EXISTS identity_keys (uuid VARCHAR(36) PRIMARY KEY, profession_id INT NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (profession_id) REFERENCES identity_professions(id))",
            "CREATE TABLE IF NOT EXISTS identity_properties (identity_uuid VARCHAR(36) PRIMARY KEY, alias VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL UNIQUE, current_level INT DEFAULT 1, exp INT DEFAULT 0, stat_points INT DEFAULT 0, head_text TEXT DEFAULT NULL, inventory_contents MEDIUMTEXT DEFAULT NULL, enderchest_contents MEDIUMTEXT DEFAULT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_stats (identity_uuid VARCHAR(36) PRIMARY KEY, energy INT DEFAULT 100, health INT DEFAULT 20, food INT DEFAULT 20, strength INT DEFAULT 0, defense INT DEFAULT 0, wisdom INT DEFAULT 0, luck INT DEFAULT 0, agility INT DEFAULT 0, resilience INT DEFAULT 0, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_sessions (player_uuid VARCHAR(36) PRIMARY KEY, identity_uuid VARCHAR(36) UNIQUE, content_key TEXT, equipped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_logs (id INT PRIMARY KEY AUTO_INCREMENT, player_uuid VARCHAR(36) NOT NULL, identity_uuid VARCHAR(36) NOT NULL, action_type VARCHAR(50) NOT NULL, action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS player_base_slots (player_uuid VARCHAR(36) PRIMARY KEY, slots INT NOT NULL DEFAULT 0, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
        };
    }

    /** {@inheritDoc} */
    @Override
    protected String[] migrationStatements() {
        // MySQL and MariaDB can place a new column, so an upgraded database
        // ends up with the same column order as a freshly created one. The
        // explicit _ci collation is what makes the UNIQUE constraint
        // case-insensitive, whatever the table default happens to be.
        return new String[] {
            "ALTER TABLE identity_properties ADD COLUMN alias VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL AFTER identity_uuid",
            "ALTER TABLE identity_properties ADD UNIQUE INDEX idx_identity_properties_alias (alias)"
        };
    }

    /** {@inheritDoc} */
    @Override
    protected String insertProfessionIgnoreSql() {
        return "INSERT IGNORE INTO identity_professions (system_code, display_name) VALUES (?, ?)";
    }

    /** {@inheritDoc} */
    @Override
    protected String upsertSessionSql() {
        return "INSERT INTO identity_sessions (player_uuid, identity_uuid, content_key) VALUES (?, ?, ?) " +
               "ON DUPLICATE KEY UPDATE identity_uuid = VALUES(identity_uuid), content_key = VALUES(content_key)";
    }

    /** {@inheritDoc} */
    /** {@inheritDoc} */
}

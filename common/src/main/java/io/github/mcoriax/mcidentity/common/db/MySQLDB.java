package io.github.mcoriax.mcidentity.common.db;

import io.github.mcoriax.mcidentity.api.db.IMCIdentityDB;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * MySQL (and MariaDB) implementation of {@link IMCIdentityDB}.
 *
 * <p>Connects to a MySQL-compatible server, which suits networks that already
 * run one for other plugins. Upserts use {@code INSERT ... ON DUPLICATE KEY
 * UPDATE}, which both MySQL and MariaDB support.</p>
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
            "CREATE TABLE IF NOT EXISTS player_stats (player_uuid VARCHAR(36) PRIMARY KEY, stat_points INT DEFAULT 0, energy INT DEFAULT 100, health INT DEFAULT 20, food INT DEFAULT 20, strength INT DEFAULT 0, defense INT DEFAULT 0, wisdom INT DEFAULT 0, luck INT DEFAULT 0, agility INT DEFAULT 0, resilience INT DEFAULT 0, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS player_professions (player_uuid VARCHAR(36) PRIMARY KEY, active_profession VARCHAR(255) NOT NULL, equipped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS player_profession_progress (player_uuid VARCHAR(36) NOT NULL, profession_code VARCHAR(255) NOT NULL, current_level INT DEFAULT 1, exp INT DEFAULT 0, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (player_uuid, profession_code))",
            "CREATE TABLE IF NOT EXISTS profession_logs (id INT PRIMARY KEY AUTO_INCREMENT, player_uuid VARCHAR(36) NOT NULL, profession_code VARCHAR(255) NOT NULL, action_type VARCHAR(50) NOT NULL, action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
        };
    }

    /** {@inheritDoc} */
    @Override
    protected String insertProfessionSql() {
        return "INSERT IGNORE INTO identity_professions (system_code, display_name) VALUES (?, ?)";
    }

    /** {@inheritDoc} */
    @Override
    protected String insertPlayerSql() {
        return "INSERT IGNORE INTO player_stats (player_uuid, stat_points, energy, health, food, strength, defense, wisdom, luck, agility, resilience) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    /** {@inheritDoc} */
    @Override
    protected String upsertActiveProfessionSql() {
        return "INSERT INTO player_professions (player_uuid, active_profession) VALUES (?, ?) "
            + "ON DUPLICATE KEY UPDATE active_profession = VALUES(active_profession), equipped_at = CURRENT_TIMESTAMP";
    }

    /** {@inheritDoc} */
    @Override
    protected String upsertProgressSql() {
        return "INSERT INTO player_profession_progress (player_uuid, profession_code, current_level, exp) VALUES (?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE current_level = VALUES(current_level), exp = VALUES(exp)";
    }
}

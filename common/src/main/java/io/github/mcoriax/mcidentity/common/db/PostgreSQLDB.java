package io.github.mcoriax.mcidentity.common.db;

import io.github.mcoriax.mcidentity.api.db.IMCIdentityDB;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * PostgreSQL implementation of {@link IMCIdentityDB}.
 *
 * <p>Connects to a PostgreSQL server and upserts with {@code ON CONFLICT}.</p>
 */
public class PostgreSQLDB extends AbstractSqlDB {

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
            "CREATE TABLE IF NOT EXISTS identity_professions (id SERIAL PRIMARY KEY, system_code VARCHAR(255) UNIQUE NOT NULL, display_name VARCHAR(255) NOT NULL)",
            "CREATE TABLE IF NOT EXISTS player_stats (player_uuid VARCHAR(36) PRIMARY KEY, stat_points INT DEFAULT 0, energy INT DEFAULT 100, health INT DEFAULT 20, food INT DEFAULT 20, strength INT DEFAULT 0, defense INT DEFAULT 0, wisdom INT DEFAULT 0, luck INT DEFAULT 0, agility INT DEFAULT 0, resilience INT DEFAULT 0, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS player_professions (player_uuid VARCHAR(36) PRIMARY KEY, active_profession VARCHAR(255) NOT NULL, equipped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS player_profession_progress (player_uuid VARCHAR(36) NOT NULL, profession_code VARCHAR(255) NOT NULL, current_level INT DEFAULT 1, exp INT DEFAULT 0, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (player_uuid, profession_code))",
            "CREATE TABLE IF NOT EXISTS profession_logs (id SERIAL PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, profession_code VARCHAR(255) NOT NULL, action_type VARCHAR(50) NOT NULL, action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
        };
    }

    /** {@inheritDoc} */
    @Override
    InsertIgnoreStatements insertIgnoreStatements() {
        return InsertIgnoreStatements.POSTGRESQL;
    }

    /** {@inheritDoc} */
    @Override
    protected String upsertActiveProfessionSql() {
        return "INSERT INTO player_professions (player_uuid, active_profession) VALUES (?, ?) "
            + "ON CONFLICT (player_uuid) DO UPDATE SET active_profession = EXCLUDED.active_profession, equipped_at = CURRENT_TIMESTAMP";
    }

    /** {@inheritDoc} */
    @Override
    protected String upsertProgressSql() {
        return "INSERT INTO player_profession_progress (player_uuid, profession_code, current_level, exp) VALUES (?, ?, ?, ?) "
            + "ON CONFLICT (player_uuid, profession_code) DO UPDATE SET current_level = EXCLUDED.current_level, exp = EXCLUDED.exp";
    }
}

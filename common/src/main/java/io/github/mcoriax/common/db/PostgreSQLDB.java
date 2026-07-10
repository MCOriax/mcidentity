package io.github.mcoriax.common.db;

import io.github.mcoriax.api.db.IMCIdentityDB;

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
            "CREATE TABLE IF NOT EXISTS identity_abilities (id SERIAL PRIMARY KEY, system_code VARCHAR(255) UNIQUE NOT NULL, display_name VARCHAR(255) NOT NULL, ability_skin VARCHAR(255) DEFAULT NULL)",
            "CREATE TABLE IF NOT EXISTS identity_runes (id SERIAL PRIMARY KEY, system_code VARCHAR(255) UNIQUE NOT NULL, display_name VARCHAR(255) NOT NULL)",
            "CREATE TABLE IF NOT EXISTS identity_keys (uuid VARCHAR(36) PRIMARY KEY, profession_id INT NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (profession_id) REFERENCES identity_professions(id))",
            "CREATE TABLE IF NOT EXISTS identity_properties (identity_uuid VARCHAR(36) PRIMARY KEY, current_level INT DEFAULT 1, exp INT DEFAULT 0, stat_points INT DEFAULT 0, head_text TEXT DEFAULT NULL, inventory_contents TEXT DEFAULT NULL, enderchest_contents TEXT DEFAULT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_stats (identity_uuid VARCHAR(36) PRIMARY KEY, energy INT DEFAULT 100, health INT DEFAULT 20, food INT DEFAULT 20, strength INT DEFAULT 0, defense INT DEFAULT 0, wisdom INT DEFAULT 0, luck INT DEFAULT 0, agility INT DEFAULT 0, resilience INT DEFAULT 0, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_equipped_abilities (identity_uuid VARCHAR(36) NOT NULL, ability_id INT NOT NULL, slot_number INT NOT NULL, ability_skin VARCHAR(255) DEFAULT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (identity_uuid, slot_number), UNIQUE (identity_uuid, ability_id), FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE, FOREIGN KEY (ability_id) REFERENCES identity_abilities(id))",
            "CREATE TABLE IF NOT EXISTS identity_equipped_runes (identity_uuid VARCHAR(36) NOT NULL, rune_id INT NOT NULL, slot_number INT NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (identity_uuid, slot_number), FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE, FOREIGN KEY (rune_id) REFERENCES identity_runes(id))",
            "CREATE TABLE IF NOT EXISTS identity_sessions (player_uuid VARCHAR(36) PRIMARY KEY, identity_uuid VARCHAR(36) UNIQUE, content_key TEXT, equipped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_equipped_cosmetics (identity_uuid VARCHAR(36) PRIMARY KEY, skin TEXT DEFAULT NULL, aura VARCHAR(255) DEFAULT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_logs (id SERIAL PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, identity_uuid VARCHAR(36) NOT NULL, action_type action_type_enum NOT NULL, action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
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
    protected String insertAbilityIgnoreSql() {
        return "INSERT INTO identity_abilities (system_code, display_name, ability_skin) VALUES (?, ?, ?) ON CONFLICT (system_code) DO NOTHING";
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
    @Override
    protected String upsertEquippedAbilitySql() {
        return "INSERT INTO identity_equipped_abilities (identity_uuid, ability_id, slot_number, ability_skin) " +
               "VALUES (?, (SELECT id FROM identity_abilities WHERE system_code = ?), ?, ?) " +
               "ON CONFLICT (identity_uuid, slot_number) DO UPDATE SET ability_id = EXCLUDED.ability_id, ability_skin = EXCLUDED.ability_skin";
    }

    /** {@inheritDoc} */
    @Override
    protected String upsertEquippedRuneSql() {
        return "INSERT INTO identity_equipped_runes (identity_uuid, rune_id, slot_number) " +
               "VALUES (?, (SELECT id FROM identity_runes WHERE system_code = ?), ?) " +
               "ON CONFLICT (identity_uuid, slot_number) DO UPDATE SET rune_id = EXCLUDED.rune_id";
    }

    /** {@inheritDoc} */
    @Override
    protected String upsertCosmeticSql(String column) {
        return "INSERT INTO identity_equipped_cosmetics (identity_uuid, " + column + ") VALUES (?, ?) " +
               "ON CONFLICT (identity_uuid) DO UPDATE SET " + column + " = EXCLUDED." + column;
    }
}

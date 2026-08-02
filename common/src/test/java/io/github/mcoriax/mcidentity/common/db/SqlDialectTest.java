package io.github.mcoriax.mcidentity.common.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Dialect tests for the MySQL and PostgreSQL backends over a mocked JDBC
 * connection, verifying each engine receives its own insert-ignore, upsert,
 * and enum-cast statement forms.
 */
class SqlDialectTest {

    /**
     * MySQL backend whose connections are the mocked one.
     */
    static class TestMySQLDB extends MySQLDB {
        private final Connection connection;

        TestMySQLDB(Connection connection) {
            super("localhost", 3306, "db", "user", "pass");
            this.connection = connection;
        }

        @Override
        protected Connection openConnection() {
            return connection;
        }
    }

    /**
     * PostgreSQL backend whose connections are the mocked one.
     */
    static class TestPostgreSQLDB extends PostgreSQLDB {
        private final Connection connection;

        TestPostgreSQLDB(Connection connection) {
            super("localhost", 5432, "db", "user", "pass");
            this.connection = connection;
        }

        @Override
        protected Connection openConnection() {
            return connection;
        }
    }

    private Connection mockConnection;
    private PreparedStatement mockStatement;
    private ResultSet mockResultSet;
    private UUID identityId;
    private UUID playerId;

    @BeforeEach
    void setUpFixtures() throws SQLException {
        mockConnection = mock(Connection.class);
        mockStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);
        identityId = UUID.randomUUID();
        playerId = UUID.randomUUID();

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockStatement.executeUpdate()).thenReturn(1);
    }

    @Test
    @DisplayName("MySQL schema uses AUTO_INCREMENT identifiers")
    void mysqlSchema() throws SQLException {
        TestMySQLDB db = new TestMySQLDB(mockConnection);
        db.initializeSchema().join();
        verify(mockConnection, atLeastOnce()).prepareStatement(contains("AUTO_INCREMENT"));
        verify(mockConnection).prepareStatement(contains("CREATE TABLE IF NOT EXISTS identity_logs"));
        db.shutdown();
    }

    @Test
    @DisplayName("PostgreSQL schema ignores duplicate-type errors on re-run")
    void postgresIgnoresDuplicateTypes() throws SQLException {
        SQLException duplicateType = new SQLException("type exists", "42710");
        when(mockConnection.prepareStatement(contains("CREATE TYPE"))).thenThrow(duplicateType);

        TestPostgreSQLDB db = new TestPostgreSQLDB(mockConnection);
        db.initializeSchema().join();
        verify(mockConnection, atLeastOnce()).prepareStatement(contains("CREATE TABLE IF NOT EXISTS identity_professions"));
        db.shutdown();
    }

    @Test
    @DisplayName("Schema initialization fails exceptionally on real errors")
    void schemaFailurePropagates() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("boom", "58000"));
        TestPostgreSQLDB db = new TestPostgreSQLDB(mockConnection);
        assertThrows(Exception.class, () -> db.initializeSchema().join());
        db.shutdown();
    }

    @Test
    @DisplayName("Both dialects declare the alias column and back-fill existing tables")
    void aliasSchemaAndMigration() throws SQLException {
        TestMySQLDB mysql = new TestMySQLDB(mockConnection);
        mysql.initializeSchema().join();
        // MySQL relies on a case-insensitive collation for the unique
        // constraint, and can place the back-filled column exactly.
        verify(mockConnection).prepareStatement(contains("alias VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL UNIQUE, current_level"));
        verify(mockConnection).prepareStatement(contains("ADD COLUMN alias VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL AFTER identity_uuid"));
        mysql.shutdown();

        TestPostgreSQLDB postgres = new TestPostgreSQLDB(mockConnection);
        postgres.initializeSchema().join();
        // PostgreSQL folds in the index instead, so the constraint holds
        // whatever the database collation is.
        verify(mockConnection).prepareStatement(contains("alias VARCHAR(32) DEFAULT NULL, current_level"));
        verify(mockConnection).prepareStatement(contains("CREATE UNIQUE INDEX IF NOT EXISTS idx_identity_properties_alias ON identity_properties (LOWER(alias))"));
        verify(mockConnection).prepareStatement(contains("ADD COLUMN IF NOT EXISTS alias VARCHAR(32)"));
        postgres.shutdown();
    }

    @Test
    @DisplayName("A migration that has already been applied does not fail startup")
    void migrationFailuresAreIgnored() throws SQLException {
        when(mockConnection.prepareStatement(contains("ADD COLUMN")))
            .thenThrow(new SQLException("duplicate column name: alias", "42S21"));

        TestPostgreSQLDB db = new TestPostgreSQLDB(mockConnection);
        db.initializeSchema().join();
        verify(mockConnection, atLeastOnce()).prepareStatement(contains("CREATE TABLE IF NOT EXISTS identity_properties"));
        db.shutdown();
    }

    @Test
    @DisplayName("Alias lookups compare on the folded value")
    void aliasLookupFoldsCase() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("identity_uuid")).thenReturn(identityId.toString());

        TestMySQLDB db = new TestMySQLDB(mockConnection);
        assertEquals(identityId, db.getIdentityByAlias("Alex").join().orElseThrow());
        verify(mockConnection).prepareStatement(contains("WHERE LOWER(alias) = ?"));
        verify(mockStatement).setString(1, "alex");
        db.shutdown();
    }

    @Test
    @DisplayName("Malformed aliases never reach SQL")
    void aliasAllowList() throws SQLException {
        TestMySQLDB db = new TestMySQLDB(mockConnection);
        assertTrue(db.getIdentityByAlias("'; DROP TABLE identity_properties --").join().isEmpty());
        assertFalse(db.setAlias(identityId, "not valid").join());
        verify(mockConnection, never()).prepareStatement(anyString());
        db.shutdown();
    }

    @Test
    @DisplayName("Stat keys outside the allow-list never reach SQL")
    void statAllowList() throws SQLException {
        TestMySQLDB db = new TestMySQLDB(mockConnection);
        db.addStat(identityId, "strength; DROP TABLE identity_stats", 1).join();
        db.getStat(identityId, "1=1").join();
        verify(mockConnection, never()).prepareStatement(anyString());
        db.shutdown();
    }
}

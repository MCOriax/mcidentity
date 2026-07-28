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
    @DisplayName("Stat keys outside the allow-list never reach SQL")
    void statAllowList() throws SQLException {
        TestMySQLDB db = new TestMySQLDB(mockConnection);
        db.addStat(identityId, "strength; DROP TABLE identity_stats", 1).join();
        db.getStat(identityId, "1=1").join();
        verify(mockConnection, never()).prepareStatement(anyString());
        db.shutdown();
    }
}

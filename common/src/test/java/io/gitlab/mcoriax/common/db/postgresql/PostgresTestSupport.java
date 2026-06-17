package io.gitlab.mcoriax.common.db.postgresql;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Shared Mockito fixtures for the PostgreSQL database utility tests.
 * <p>
 * The PostgreSQL suite is split across several focused subclasses so that no
 * single test file grows too large. They all share this one JDBC mock setup.
 * </p>
 */
abstract class PostgresTestSupport {

    protected Connection mockConnection;
    protected PreparedStatement mockStatement;
    protected ResultSet mockResultSet;
    protected UUID identityId;
    protected UUID playerId;
    protected TestMCIdentityPostgreSQL provider;

    /**
     * Concrete subclass of {@link MCIdentityPostgreSQL} exposing the mocked JDBC connection.
     */
    static class TestMCIdentityPostgreSQL extends MCIdentityPostgreSQL {
        private final Connection connection;

        TestMCIdentityPostgreSQL(Connection connection) {
            super((HikariDataSource) null);
            this.connection = connection;
        }

        @Override
        protected Connection getConnection() throws SQLException {
            return connection;
        }
    }

    @BeforeEach
    void setUpFixtures() throws SQLException {
        mockConnection = mock(Connection.class);
        mockStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);
        identityId = UUID.randomUUID();
        playerId = UUID.randomUUID();
        provider = new TestMCIdentityPostgreSQL(mockConnection);

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockStatement.executeUpdate()).thenReturn(1);
    }
}

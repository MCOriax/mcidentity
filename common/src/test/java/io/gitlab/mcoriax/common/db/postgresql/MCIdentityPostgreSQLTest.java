package io.gitlab.mcoriax.common.db.postgresql;

import io.gitlab.mcoriax.api.model.IdentityKey;
import io.gitlab.mcoriax.api.model.IdentityProperties;
import io.gitlab.mcoriax.common.db.postgresql.utils.GetIdentityKey;
import io.gitlab.mcoriax.common.db.postgresql.utils.UpdateProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MCIdentityPostgreSQLTest {

    private Connection mockConnection;
    private PreparedStatement mockStatement;
    private ResultSet mockResultSet;
    private UUID identityId;

    @BeforeEach
    void setUp() throws SQLException {
        mockConnection = mock(Connection.class);
        mockStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);
        identityId = UUID.randomUUID();

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
    }

    @Test
    @DisplayName("Test GetIdentityKey utility (PostgreSQL)")
    void testGetIdentityKey() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("profession")).thenReturn("mage");

        Optional<IdentityKey> result = GetIdentityKey.invoke(mockConnection, identityId);

        assertTrue(result.isPresent());
        assertEquals(identityId, result.get().getUniqueId());
        assertEquals("mage", result.get().getProfessionCode());

        verify(mockConnection).prepareStatement(contains("SELECT"));
        verify(mockStatement).setString(1, identityId.toString());
    }

    @Test
    @DisplayName("Test GetIdentityKey when not found (PostgreSQL)")
    void testGetIdentityKeyNotFound() throws SQLException {
        when(mockResultSet.next()).thenReturn(false);

        Optional<IdentityKey> result = GetIdentityKey.invoke(mockConnection, identityId);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Test UpdateProperties utility (PostgreSQL)")
    void testUpdateProperties() throws SQLException {
        IdentityProperties props = mock(IdentityProperties.class);
        when(props.getLevel()).thenReturn(20);
        when(props.getExp()).thenReturn(2000);
        when(props.getStatPoints()).thenReturn(0);
        when(props.getHeadText()).thenReturn("head-texture-pg");
        when(props.getInventoryContents()).thenReturn("inv-pg");
        when(props.getEnderchestContents()).thenReturn("ec-pg");

        UpdateProperties.invoke(mockConnection, identityId, props);

        verify(mockConnection).prepareStatement(contains("UPDATE identity_properties"));
        verify(mockStatement).setInt(1, 20);
        verify(mockStatement).setInt(2, 2000);
        verify(mockStatement).setInt(3, 0);
        verify(mockStatement).setString(4, "head-texture-pg");
        verify(mockStatement).setString(5, "inv-pg");
        verify(mockStatement).setString(6, "ec-pg");
        verify(mockStatement).setString(7, identityId.toString());
        verify(mockStatement).executeUpdate();
    }
}

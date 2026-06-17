package io.gitlab.mcoriax.common.db.postgresql;

import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.common.db.postgresql.utils.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Schema bootstrapping, provider delegation and failure-path tests for PostgreSQL.
 */
class PostgresSchemaAndProviderTest extends PostgresTestSupport {

    @Test
    @DisplayName("Test MCIdentityPostgreSQL provider method calls")
    void testProviderMethods() throws Exception {
        provider.initializeSchema().join();
        verify(mockConnection, atLeastOnce()).prepareStatement(anyString());

        provider.createProfession("warrior", "Warrior").join();
        verify(mockConnection).prepareStatement(eq("INSERT INTO identity_professions (system_code, display_name) VALUES (?, ?) ON CONFLICT (system_code) DO NOTHING"));

        provider.addStat(identityId, "strength", 10).join();
        verify(mockConnection).prepareStatement(eq("UPDATE identity_stats SET strength = strength + ? WHERE identity_uuid = ?"));
    }

    @Test
    @DisplayName("Test InitializeSchema utility rigor")
    void testInitializeSchema() throws SQLException {
        InitializeSchema.invoke(mockConnection);
        verify(mockConnection, atLeastOnce()).prepareStatement(contains("CREATE TABLE IF NOT EXISTS"));
    }

    @Test
    @DisplayName("Test GetIdentityKey when exception occurs")
    void testGetIdentityKeyException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));
        Optional<IdentityKey> result = GetIdentityKey.invoke(mockConnection, identityId);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Test AddStat with invalid key")
    void testAddStatInvalidKey() throws SQLException {
        AddStat.invoke(mockConnection, identityId, "invalid_stat", 10);
        verify(mockConnection, never()).prepareStatement(anyString());
    }

    @Test
    @DisplayName("Test GetStats when record missing")
    void testGetStatsMissing() throws SQLException {
        when(mockResultSet.next()).thenReturn(false);
        Optional<IdentityStats> result = GetStats.invoke(mockConnection, identityId);
        assertFalse(result.isPresent());
    }
}

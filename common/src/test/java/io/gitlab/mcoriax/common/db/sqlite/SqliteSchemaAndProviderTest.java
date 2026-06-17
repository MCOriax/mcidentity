package io.gitlab.mcoriax.common.db.sqlite;

import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.common.db.sqlite.utils.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Schema bootstrapping, provider delegation and failure-path tests for SQLite.
 */
class SqliteSchemaAndProviderTest extends SqliteTestSupport {

    @Test
    @DisplayName("Test MCIdentitySQLite provider delegation logic")
    void testProviderDelegation() throws Exception {
        // Initialize Schema
        provider.initializeSchema().join();
        verify(mockConnection, atLeastOnce()).prepareStatement(contains("CREATE TABLE IF NOT EXISTS"));

        // Create Profession
        provider.createProfession("warrior", "Warrior Class").join();
        verify(mockConnection).prepareStatement(eq("INSERT OR IGNORE INTO identity_professions (system_code, display_name) VALUES (?, ?)"));
        verify(mockStatement).setString(1, "warrior");
        verify(mockStatement).setString(2, "Warrior Class");

        // Add Stat
        provider.addStat(identityId, "strength", 15).join();
        verify(mockConnection).prepareStatement(eq("UPDATE identity_stats SET strength = strength + ? WHERE identity_uuid = ?"));
        verify(mockStatement).setInt(1, 15);
        verify(mockStatement).setString(2, identityId.toString());
    }

    @Test
    @DisplayName("Test InitializeSchema utility rigor")
    void testInitializeSchemaRigorous() throws SQLException {
        InitializeSchema.invoke(mockConnection);
        // Verify multiple table creations
        verify(mockConnection).prepareStatement(contains("CREATE TABLE IF NOT EXISTS identity_professions"));
        verify(mockConnection).prepareStatement(contains("CREATE TABLE IF NOT EXISTS identity_keys"));
        verify(mockConnection).prepareStatement(contains("CREATE TABLE IF NOT EXISTS identity_properties"));
        verify(mockConnection).prepareStatement(contains("CREATE TABLE IF NOT EXISTS identity_stats"));
        verify(mockConnection, atLeast(5)).prepareStatement(anyString());
    }

    @Test
    @DisplayName("Test GetIdentityKey when SQLException occurs")
    void testGetIdentityKeyException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));
        Optional<IdentityKey> result = GetIdentityKey.invoke(mockConnection, identityId);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Test AddStat with invalid key logic")
    void testAddStatInvalidKey() throws SQLException {
        AddStat.invoke(mockConnection, identityId, "invalid_column", 10);
        // Utility has internal check for valid stat keys
        verify(mockConnection, never()).prepareStatement(anyString());
    }

    @Test
    @DisplayName("Test GetStats when record is missing")
    void testGetStatsMissing() throws SQLException {
        when(mockResultSet.next()).thenReturn(false);
        Optional<IdentityStats> result = GetStats.invoke(mockConnection, identityId);
        assertFalse(result.isPresent());
    }
}

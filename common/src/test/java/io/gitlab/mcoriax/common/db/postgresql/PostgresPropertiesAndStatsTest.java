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
 * Properties and stats read/update tests for PostgreSQL.
 */
class PostgresPropertiesAndStatsTest extends PostgresTestSupport {

    @Test
    @DisplayName("Test UpdateProperties utility rigor")
    void testUpdateProperties() throws SQLException {
        IdentityProperties props = mock(IdentityProperties.class);
        when(props.getLevel()).thenReturn(10);
        when(props.getExp()).thenReturn(500);
        when(props.getStatPoints()).thenReturn(5);
        when(props.getHeadText()).thenReturn("head-texture");
        when(props.getInventoryContents()).thenReturn("inv-data");
        when(props.getEnderchestContents()).thenReturn("ec-data");

        UpdateProperties.invoke(mockConnection, identityId, props);

        verify(mockConnection).prepareStatement(eq("UPDATE identity_properties SET current_level = ?, exp = ?, stat_points = ?, head_text = ?, inventory_contents = ?, enderchest_contents = ? WHERE identity_uuid = ?"));
        verify(mockStatement).setInt(1, 10);
        verify(mockStatement).setInt(2, 500);
        verify(mockStatement).setInt(3, 5);
        verify(mockStatement).setString(4, "head-texture");
        verify(mockStatement).setString(5, "inv-data");
        verify(mockStatement).setString(6, "ec-data");
        verify(mockStatement).setString(7, identityId.toString());
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test GetProperties utility rigor")
    void testGetProperties() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("current_level")).thenReturn(5);

        Optional<IdentityProperties> result = GetProperties.invoke(mockConnection, identityId);
        assertTrue(result.isPresent());
        assertEquals(5, result.get().getLevel());

        verify(mockConnection).prepareStatement(eq("SELECT current_level, exp, stat_points, head_text, inventory_contents, enderchest_contents FROM identity_properties WHERE identity_uuid = ?"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).executeQuery();
    }

    @Test
    @DisplayName("Test AddStat utility rigor")
    void testAddStat() throws SQLException {
        AddStat.invoke(mockConnection, identityId, "health", 20);
        verify(mockConnection).prepareStatement(eq("UPDATE identity_stats SET health = health + ? WHERE identity_uuid = ?"));
        verify(mockStatement).setInt(1, 20);
        verify(mockStatement).setString(2, identityId.toString());
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test GetStat utility rigor")
    void testGetStat() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(anyString())).thenReturn(50);

        int result = GetStat.invoke(mockConnection, identityId, "strength");

        assertEquals(50, result);
        verify(mockConnection).prepareStatement(eq("SELECT strength FROM identity_stats WHERE identity_uuid = ?"));
        verify(mockStatement).setString(1, identityId.toString());
    }

    @Test
    @DisplayName("Test GetStats utility rigor")
    void testGetStats() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("energy")).thenReturn(100);
        when(mockResultSet.getInt("health")).thenReturn(200);

        Optional<IdentityStats> result = GetStats.invoke(mockConnection, identityId);

        assertTrue(result.isPresent());
        assertEquals(100, result.get().getEnergy());
        assertEquals(200, result.get().getHealth());

        verify(mockConnection).prepareStatement(eq("SELECT energy, health, food, strength, defense, wisdom, luck, agility, resilience FROM identity_stats WHERE identity_uuid = ?"));
        verify(mockStatement).setString(1, identityId.toString());
    }

    @Test
    @DisplayName("Test UpdateStats utility rigor")
    void testUpdateStats() throws SQLException {
        IdentityStats stats = mock(IdentityStats.class);
        when(stats.getEnergy()).thenReturn(100);
        when(stats.getHealth()).thenReturn(200);
        when(stats.getFood()).thenReturn(300);
        when(stats.getStrength()).thenReturn(10);
        when(stats.getDefense()).thenReturn(20);
        when(stats.getWisdom()).thenReturn(30);
        when(stats.getLuck()).thenReturn(40);
        when(stats.getAgility()).thenReturn(50);
        when(stats.getResilience()).thenReturn(60);

        UpdateStats.invoke(mockConnection, identityId, stats);
        verify(mockConnection).prepareStatement(eq("UPDATE identity_stats SET energy = ?, health = ?, food = ?, strength = ?, defense = ?, wisdom = ?, luck = ?, agility = ?, resilience = ? WHERE identity_uuid = ?"));
        verify(mockStatement).setInt(1, 100);
        verify(mockStatement).setString(10, identityId.toString());
        verify(mockStatement).executeUpdate();
    }
}

package io.github.mcoriax.common.db.sqlite;

import io.github.mcoriax.api.model.*;
import io.github.mcoriax.common.db.sqlite.utils.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Properties and stats read/update tests for SQLite.
 */
class SqlitePropertiesAndStatsTest extends SqliteTestSupport {

    @Test
    @DisplayName("Test UpdateProperties utility rigor")
    void testUpdatePropertiesRigorous() throws SQLException {
        IdentityProperties props = mock(IdentityProperties.class);
        when(props.getLevel()).thenReturn(15);
        when(props.getExp()).thenReturn(1500);
        when(props.getStatPoints()).thenReturn(2);
        when(props.getHeadText()).thenReturn("new_head");
        when(props.getInventoryContents()).thenReturn("inv_data");
        when(props.getEnderchestContents()).thenReturn("ec_data");

        UpdateProperties.invoke(mockConnection, identityId, props);

        verify(mockConnection).prepareStatement(eq("UPDATE identity_properties SET current_level = ?, exp = ?, stat_points = ?, head_text = ?, inventory_contents = ?, enderchest_contents = ? WHERE identity_uuid = ?"));
        verify(mockStatement).setInt(1, 15);
        verify(mockStatement).setInt(2, 1500);
        verify(mockStatement).setInt(3, 2);
        verify(mockStatement).setString(4, "new_head");
        verify(mockStatement).setString(5, "inv_data");
        verify(mockStatement).setString(6, "ec_data");
        verify(mockStatement).setString(7, identityId.toString());
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test GetProperties utility rigor")
    void testGetPropertiesRigorous() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("current_level")).thenReturn(10);
        when(mockResultSet.getInt("exp")).thenReturn(100);
        when(mockResultSet.getInt("stat_points")).thenReturn(5);
        when(mockResultSet.getString("head_text")).thenReturn("head_tex");
        when(mockResultSet.getString("inventory_contents")).thenReturn("inv");
        when(mockResultSet.getString("enderchest_contents")).thenReturn("ec");

        Optional<IdentityProperties> result = GetProperties.invoke(mockConnection, identityId);
        assertTrue(result.isPresent());
        assertEquals(10, result.get().getLevel());
        assertEquals("inv", result.get().getInventoryContents());

        verify(mockConnection).prepareStatement(eq("SELECT current_level, exp, stat_points, head_text, inventory_contents, enderchest_contents FROM identity_properties WHERE identity_uuid = ?"));
        verify(mockStatement).setString(1, identityId.toString());
    }

    @Test
    @DisplayName("Test AddStat utility rigor")
    void testAddStatRigorous() throws SQLException {
        AddStat.invoke(mockConnection, identityId, "energy", 50);
        verify(mockConnection).prepareStatement(eq("UPDATE identity_stats SET energy = energy + ? WHERE identity_uuid = ?"));
        verify(mockStatement).setInt(1, 50);
        verify(mockStatement).setString(2, identityId.toString());
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test GetStat utility rigor")
    void testGetStatRigorous() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(anyString())).thenReturn(42);

        int result = GetStat.invoke(mockConnection, identityId, "luck");

        assertEquals(42, result);
        verify(mockConnection).prepareStatement(eq("SELECT luck FROM identity_stats WHERE identity_uuid = ?"));
        verify(mockStatement).setString(1, identityId.toString());
    }

    @Test
    @DisplayName("Test GetStats utility rigor")
    void testGetStatsRigorous() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("energy")).thenReturn(100);
        when(mockResultSet.getInt("health")).thenReturn(200);
        when(mockResultSet.getInt("food")).thenReturn(50);
        when(mockResultSet.getInt("strength")).thenReturn(10);
        when(mockResultSet.getInt("defense")).thenReturn(5);
        when(mockResultSet.getInt("wisdom")).thenReturn(3);
        when(mockResultSet.getInt("luck")).thenReturn(2);
        when(mockResultSet.getInt("agility")).thenReturn(8);
        when(mockResultSet.getInt("resilience")).thenReturn(4);

        Optional<IdentityStats> result = GetStats.invoke(mockConnection, identityId);

        assertTrue(result.isPresent());
        assertEquals(100, result.get().getEnergy());
        assertEquals(200, result.get().getHealth());
        assertEquals(10, result.get().getStrength());

        verify(mockConnection).prepareStatement(eq("SELECT energy, health, food, strength, defense, wisdom, luck, agility, resilience FROM identity_stats WHERE identity_uuid = ?"));
        verify(mockStatement).setString(1, identityId.toString());
    }

    @Test
    @DisplayName("Test UpdateStats utility rigor")
    void testUpdateStatsRigorous() throws SQLException {
        IdentityStats stats = mock(IdentityStats.class);
        when(stats.getEnergy()).thenReturn(100);
        when(stats.getHealth()).thenReturn(200);
        when(stats.getFood()).thenReturn(50);
        when(stats.getStrength()).thenReturn(10);
        when(stats.getDefense()).thenReturn(5);
        when(stats.getWisdom()).thenReturn(3);
        when(stats.getLuck()).thenReturn(2);
        when(stats.getAgility()).thenReturn(8);
        when(stats.getResilience()).thenReturn(4);

        UpdateStats.invoke(mockConnection, identityId, stats);
        verify(mockConnection).prepareStatement(eq("UPDATE identity_stats SET energy = ?, health = ?, food = ?, strength = ?, defense = ?, wisdom = ?, luck = ?, agility = ?, resilience = ? WHERE identity_uuid = ?"));
        verify(mockStatement).setInt(1, 100);
        verify(mockStatement).setInt(2, 200);
        verify(mockStatement).setInt(3, 50);
        verify(mockStatement).setInt(4, 10);
        verify(mockStatement).setInt(5, 5);
        verify(mockStatement).setInt(6, 3);
        verify(mockStatement).setInt(7, 2);
        verify(mockStatement).setInt(8, 8);
        verify(mockStatement).setInt(9, 4);
        verify(mockStatement).setString(10, identityId.toString());
        verify(mockStatement).executeUpdate();
    }
}

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
 * Profession and identity-key creation/lookup tests for SQLite.
 */
class SqliteProfessionAndIdentityTest extends SqliteTestSupport {

    @Test
    @DisplayName("Test CreateProfession utility rigor")
    void testCreateProfessionRigorous() throws SQLException {
        CreateProfession.invoke(mockConnection, "mage", "Mage Class");
        verify(mockConnection).prepareStatement(eq("INSERT OR IGNORE INTO identity_professions (system_code, display_name) VALUES (?, ?)"));
        verify(mockStatement).setString(1, "mage");
        verify(mockStatement).setString(2, "Mage Class");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test GetProfessions utility rigor")
    void testGetProfessionsRigorous() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getString("system_code")).thenReturn("warrior", "mage");
        when(mockResultSet.getString("display_name")).thenReturn("Warrior", "Mage");

        Collection<Profession> result = GetProfessions.invoke(mockConnection);

        assertEquals(2, result.size());
        verify(mockConnection).prepareStatement(eq("SELECT system_code, display_name FROM identity_professions"));
    }

    @Test
    @DisplayName("Test CreateIdentity utility rigor")
    void testCreateIdentityRigorous() throws SQLException {
        IdentityKey key = mock(IdentityKey.class);
        when(key.getUniqueId()).thenReturn(identityId);
        when(key.getProfessionCode()).thenReturn("archer");

        IdentityProperties props = mock(IdentityProperties.class);
        when(props.getLevel()).thenReturn(1);
        when(props.getExp()).thenReturn(0);
        when(props.getStatPoints()).thenReturn(5);
        when(props.getHeadText()).thenReturn("base64_tex");

        IdentityStats stats = mock(IdentityStats.class);
        when(stats.getEnergy()).thenReturn(100);
        when(stats.getHealth()).thenReturn(250);
        when(stats.getFood()).thenReturn(50);
        when(stats.getStrength()).thenReturn(10);
        when(stats.getDefense()).thenReturn(5);
        when(stats.getWisdom()).thenReturn(2);
        when(stats.getLuck()).thenReturn(1);
        when(stats.getAgility()).thenReturn(8);
        when(stats.getResilience()).thenReturn(4);

        CreateIdentity.invoke(mockConnection, key, props, stats);

        // 1. Profession exist check
        verify(mockConnection, atLeastOnce()).prepareStatement(eq("INSERT OR IGNORE INTO identity_professions (system_code, display_name) VALUES (?, ?)"));
        verify(mockStatement, atLeastOnce()).setString(1, "archer");
        verify(mockStatement, atLeastOnce()).setString(2, "archer");

        // 2. Identity Key insertion
        verify(mockConnection, atLeastOnce()).prepareStatement(eq("INSERT INTO identity_keys (uuid, profession_id) VALUES (?, (SELECT id FROM identity_professions WHERE system_code = ?))"));
        verify(mockStatement, atLeastOnce()).setString(1, identityId.toString());
        verify(mockStatement, atLeastOnce()).setString(2, "archer");

        // 3. Properties insertion
        verify(mockConnection, atLeastOnce()).prepareStatement(eq("INSERT INTO identity_properties (identity_uuid, current_level, exp, stat_points, head_text) VALUES (?, ?, ?, ?, ?)"));
        verify(mockStatement, atLeastOnce()).setString(1, identityId.toString());
        verify(mockStatement, atLeastOnce()).setInt(2, 1);
        verify(mockStatement, atLeastOnce()).setInt(3, 0);
        verify(mockStatement, atLeastOnce()).setInt(4, 5);
        verify(mockStatement, atLeastOnce()).setString(5, "base64_tex");

        // 4. Stats insertion
        verify(mockConnection, atLeastOnce()).prepareStatement(eq("INSERT INTO identity_stats (identity_uuid, energy, health, food, strength, defense, wisdom, luck, agility, resilience) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"));
        verify(mockStatement, atLeastOnce()).setString(1, identityId.toString());
        verify(mockStatement, atLeastOnce()).setInt(2, 100);
        verify(mockStatement, atLeastOnce()).setInt(3, 250);
        verify(mockStatement, atLeastOnce()).setInt(4, 50);
        verify(mockStatement, atLeastOnce()).setInt(5, 10);
        verify(mockStatement, atLeastOnce()).setInt(10, 4);

        verify(mockConnection).commit();
    }

    @Test
    @DisplayName("Test GetIdentityKey utility rigor")
    void testGetIdentityKeyRigorous() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("profession")).thenReturn("warrior");

        Optional<IdentityKey> result = GetIdentityKey.invoke(mockConnection, identityId);

        assertTrue(result.isPresent());
        assertEquals(identityId, result.get().getUniqueId());
        assertEquals("warrior", result.get().getProfessionCode());

        verify(mockConnection).prepareStatement(eq("SELECT p.system_code as profession FROM identity_keys k JOIN identity_professions p ON k.profession_id = p.id WHERE k.uuid = ?"));
        verify(mockStatement).setString(1, identityId.toString());
    }
}

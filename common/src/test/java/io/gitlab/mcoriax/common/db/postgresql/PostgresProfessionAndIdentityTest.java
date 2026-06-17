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
 * Profession and identity-key creation/lookup tests for PostgreSQL.
 */
class PostgresProfessionAndIdentityTest extends PostgresTestSupport {

    @Test
    @DisplayName("Test CreateProfession utility rigor")
    void testCreateProfession() throws SQLException {
        CreateProfession.invoke(mockConnection, "mage", "Mage Class");
        verify(mockConnection).prepareStatement(eq("INSERT INTO identity_professions (system_code, display_name) VALUES (?, ?) ON CONFLICT (system_code) DO NOTHING"));
        verify(mockStatement).setString(1, "mage");
        verify(mockStatement).setString(2, "Mage Class");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test GetProfessions utility rigor")
    void testGetProfessions() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("system_code")).thenReturn("warrior");
        when(mockResultSet.getString("display_name")).thenReturn("Warrior");

        Collection<Profession> result = GetProfessions.invoke(mockConnection);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("warrior", result.iterator().next().getSystemCode());

        verify(mockConnection).prepareStatement(eq("SELECT system_code, display_name FROM identity_professions"));
    }

    @Test
    @DisplayName("Test CreateIdentity utility rigor")
    void testCreateIdentity() throws SQLException {
        IdentityKey key = mock(IdentityKey.class);
        when(key.getUniqueId()).thenReturn(identityId);
        when(key.getProfessionCode()).thenReturn("warrior");
        IdentityProperties props = mock(IdentityProperties.class);
        when(props.getLevel()).thenReturn(1);
        when(props.getExp()).thenReturn(0);
        when(props.getStatPoints()).thenReturn(0);
        when(props.getHeadText()).thenReturn("texture");
        IdentityStats stats = mock(IdentityStats.class);
        when(stats.getEnergy()).thenReturn(100);
        when(stats.getHealth()).thenReturn(100);
        when(stats.getFood()).thenReturn(100);

        CreateIdentity.invoke(mockConnection, key, props, stats);

        verify(mockConnection).prepareStatement(eq("INSERT INTO identity_professions (system_code, display_name) VALUES (?, ?) ON CONFLICT (system_code) DO NOTHING"));
        // 2. Identity Key insertion
        verify(mockConnection, atLeastOnce()).prepareStatement(eq("INSERT INTO identity_keys (uuid, profession_id) VALUES (?, (SELECT id FROM identity_professions WHERE system_code = ?))"));
        verify(mockStatement, atLeastOnce()).setString(1, identityId.toString());
        verify(mockStatement, atLeastOnce()).setString(2, "warrior");
        verify(mockConnection).commit();
    }

    @Test
    @DisplayName("Test GetIdentityKey utility rigor")
    void testGetIdentityKey() throws SQLException {
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

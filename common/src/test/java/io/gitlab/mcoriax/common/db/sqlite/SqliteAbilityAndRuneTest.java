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
 * Ability and rune catalog/equipped-slot tests for SQLite.
 */
class SqliteAbilityAndRuneTest extends SqliteTestSupport {

    @Test
    @DisplayName("Test CreateAbility utility rigor")
    void testCreateAbilityRigorous() throws SQLException {
        CreateAbility.invoke(mockConnection, "fireball", "Fireball", AbilityType.ACTIVE, true);
        verify(mockConnection).prepareStatement(eq("INSERT OR IGNORE INTO identity_abilities (system_code, display_name, ability_type, is_global) VALUES (?, ?, ?, ?)"));
        verify(mockStatement).setString(1, "fireball");
        verify(mockStatement).setString(2, "Fireball");
        verify(mockStatement).setString(3, "ACTIVE");
        verify(mockStatement).setBoolean(4, true);
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test GetAbilities utility rigor")
    void testGetAbilitiesRigorous() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("system_code")).thenReturn("fireball");
        when(mockResultSet.getString("display_name")).thenReturn("Fireball");
        when(mockResultSet.getString("ability_type")).thenReturn("ACTIVE");
        when(mockResultSet.getBoolean("is_global")).thenReturn(true);

        Collection<Ability> result = GetAbilities.invoke(mockConnection);
        assertFalse(result.isEmpty());
        verify(mockConnection).prepareStatement(eq("SELECT system_code, display_name, ability_type, is_global FROM identity_abilities"));
    }

    @Test
    @DisplayName("Test SetEquippedAbility utility rigor")
    void testSetEquippedAbilityRigorous() throws SQLException {
        SetEquippedAbility.invoke(mockConnection, identityId, 2, "blink", "m1");
        verify(mockConnection).prepareStatement(eq("INSERT OR REPLACE INTO identity_equipped_abilities (identity_uuid, ability_id, slot_number, model_id) " +
                         "VALUES (?, (SELECT id FROM identity_abilities WHERE system_code = ?), ?, ?)"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).setString(2, "blink");
        verify(mockStatement).setInt(3, 2);
        verify(mockStatement).setString(4, "m1");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test GetEquippedAbilities utility rigor")
    void testGetEquippedAbilitiesRigorous() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("slot_number")).thenReturn(1);
        when(mockResultSet.getString("system_code")).thenReturn("blink");
        when(mockResultSet.getString("model_id")).thenReturn("model_x");

        Map<Integer, EquippedAbility> result = GetEquippedAbilities.invoke(mockConnection, identityId);
        assertNotNull(result.get(1));
        assertEquals("blink", result.get(1).systemCode());

        verify(mockConnection).prepareStatement(eq("SELECT a.system_code, ia.slot_number, ia.model_id FROM identity_equipped_abilities ia JOIN identity_abilities a ON ia.ability_id = a.id WHERE ia.identity_uuid = ?"));
        verify(mockStatement).setString(1, identityId.toString());
    }

    @Test
    @DisplayName("Test SetRune utility rigor")
    void testSetRuneRigorous() throws SQLException {
        SetRune.invoke(mockConnection, identityId, 1, "speed");
        verify(mockConnection).prepareStatement(eq("INSERT OR REPLACE INTO identity_equipped_runes (identity_uuid, rune_id, slot_number) " +
                         "VALUES (?, (SELECT id FROM identity_runes WHERE system_code = ?), ?)"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).setString(2, "speed");
        verify(mockStatement).setInt(3, 1);
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test GetRunes utility rigor")
    void testGetRunesRigorous() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("system_code")).thenReturn("haste");

        Collection<Rune> result = GetRunes.invoke(mockConnection);
        assertFalse(result.isEmpty());
        verify(mockConnection).prepareStatement(eq("SELECT system_code, display_name FROM identity_runes"));
    }
}

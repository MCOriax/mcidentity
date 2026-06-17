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
 * Ability and rune catalog/equipped-slot tests for PostgreSQL.
 */
class PostgresAbilityAndRuneTest extends PostgresTestSupport {

    @Test
    @DisplayName("Test CreateAbility utility rigor")
    void testCreateAbility() throws SQLException {
        CreateAbility.invoke(mockConnection, "slash", "Slash Attack", AbilityType.ACTIVE, false);
        verify(mockConnection).prepareStatement(eq("INSERT INTO identity_abilities (system_code, display_name, ability_type, is_global) VALUES (?, ?, ?::ability_type_enum, ?) ON CONFLICT (system_code) DO NOTHING"));
        verify(mockStatement).setString(1, "slash");
        verify(mockStatement).setString(2, "Slash Attack");
        verify(mockStatement).setString(3, "ACTIVE");
        verify(mockStatement).setBoolean(4, false);
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test GetAbilities utility rigor")
    void testGetAbilities() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("system_code")).thenReturn("fireball");
        when(mockResultSet.getString("display_name")).thenReturn("Fireball");
        when(mockResultSet.getString("ability_type")).thenReturn("ACTIVE");

        Collection<Ability> result = GetAbilities.invoke(mockConnection);
        assertFalse(result.isEmpty());
        verify(mockConnection).prepareStatement(eq("SELECT system_code, display_name, ability_type, is_global FROM identity_abilities"));
    }

    @Test
    @DisplayName("Test SetEquippedAbility utility rigor")
    void testSetEquippedAbility() throws SQLException {
        SetEquippedAbility.invoke(mockConnection, identityId, 1, "fireball", "item_model");
        verify(mockConnection).prepareStatement(eq("INSERT INTO identity_equipped_abilities (identity_uuid, ability_id, slot_number, model_id) " +
                         "VALUES (?, (SELECT id FROM identity_abilities WHERE system_code = ?), ?, ?) " +
                         "ON CONFLICT (identity_uuid, slot_number) DO UPDATE SET ability_id = EXCLUDED.ability_id, model_id = EXCLUDED.model_id"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).setString(2, "fireball");
        verify(mockStatement).setInt(3, 1);
        verify(mockStatement).setString(4, "item_model");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test GetEquippedAbilities utility rigor")
    void testGetEquippedAbilities() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("slot_number")).thenReturn(1);
        when(mockResultSet.getString("system_code")).thenReturn("blink");
        when(mockResultSet.getString("model_id")).thenReturn("m1");

        Map<Integer, EquippedAbility> result = GetEquippedAbilities.invoke(mockConnection, identityId);
        assertFalse(result.isEmpty());
        assertEquals("blink", result.get(1).systemCode());

        verify(mockConnection).prepareStatement(eq("SELECT a.system_code, ia.slot_number, ia.model_id FROM identity_equipped_abilities ia JOIN identity_abilities a ON ia.ability_id = a.id WHERE ia.identity_uuid = ?"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).executeQuery();
    }

    @Test
    @DisplayName("Test SetRune utility rigor")
    void testSetRune() throws SQLException {
        SetRune.invoke(mockConnection, identityId, 1, "haste");
        verify(mockConnection).prepareStatement(eq("INSERT INTO identity_equipped_runes (identity_uuid, rune_id, slot_number) " +
                         "VALUES (?, (SELECT id FROM identity_runes WHERE system_code = ?), ?) " +
                         "ON CONFLICT (identity_uuid, slot_number) DO UPDATE SET rune_id = EXCLUDED.rune_id"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).setString(2, "haste");
        verify(mockStatement).setInt(3, 1);
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test GetRunes utility rigor")
    void testGetRunes() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("system_code")).thenReturn("haste");

        Collection<Rune> result = GetRunes.invoke(mockConnection);
        assertFalse(result.isEmpty());
        verify(mockConnection).prepareStatement(eq("SELECT system_code, display_name FROM identity_runes"));
    }
}

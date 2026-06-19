package io.github.mcoriax.common.db.postgresql;

import io.github.mcoriax.api.model.*;
import io.github.mcoriax.common.db.postgresql.utils.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Session, cosmetics and action-logging tests for PostgreSQL.
 */
class PostgresSessionAndCosmeticsTest extends PostgresTestSupport {

    @Test
    @DisplayName("Test GetSession utility rigor")
    void testGetSession() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("identity_uuid")).thenReturn(identityId.toString());
        when(mockResultSet.getString("content_key")).thenReturn("some-key");

        Optional<IdentitySession> result = GetSession.invoke(mockConnection, playerId);

        assertTrue(result.isPresent());
        assertEquals(identityId, result.get().getIdentityUniqueId());

        verify(mockConnection).prepareStatement(eq("SELECT identity_uuid, content_key FROM identity_sessions WHERE player_uuid = ?"));
        verify(mockStatement).setString(1, playerId.toString());
    }

    @Test
    @DisplayName("Test SetActiveIdentity utility rigor")
    void testSetActiveIdentity() throws SQLException {
        SetActiveIdentity.invoke(mockConnection, playerId, identityId, "content-data");
        verify(mockConnection).prepareStatement(eq("INSERT INTO identity_sessions (player_uuid, identity_uuid, content_key) VALUES (?, ?, ?) ON CONFLICT (player_uuid) DO UPDATE SET identity_uuid = EXCLUDED.identity_uuid, content_key = EXCLUDED.content_key"));
        verify(mockStatement).setString(1, playerId.toString());
        verify(mockStatement).setString(2, identityId.toString());
        verify(mockStatement).setString(3, "content-data");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test GetActiveIdentity utility rigor")
    void testGetActiveIdentity() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("identity_uuid")).thenReturn(identityId.toString());

        Optional<UUID> result = GetActiveIdentity.invoke(mockConnection, playerId);
        assertTrue(result.isPresent());
        assertEquals(identityId, result.get());

        verify(mockConnection).prepareStatement(eq("SELECT identity_uuid FROM identity_sessions WHERE player_uuid = ?"));
        verify(mockStatement).setString(1, playerId.toString());
        verify(mockStatement).executeQuery();
    }

    @Test
    @DisplayName("Test UpdateSessionContentKey utility rigor")
    void testUpdateSessionContentKey() throws SQLException {
        UpdateSessionContentKey.invoke(mockConnection, playerId, "new-key");
        verify(mockConnection).prepareStatement(eq("UPDATE identity_sessions SET content_key = ? WHERE player_uuid = ?"));
        verify(mockStatement).setString(1, "new-key");
        verify(mockStatement).setObject(2, playerId);
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test SaveSkin utility rigor")
    void testSaveSkin() throws SQLException {
        SaveSkin.invoke(mockConnection, identityId, "skin-base64");
        verify(mockConnection).prepareStatement(eq("INSERT INTO identity_equipped_cosmetics (identity_uuid, skin) VALUES (?, ?) ON CONFLICT (identity_uuid) DO UPDATE SET skin = EXCLUDED.skin"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).setString(2, "skin-base64");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test SaveAura utility rigor")
    void testSaveAura() throws SQLException {
        SaveAura.invoke(mockConnection, identityId, "fire-aura");
        verify(mockConnection).prepareStatement(eq("INSERT INTO identity_equipped_cosmetics (identity_uuid, aura) VALUES (?, ?) ON CONFLICT (identity_uuid) DO UPDATE SET aura = EXCLUDED.aura"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).setString(2, "fire-aura");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test GetCosmetics utility rigor")
    void testGetCosmetics() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("skin")).thenReturn("skin-val");
        when(mockResultSet.getString("aura")).thenReturn("aura-val");

        Map<String, String> result = GetCosmetics.invoke(mockConnection, identityId);
        assertEquals("skin-val", result.get("skin"));
        assertEquals("aura-val", result.get("aura"));

        verify(mockConnection).prepareStatement(eq("SELECT skin, aura FROM identity_equipped_cosmetics WHERE identity_uuid = ?"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).executeQuery();
    }

    @Test
    @DisplayName("Test LogAction utility rigor")
    void testLogAction() throws SQLException {
        LogAction.invoke(mockConnection, playerId, identityId, ActionType.EQUIP);
        verify(mockConnection).prepareStatement(eq("INSERT INTO identity_logs (player_uuid, identity_uuid, action_type) VALUES (?, ?, ?::action_type_enum)"));
        verify(mockStatement).setString(1, playerId.toString());
        verify(mockStatement).setString(2, identityId.toString());
        verify(mockStatement).setString(3, "EQUIP");
        verify(mockStatement).executeUpdate();
    }
}

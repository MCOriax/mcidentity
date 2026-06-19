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
 * Session, cosmetics and action-logging tests for SQLite.
 */
class SqliteSessionAndCosmeticsTest extends SqliteTestSupport {

    @Test
    @DisplayName("Test GetSession utility rigor")
    void testGetSessionRigorous() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("identity_uuid")).thenReturn(identityId.toString());
        when(mockResultSet.getString("content_key")).thenReturn("serialized_item");

        Optional<IdentitySession> result = GetSession.invoke(mockConnection, playerId);

        assertTrue(result.isPresent());
        assertEquals(identityId, result.get().getIdentityUniqueId());
        assertEquals("serialized_item", result.get().getContentKey());

        verify(mockConnection).prepareStatement(eq("SELECT identity_uuid, content_key FROM identity_sessions WHERE player_uuid = ?"));
        verify(mockStatement).setString(1, playerId.toString());
    }

    @Test
    @DisplayName("Test SetActiveIdentity utility rigor")
    void testSetActiveIdentityRigorous() throws SQLException {
        SetActiveIdentity.invoke(mockConnection, playerId, identityId, "new_content");
        verify(mockConnection).prepareStatement(eq("INSERT OR REPLACE INTO identity_sessions (player_uuid, identity_uuid, content_key) VALUES (?, ?, ?)"));
        verify(mockStatement).setString(1, playerId.toString());
        verify(mockStatement).setString(2, identityId.toString());
        verify(mockStatement).setString(3, "new_content");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test GetActiveIdentity utility rigor")
    void testGetActiveIdentityRigorous() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("identity_uuid")).thenReturn(identityId.toString());

        Optional<UUID> result = GetActiveIdentity.invoke(mockConnection, playerId);
        assertTrue(result.isPresent());
        assertEquals(identityId, result.get());

        verify(mockConnection).prepareStatement(eq("SELECT identity_uuid FROM identity_sessions WHERE player_uuid = ?"));
        verify(mockStatement).setString(1, playerId.toString());
    }

    @Test
    @DisplayName("Test UpdateSessionContentKey utility rigor")
    void testUpdateSessionContentKeyRigorous() throws SQLException {
        UpdateSessionContentKey.invoke(mockConnection, playerId, "updated_key");
        verify(mockConnection).prepareStatement(eq("UPDATE identity_sessions SET content_key = ? WHERE player_uuid = ?"));
        verify(mockStatement).setString(1, "updated_key");
        verify(mockStatement).setString(2, playerId.toString());
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test SaveSkin utility rigor")
    void testSaveSkinRigorous() throws SQLException {
        SaveSkin.invoke(mockConnection, identityId, "skin_tex");
        verify(mockConnection).prepareStatement(eq("INSERT INTO identity_equipped_cosmetics (identity_uuid, skin) VALUES (?, ?) ON CONFLICT (identity_uuid) DO UPDATE SET skin = EXCLUDED.skin"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).setString(2, "skin_tex");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test SaveAura utility rigor")
    void testSaveAuraRigorous() throws SQLException {
        SaveAura.invoke(mockConnection, identityId, "aura_code");
        verify(mockConnection).prepareStatement(eq("INSERT INTO identity_equipped_cosmetics (identity_uuid, aura) VALUES (?, ?) ON CONFLICT (identity_uuid) DO UPDATE SET aura = EXCLUDED.aura"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).setString(2, "aura_code");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test GetCosmetics utility rigor")
    void testGetCosmeticsRigorous() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("skin")).thenReturn("skin_val");
        when(mockResultSet.getString("aura")).thenReturn("aura_val");

        Map<String, String> result = GetCosmetics.invoke(mockConnection, identityId);
        assertEquals("skin_val", result.get("skin"));
        assertEquals("aura_val", result.get("aura"));

        verify(mockConnection).prepareStatement(eq("SELECT skin, aura FROM identity_equipped_cosmetics WHERE identity_uuid = ?"));
        verify(mockStatement).setString(1, identityId.toString());
    }

    @Test
    @DisplayName("Test LogAction utility rigor")
    void testLogActionRigorous() throws SQLException {
        LogAction.invoke(mockConnection, playerId, identityId, ActionType.UNEQUIP);
        verify(mockConnection).prepareStatement(eq("INSERT INTO identity_logs (player_uuid, identity_uuid, action_type) VALUES (?, ?, ?)"));
        verify(mockStatement).setString(1, playerId.toString());
        verify(mockStatement).setString(2, identityId.toString());
        verify(mockStatement).setString(3, "UNEQUIP");
        verify(mockStatement).executeUpdate();
    }
}

package io.gitlab.mcoriax.common.db.sqlite;

import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.common.db.sqlite.utils.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MCIdentitySQLiteTest {

    private Connection mockConnection;
    private PreparedStatement mockStatement;
    private ResultSet mockResultSet;
    private UUID identityId;
    private UUID playerId;
    private TestMCIdentitySQLite provider;

    // Concrete subclass for testing the abstract MCIdentitySQLite
    static class TestMCIdentitySQLite extends MCIdentitySQLite {
        private final Connection connection;

        public TestMCIdentitySQLite(Connection connection) {
            super("dummy.db");
            this.connection = connection;
        }

        @Override
        protected Connection getConnection() throws SQLException {
            return connection;
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        mockConnection = mock(Connection.class);
        mockStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);
        identityId = UUID.randomUUID();
        playerId = UUID.randomUUID();
        provider = new TestMCIdentitySQLite(mockConnection);

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
    }

    // --- Main Provider Tests ---

    @Test
    @DisplayName("Test MCIdentitySQLite provider method calls")
    void testProviderMethods() throws Exception {
        provider.initializeSchema().get();
        verify(mockConnection).prepareStatement(contains("CREATE TABLE"));

        provider.createProfession("warrior", "Warrior").get();
        verify(mockConnection).prepareStatement(contains("INSERT INTO professions"));

        provider.addStat(identityId, "strength", 10).get();
        verify(mockConnection).prepareStatement(contains("UPDATE identity_stats SET strength = strength + ?"));
    }

    // --- Utility Tests ---

    @Test
    @DisplayName("Test GetIdentityKey utility")
    void testGetIdentityKey() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("profession")).thenReturn("warrior");

        Optional<IdentityKey> result = GetIdentityKey.invoke(mockConnection, identityId);

        assertTrue(result.isPresent());
        assertEquals(identityId, result.get().getUniqueId());
        assertEquals("warrior", result.get().getProfessionCode());

        verify(mockConnection).prepareStatement(contains("SELECT"));
        verify(mockStatement).setString(1, identityId.toString());
    }

    @Test
    @DisplayName("Test UpdateProperties utility")
    void testUpdateProperties() throws SQLException {
        IdentityProperties props = mock(IdentityProperties.class);
        when(props.getLevel()).thenReturn(10);
        when(props.getExp()).thenReturn(500);
        when(props.getStatPoints()).thenReturn(5);
        when(props.getHeadText()).thenReturn("head-texture");
        when(props.getInventoryContents()).thenReturn("inv-data");
        when(props.getEnderchestContents()).thenReturn("ec-data");

        UpdateProperties.invoke(mockConnection, identityId, props);

        verify(mockConnection).prepareStatement(contains("UPDATE identity_properties"));
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
    @DisplayName("Test CreateAbility utility")
    void testCreateAbility() throws SQLException {
        CreateAbility.invoke(mockConnection, "slash", "Slash Attack", AbilityType.ACTIVE, false);
        verify(mockConnection).prepareStatement(contains("INSERT OR IGNORE INTO abilities"));
        verify(mockStatement).setString(1, "slash");
        verify(mockStatement).setString(2, "Slash Attack");
        verify(mockStatement).setString(3, "ACTIVE");
        verify(mockStatement).setBoolean(4, false);
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test AddStat utility")
    void testAddStat() throws SQLException {
        AddStat.invoke(mockConnection, identityId, "health", 20);
        verify(mockConnection).prepareStatement(contains("UPDATE identity_stats SET health = health + ?"));
        verify(mockStatement).setInt(1, 20);
        verify(mockStatement).setString(2, identityId.toString());
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test GetStats utility")
    void testGetStats() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("energy")).thenReturn(100);
        when(mockResultSet.getInt("health")).thenReturn(200);

        Optional<IdentityStats> result = GetStats.invoke(mockConnection, identityId);

        assertTrue(result.isPresent());
        assertEquals(100, result.get().getEnergy());
        assertEquals(200, result.get().getHealth());
    }

    @Test
    @DisplayName("Test GetSession utility")
    void testGetSession() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("identity_uuid")).thenReturn(identityId.toString());
        when(mockResultSet.getString("profession_code")).thenReturn("mage");
        when(mockResultSet.getString("content_key")).thenReturn("some-key");

        Optional<IdentitySession> result = GetSession.invoke(mockConnection, playerId);

        assertTrue(result.isPresent());
        assertEquals(identityId, result.get().identityUuid());
        assertEquals("mage", result.get().identityKey().getProfessionCode());
    }

    @Test
    @DisplayName("Test SetActiveIdentity utility")
    void testSetActiveIdentity() throws SQLException {
        SetActiveIdentity.invoke(mockConnection, playerId, identityId, "content-data");
        verify(mockConnection).prepareStatement(contains("REPLACE INTO identity_sessions"));
        verify(mockStatement).setString(1, playerId.toString());
        verify(mockStatement).setString(2, identityId.toString());
        verify(mockStatement).setString(3, "content-data");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test GetProfessions utility")
    void testGetProfessions() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("system_code")).thenReturn("warrior");
        when(mockResultSet.getString("display_name")).thenReturn("Warrior");

        Collection<Profession> result = GetProfessions.invoke(mockConnection);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("warrior", result.iterator().next().getSystemCode());
    }

    @Test
    @DisplayName("Test SetEquippedAbility utility")
    void testSetEquippedAbility() throws SQLException {
        SetEquippedAbility.invoke(mockConnection, identityId, 1, "fireball", "item_model");
        verify(mockConnection).prepareStatement(contains("REPLACE INTO identity_abilities"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).setInt(2, 1);
        verify(mockStatement).setString(3, "fireball");
        verify(mockStatement).setString(4, "item_model");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test LogAction utility")
    void testLogAction() throws SQLException {
        LogAction.invoke(mockConnection, playerId, identityId, ActionType.EQUIP);
        verify(mockConnection).prepareStatement(contains("INSERT INTO identity_logs"));
        verify(mockStatement).setString(1, playerId.toString());
        verify(mockStatement).setString(2, identityId.toString());
        verify(mockStatement).setString(3, "EQUIP");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test SaveSkin utility")
    void testSaveSkin() throws SQLException {
        SaveSkin.invoke(mockConnection, identityId, "skin-base64");
        verify(mockConnection).prepareStatement(contains("REPLACE INTO identity_cosmetics"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).setString(2, "skin");
        verify(mockStatement).setString(3, "skin-base64");
        verify(mockStatement).executeUpdate();
    }

    // --- Edge Cases ---

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

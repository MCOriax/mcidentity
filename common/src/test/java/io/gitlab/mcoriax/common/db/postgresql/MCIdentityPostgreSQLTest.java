package io.gitlab.mcoriax.common.db.postgresql;

import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.common.db.postgresql.utils.*;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MCIdentityPostgreSQLTest {

    private Connection mockConnection;
    private PreparedStatement mockStatement;
    private ResultSet mockResultSet;
    private UUID identityId;
    private UUID playerId;
    private TestMCIdentityPostgreSQL provider;

    // Concrete subclass for testing the abstract MCIdentityPostgreSQL
    static class TestMCIdentityPostgreSQL extends MCIdentityPostgreSQL {
        private final Connection connection;

        public TestMCIdentityPostgreSQL(Connection connection) {
            super("localhost", 5432, "mcidentity", "postgres", "password");
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
        provider = new TestMCIdentityPostgreSQL(mockConnection);

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockStatement.executeUpdate()).thenReturn(1);
    }

    // --- Main Provider Tests ---

    @Test
    @DisplayName("Test MCIdentityPostgreSQL provider method calls")
    void testProviderMethods() throws Exception {
        provider.initializeSchema().get();
        verify(mockConnection, atLeastOnce()).prepareStatement(anyString());

        provider.createProfession("warrior", "Warrior").get();
        verify(mockConnection).prepareStatement(contains("INSERT"));

        provider.addStat(identityId, "strength", 10).get();
        verify(mockConnection).prepareStatement(contains("UPDATE identity_stats SET strength = strength + ?"));
    }

    // --- Utility Tests ---

    @Test
    @DisplayName("Test InitializeSchema utility")
    void testInitializeSchema() throws SQLException {
        InitializeSchema.invoke(mockConnection);
        verify(mockConnection, atLeastOnce()).prepareStatement(anyString());
    }

    @Test
    @DisplayName("Test CreateProfession utility")
    void testCreateProfession() throws SQLException {
        CreateProfession.invoke(mockConnection, "mage", "Mage Class");
        verify(mockConnection).prepareStatement(contains("INSERT INTO professions"));
        verify(mockStatement).setString(1, "mage");
        verify(mockStatement).setString(2, "Mage Class");
    }

    @Test
    @DisplayName("Test CreateIdentity utility")
    void testCreateIdentity() throws SQLException {
        IdentityKey key = mock(IdentityKey.class);
        when(key.getUniqueId()).thenReturn(identityId);
        when(key.getProfessionCode()).thenReturn("warrior");
        IdentityProperties props = mock(IdentityProperties.class);
        IdentityStats stats = mock(IdentityStats.class);

        CreateIdentity.invoke(mockConnection, key, props, stats);

        verify(mockConnection, atLeastOnce()).prepareStatement(anyString());
        verify(mockConnection).commit();
    }

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
        verify(mockStatement).setObject(1, identityId);
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
        verify(mockStatement).setObject(7, identityId);
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test CreateAbility utility")
    void testCreateAbility() throws SQLException {
        CreateAbility.invoke(mockConnection, "slash", "Slash Attack", AbilityType.ACTIVE, false);
        verify(mockConnection).prepareStatement(contains("INSERT INTO abilities"));
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
        verify(mockStatement).setObject(2, identityId);
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
    @DisplayName("Test GetStat utility")
    void testGetStat() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(50);

        int result = GetStat.invoke(mockConnection, identityId, "strength");

        assertEquals(50, result);
        verify(mockConnection).prepareStatement(contains("SELECT strength"));
    }

    @Test
    @DisplayName("Test GetSession utility")
    void testGetSession() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getObject("identity_uuid")).thenReturn(identityId);
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
        verify(mockConnection).prepareStatement(contains("INSERT INTO identity_sessions"));
        verify(mockStatement).setObject(1, playerId);
        verify(mockStatement).setObject(2, identityId);
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
        verify(mockConnection).prepareStatement(contains("INSERT INTO identity_abilities"));
        verify(mockStatement).setObject(1, identityId);
        verify(mockStatement).setInt(2, 1);
        verify(mockStatement).setString(3, "fireball");
        verify(mockStatement).setString(4, "item_model");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test SetRune utility")
    void testSetRune() throws SQLException {
        SetRune.invoke(mockConnection, identityId, 1, "haste");
        verify(mockConnection).prepareStatement(contains("INSERT INTO identity_runes"));
        verify(mockStatement).setObject(1, identityId);
        verify(mockStatement).setInt(2, 1);
        verify(mockStatement).setString(3, "haste");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test LogAction utility")
    void testLogAction() throws SQLException {
        LogAction.invoke(mockConnection, playerId, identityId, ActionType.EQUIP);
        verify(mockConnection).prepareStatement(contains("INSERT INTO identity_logs"));
        verify(mockStatement).setObject(1, playerId);
        verify(mockStatement).setObject(2, identityId);
        verify(mockStatement).setString(3, "EQUIP");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test SaveSkin utility")
    void testSaveSkin() throws SQLException {
        SaveSkin.invoke(mockConnection, identityId, "skin-base64");
        verify(mockConnection).prepareStatement(contains("INSERT INTO identity_cosmetics"));
        verify(mockStatement).setObject(1, identityId);
        verify(mockStatement).setString(2, "skin");
        verify(mockStatement).setString(3, "skin-base64");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test SaveAura utility")
    void testSaveAura() throws SQLException {
        SaveAura.invoke(mockConnection, identityId, "fire-aura");
        verify(mockConnection).prepareStatement(contains("INSERT INTO identity_cosmetics"));
        verify(mockStatement).setObject(1, identityId);
        verify(mockStatement).setString(2, "aura");
        verify(mockStatement).setString(3, "fire-aura");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test GetAbilities utility")
    void testGetAbilities() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("system_code")).thenReturn("fireball");
        
        Collection<Ability> result = GetAbilities.invoke(mockConnection);
        assertFalse(result.isEmpty());
        verify(mockConnection).prepareStatement(contains("SELECT system_code"));
    }

    @Test
    @DisplayName("Test GetRunes utility")
    void testGetRunes() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("system_code")).thenReturn("haste");
        
        Collection<Rune> result = GetRunes.invoke(mockConnection);
        assertFalse(result.isEmpty());
        verify(mockConnection).prepareStatement(contains("SELECT system_code"));
    }

    @Test
    @DisplayName("Test GetEquippedAbilities utility")
    void testGetEquippedAbilities() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("slot_number")).thenReturn(1);
        when(mockResultSet.getString("system_code")).thenReturn("blink");
        
        Map<Integer, EquippedAbility> result = GetEquippedAbilities.invoke(mockConnection, identityId);
        assertFalse(result.isEmpty());
        assertEquals("blink", result.get(1).abilityCode());
    }

    @Test
    @DisplayName("Test GetCosmetics utility")
    void testGetCosmetics() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getString("type")).thenReturn("skin", "aura");
        when(mockResultSet.getString("value")).thenReturn("skin-val", "aura-val");
        
        Map<String, String> result = GetCosmetics.invoke(mockConnection, identityId);
        assertEquals("skin-val", result.get("skin"));
        assertEquals("aura-val", result.get("aura"));
    }

    @Test
    @DisplayName("Test GetActiveIdentity utility")
    void testGetActiveIdentity() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getObject("identity_uuid")).thenReturn(identityId);
        
        Optional<UUID> result = GetActiveIdentity.invoke(mockConnection, playerId);
        assertTrue(result.isPresent());
        assertEquals(identityId, result.get());
    }

    @Test
    @DisplayName("Test GetProperties utility")
    void testGetProperties() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("current_level")).thenReturn(5);
        
        Optional<IdentityProperties> result = GetProperties.invoke(mockConnection, identityId);
        assertTrue(result.isPresent());
        assertEquals(5, result.get().getLevel());
    }

    @Test
    @DisplayName("Test UpdateStats utility")
    void testUpdateStats() throws SQLException {
        IdentityStats stats = mock(IdentityStats.class);
        UpdateStats.invoke(mockConnection, identityId, stats);
        verify(mockConnection).prepareStatement(contains("UPDATE identity_stats"));
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test UpdateSessionContentKey utility")
    void testUpdateSessionContentKey() throws SQLException {
        UpdateSessionContentKey.invoke(mockConnection, playerId, "new-key");
        verify(mockConnection).prepareStatement(contains("UPDATE identity_sessions"));
        verify(mockStatement).setString(1, "new-key");
        verify(mockStatement).setObject(2, playerId);
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

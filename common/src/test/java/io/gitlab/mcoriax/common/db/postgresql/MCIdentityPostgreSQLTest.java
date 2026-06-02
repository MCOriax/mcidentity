package io.gitlab.mcoriax.common.db.postgresql;

import com.zaxxer.hikari.HikariDataSource;
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
            super((HikariDataSource) null);
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
        provider.initializeSchema().join();
        verify(mockConnection, atLeastOnce()).prepareStatement(anyString());

        provider.createProfession("warrior", "Warrior").join();
        verify(mockConnection).prepareStatement(eq("INSERT INTO professions (system_code, display_name) VALUES (?, ?) ON CONFLICT (system_code) DO NOTHING"));

        provider.addStat(identityId, "strength", 10).join();
        verify(mockConnection).prepareStatement(eq("UPDATE identity_stats SET strength = strength + ? WHERE identity_uuid = ?"));
    }

    // --- Utility Tests ---

    @Test
    @DisplayName("Test InitializeSchema utility rigor")
    void testInitializeSchema() throws SQLException {
        InitializeSchema.invoke(mockConnection);
        verify(mockConnection, atLeastOnce()).prepareStatement(contains("CREATE TABLE IF NOT EXISTS"));
    }

    @Test
    @DisplayName("Test CreateProfession utility rigor")
    void testCreateProfession() throws SQLException {
        CreateProfession.invoke(mockConnection, "mage", "Mage Class");
        verify(mockConnection).prepareStatement(eq("INSERT INTO professions (system_code, display_name) VALUES (?, ?) ON CONFLICT (system_code) DO NOTHING"));
        verify(mockStatement).setString(1, "mage");
        verify(mockStatement).setString(2, "Mage Class");
        verify(mockStatement).executeUpdate();
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

        verify(mockConnection).prepareStatement(eq("INSERT INTO professions (system_code, display_name) VALUES (?, ?) ON CONFLICT (system_code) DO NOTHING"));
        // 2. Identity Key insertion
        verify(mockConnection, atLeastOnce()).prepareStatement(eq("INSERT INTO identity_keys (uuid, profession_id) VALUES (?, (SELECT id FROM professions WHERE system_code = ?))"));
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

        verify(mockConnection).prepareStatement(eq("SELECT p.system_code as profession FROM identity_keys k JOIN professions p ON k.profession_id = p.id WHERE k.uuid = ?"));
        verify(mockStatement).setString(1, identityId.toString());
    }

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
    @DisplayName("Test CreateAbility utility rigor")
    void testCreateAbility() throws SQLException {
        CreateAbility.invoke(mockConnection, "slash", "Slash Attack", AbilityType.ACTIVE, false);
        verify(mockConnection).prepareStatement(eq("INSERT INTO abilities (system_code, display_name, ability_type, is_global) VALUES (?, ?, ?::ability_type_enum, ?) ON CONFLICT (system_code) DO NOTHING"));
        verify(mockStatement).setString(1, "slash");
        verify(mockStatement).setString(2, "Slash Attack");
        verify(mockStatement).setString(3, "ACTIVE");
        verify(mockStatement).setBoolean(4, false);
        verify(mockStatement).executeUpdate();
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
    @DisplayName("Test GetProfessions utility rigor")
    void testGetProfessions() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("system_code")).thenReturn("warrior");
        when(mockResultSet.getString("display_name")).thenReturn("Warrior");

        Collection<Profession> result = GetProfessions.invoke(mockConnection);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("warrior", result.iterator().next().getSystemCode());

        verify(mockConnection).prepareStatement(eq("SELECT system_code, display_name FROM professions"));
    }

    @Test
    @DisplayName("Test SetEquippedAbility utility rigor")
    void testSetEquippedAbility() throws SQLException {
        SetEquippedAbility.invoke(mockConnection, identityId, 1, "fireball", "item_model");
        verify(mockConnection).prepareStatement(eq("INSERT INTO identity_abilities (identity_uuid, ability_id, slot_number, model_id) " +
                         "VALUES (?, (SELECT id FROM abilities WHERE system_code = ?), ?, ?) " +
                         "ON CONFLICT (identity_uuid, slot_number) DO UPDATE SET ability_id = EXCLUDED.ability_id, model_id = EXCLUDED.model_id"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).setString(2, "fireball");
        verify(mockStatement).setInt(3, 1);
        verify(mockStatement).setString(4, "item_model");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test SetRune utility rigor")
    void testSetRune() throws SQLException {
        SetRune.invoke(mockConnection, identityId, 1, "haste");
        verify(mockConnection).prepareStatement(eq("INSERT INTO identity_runes (identity_uuid, rune_id, slot_number) " +
                         "VALUES (?, (SELECT id FROM runes WHERE system_code = ?), ?) " +
                         "ON CONFLICT (identity_uuid, slot_number) DO UPDATE SET rune_id = EXCLUDED.rune_id"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).setString(2, "haste");
        verify(mockStatement).setInt(3, 1);
        verify(mockStatement).executeUpdate();
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

    @Test
    @DisplayName("Test SaveSkin utility rigor")
    void testSaveSkin() throws SQLException {
        SaveSkin.invoke(mockConnection, identityId, "skin-base64");
        verify(mockConnection).prepareStatement(eq("INSERT INTO identity_cosmetics (identity_uuid, skin) VALUES (?, ?) ON CONFLICT (identity_uuid) DO UPDATE SET skin = EXCLUDED.skin"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).setString(2, "skin-base64");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test SaveAura utility rigor")
    void testSaveAura() throws SQLException {
        SaveAura.invoke(mockConnection, identityId, "fire-aura");
        verify(mockConnection).prepareStatement(eq("INSERT INTO identity_cosmetics (identity_uuid, aura) VALUES (?, ?) ON CONFLICT (identity_uuid) DO UPDATE SET aura = EXCLUDED.aura"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).setString(2, "fire-aura");
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
        verify(mockConnection).prepareStatement(eq("SELECT system_code, display_name, ability_type, is_global FROM abilities"));
    }

    @Test
    @DisplayName("Test GetRunes utility rigor")
    void testGetRunes() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("system_code")).thenReturn("haste");
        
        Collection<Rune> result = GetRunes.invoke(mockConnection);
        assertFalse(result.isEmpty());
        verify(mockConnection).prepareStatement(eq("SELECT system_code, display_name FROM runes"));
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

        verify(mockConnection).prepareStatement(eq("SELECT a.system_code, ia.slot_number, ia.model_id FROM identity_abilities ia JOIN abilities a ON ia.ability_id = a.id WHERE ia.identity_uuid = ?"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).executeQuery();
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

        verify(mockConnection).prepareStatement(eq("SELECT skin, aura FROM identity_cosmetics WHERE identity_uuid = ?"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).executeQuery();
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

    @Test
    @DisplayName("Test UpdateSessionContentKey utility rigor")
    void testUpdateSessionContentKey() throws SQLException {
        UpdateSessionContentKey.invoke(mockConnection, playerId, "new-key");
        verify(mockConnection).prepareStatement(eq("UPDATE identity_sessions SET content_key = ? WHERE player_uuid = ?"));
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

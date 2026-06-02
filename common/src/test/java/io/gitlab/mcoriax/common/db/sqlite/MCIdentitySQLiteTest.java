package io.gitlab.mcoriax.common.db.sqlite;

import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.common.db.sqlite.utils.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

/**
 * Rigorous unit tests for MCIdentitySQLite and its database utility classes.
 * Ensures correct SQL execution and parameter binding for all database operations.
 */
class MCIdentitySQLiteTest {

    private Connection mockConnection;
    private PreparedStatement mockStatement;
    private ResultSet mockResultSet;
    private UUID identityId;
    private UUID playerId;
    private TestMCIdentitySQLite provider;

    /**
     * Concrete subclass of MCIdentitySQLite for testing abstract methods.
     */
    static class TestMCIdentitySQLite extends MCIdentitySQLite {
        private final Connection connection;

        public TestMCIdentitySQLite(Connection connection) {
            super("jdbc:sqlite::memory:");
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
        when(mockStatement.executeUpdate()).thenReturn(1);
    }

    // --- Main Provider Delegation Tests ---

    @Test
    @DisplayName("Test MCIdentitySQLite provider delegation logic")
    void testProviderDelegation() throws Exception {
        // Initialize Schema
        provider.initializeSchema().join();
        verify(mockConnection, atLeastOnce()).prepareStatement(contains("CREATE TABLE IF NOT EXISTS"));

        // Create Profession
        provider.createProfession("warrior", "Warrior Class").join();
        verify(mockConnection).prepareStatement(eq("INSERT OR IGNORE INTO professions (system_code, display_name) VALUES (?, ?)"));
        verify(mockStatement).setString(1, "warrior");
        verify(mockStatement).setString(2, "Warrior Class");

        // Add Stat
        provider.addStat(identityId, "strength", 15).join();
        verify(mockConnection).prepareStatement(eq("UPDATE identity_stats SET strength = strength + ? WHERE identity_uuid = ?"));
        verify(mockStatement).setInt(1, 15);
        verify(mockStatement).setString(2, identityId.toString());
    }

    // --- Utility Class Rigorous Tests ---

    @Test
    @DisplayName("Test InitializeSchema utility rigor")
    void testInitializeSchemaRigorous() throws SQLException {
        InitializeSchema.invoke(mockConnection);
        // Verify multiple table creations
        verify(mockConnection).prepareStatement(contains("CREATE TABLE IF NOT EXISTS professions"));
        verify(mockConnection).prepareStatement(contains("CREATE TABLE IF NOT EXISTS identity_keys"));
        verify(mockConnection).prepareStatement(contains("CREATE TABLE IF NOT EXISTS identity_properties"));
        verify(mockConnection).prepareStatement(contains("CREATE TABLE IF NOT EXISTS identity_stats"));
        verify(mockConnection, atLeast(5)).prepareStatement(anyString());
    }

    @Test
    @DisplayName("Test CreateProfession utility rigor")
    void testCreateProfessionRigorous() throws SQLException {
        CreateProfession.invoke(mockConnection, "mage", "Mage Class");
        verify(mockConnection).prepareStatement(eq("INSERT OR IGNORE INTO professions (system_code, display_name) VALUES (?, ?)"));
        verify(mockStatement).setString(1, "mage");
        verify(mockStatement).setString(2, "Mage Class");
        verify(mockStatement).executeUpdate();
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
        verify(mockConnection).prepareStatement(eq("INSERT OR IGNORE INTO professions (system_code, display_name) VALUES (?, ?)"));
        verify(mockStatement).setString(1, "archer");
        verify(mockStatement).setString(2, "archer");

        // 2. Identity Key insertion
        verify(mockConnection).prepareStatement(eq("INSERT INTO identity_keys (uuid, profession_id) VALUES (?, (SELECT id FROM professions WHERE system_code = ?))"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).setString(2, "archer");

        // 3. Properties insertion
        verify(mockConnection).prepareStatement(eq("INSERT INTO identity_properties (identity_uuid, current_level, exp, stat_points, head_text) VALUES (?, ?, ?, ?, ?)"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).setInt(2, 1);
        verify(mockStatement).setInt(3, 0);
        verify(mockStatement).setInt(4, 5);
        verify(mockStatement).setString(5, "base64_tex");

        // 4. Stats insertion
        verify(mockConnection).prepareStatement(eq("INSERT INTO identity_stats (identity_uuid, energy, health, food, strength, defense, wisdom, luck, agility, resilience) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"));
        verify(mockStatement, atLeastOnce()).setString(1, identityId.toString());
        verify(mockStatement).setInt(2, 100);
        verify(mockStatement).setInt(3, 250);
        verify(mockStatement).setInt(4, 50);
        verify(mockStatement).setInt(5, 10);
        verify(mockStatement).setInt(10, 4);

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

        verify(mockConnection).prepareStatement(eq("SELECT p.system_code as profession FROM identity_keys k JOIN professions p ON k.profession_id = p.id WHERE k.uuid = ?"));
        verify(mockStatement).setString(1, identityId.toString());
    }

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
        verify(mockStatement).setString(6, "ec-data");
        verify(mockStatement).setString(7, identityId.toString());
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test CreateAbility utility rigor")
    void testCreateAbilityRigorous() throws SQLException {
        CreateAbility.invoke(mockConnection, "fireball", "Fireball", AbilityType.ACTIVE, true);
        verify(mockConnection).prepareStatement(eq("INSERT OR IGNORE INTO abilities (system_code, display_name, ability_type, is_global) VALUES (?, ?, ?, ?)"));
        verify(mockStatement).setString(1, "fireball");
        verify(mockStatement).setString(2, "Fireball");
        verify(mockStatement).setString(3, "ACTIVE");
        verify(mockStatement).setBoolean(4, true);
        verify(mockStatement).executeUpdate();
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
    @DisplayName("Test GetStat utility rigor")
    void testGetStatRigorous() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(42);

        int result = GetStat.invoke(mockConnection, identityId, "luck");

        assertEquals(42, result);
        verify(mockConnection).prepareStatement(eq("SELECT luck FROM identity_stats WHERE identity_uuid = ?"));
        verify(mockStatement).setString(1, identityId.toString());
    }

    @Test
    @DisplayName("Test GetSession utility rigor")
    void testGetSessionRigorous() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("identity_uuid")).thenReturn(identityId.toString());
        when(mockResultSet.getString("profession_code")).thenReturn("mage");
        when(mockResultSet.getString("content_key")).thenReturn("serialized_item");

        Optional<IdentitySession> result = GetSession.invoke(mockConnection, playerId);

        assertTrue(result.isPresent());
        assertEquals(identityId, result.get().getIdentityUniqueId());
        assertEquals("serialized_item", result.get().getContentKey());

        verify(mockConnection).prepareStatement(eq("SELECT identity_uuid, profession_code, content_key FROM identity_sessions s JOIN identity_keys k ON s.identity_uuid = k.uuid JOIN professions p ON k.profession_id = p.id WHERE s.player_uuid = ?"));
        verify(mockStatement).setString(1, playerId.toString());
    }

    @Test
    @DisplayName("Test SetActiveIdentity utility rigor")
    void testSetActiveIdentityRigorous() throws SQLException {
        SetActiveIdentity.invoke(mockConnection, playerId, identityId, "new_content");
        verify(mockConnection).prepareStatement(eq("REPLACE INTO identity_sessions (player_uuid, identity_uuid, content_key) VALUES (?, ?, ?)"));
        verify(mockStatement).setString(1, playerId.toString());
        verify(mockStatement).setString(2, identityId.toString());
        verify(mockStatement).setString(3, "new_content");
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
        verify(mockConnection).prepareStatement(eq("SELECT id, system_code, display_name FROM professions"));
    }

    @Test
    @DisplayName("Test SetEquippedAbility utility rigor")
    void testSetEquippedAbilityRigorous() throws SQLException {
        SetEquippedAbility.invoke(mockConnection, identityId, 2, "blink", "m1");
        verify(mockConnection).prepareStatement(eq("REPLACE INTO identity_abilities (identity_uuid, slot_number, ability_id, model_id) VALUES (?, ?, (SELECT id FROM abilities WHERE system_code = ?), ?)"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).setInt(2, 2);
        verify(mockStatement).setString(3, "blink");
        verify(mockStatement).setString(4, "m1");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test SetRune utility rigor")
    void testSetRuneRigorous() throws SQLException {
        SetRune.invoke(mockConnection, identityId, 1, "speed");
        verify(mockConnection).prepareStatement(eq("REPLACE INTO identity_runes (identity_uuid, slot_number, rune_id) VALUES (?, ?, (SELECT id FROM runes WHERE system_code = ?))"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).setInt(2, 1);
        verify(mockStatement).setString(3, "speed");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test LogAction utility rigor")
    void testLogActionRigorous() throws SQLException {
        LogAction.invoke(mockConnection, playerId, identityId, ActionType.UNEQUIP);
        verify(mockConnection).prepareStatement(eq("INSERT INTO identity_logs (player_uuid, identity_uuid, action_type, timestamp) VALUES (?, ?, ?, ?)"));
        verify(mockStatement).setString(1, playerId.toString());
        verify(mockStatement).setString(2, identityId.toString());
        verify(mockStatement).setString(3, "UNEQUIP");
        verify(mockStatement, atLeastOnce()).setString(eq(4), anyString());
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test SaveSkin utility rigor")
    void testSaveSkinRigorous() throws SQLException {
        SaveSkin.invoke(mockConnection, identityId, "skin_tex");
        verify(mockConnection).prepareStatement(eq("REPLACE INTO identity_cosmetics (identity_uuid, type, value) VALUES (?, ?, ?)"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).setString(2, "skin");
        verify(mockStatement).setString(3, "skin_tex");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test SaveAura utility rigor")
    void testSaveAuraRigorous() throws SQLException {
        SaveAura.invoke(mockConnection, identityId, "aura_code");
        verify(mockConnection).prepareStatement(eq("REPLACE INTO identity_cosmetics (identity_uuid, type, value) VALUES (?, ?, ?)"));
        verify(mockStatement).setString(1, identityId.toString());
        verify(mockStatement).setString(2, "aura");
        verify(mockStatement).setString(3, "aura_code");
        verify(mockStatement).executeUpdate();
    }

    @Test
    @DisplayName("Test GetAbilities utility rigor")
    void testGetAbilitiesRigorous() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("system_code")).thenReturn("fireball");
        
        Collection<Ability> result = GetAbilities.invoke(mockConnection);
        assertFalse(result.isEmpty());
        verify(mockConnection).prepareStatement(eq("SELECT system_code, display_name, ability_type, is_global FROM abilities"));
    }

    @Test
    @DisplayName("Test GetRunes utility rigor")
    void testGetRunesRigorous() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("system_code")).thenReturn("haste");
        
        Collection<Rune> result = GetRunes.invoke(mockConnection);
        assertFalse(result.isEmpty());
        verify(mockConnection).prepareStatement(eq("SELECT system_code, display_name FROM runes"));
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

        verify(mockConnection).prepareStatement(eq("SELECT a.system_code, ia.slot_number, ia.model_id FROM identity_abilities ia JOIN abilities a ON ia.ability_id = a.id WHERE ia.identity_uuid = ?"));
        verify(mockStatement).setString(1, identityId.toString());
    }

    @Test
    @DisplayName("Test GetCosmetics utility rigor")
    void testGetCosmeticsRigorous() throws SQLException {
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getString("type")).thenReturn("skin", "aura");
        when(mockResultSet.getString("value")).thenReturn("skin_val", "aura-val");
        
        Map<String, String> result = GetCosmetics.invoke(mockConnection, identityId);
        assertEquals("skin_val", result.get("skin"));
        assertEquals("aura_val", result.get("aura"));

        verify(mockConnection).prepareStatement(eq("SELECT type, value FROM identity_cosmetics WHERE identity_uuid = ?"));
        verify(mockStatement).setString(1, identityId.toString());
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

    @Test
    @DisplayName("Test UpdateSessionContentKey utility rigor")
    void testUpdateSessionContentKeyRigorous() throws SQLException {
        UpdateSessionContentKey.invoke(mockConnection, playerId, "updated_key");
        verify(mockConnection).prepareStatement(eq("UPDATE identity_sessions SET content_key = ? WHERE player_uuid = ?"));
        verify(mockStatement).setString(1, "updated_key");
        verify(mockStatement).setString(2, playerId.toString());
        verify(mockStatement).executeUpdate();
    }

    // --- Edge Cases ---

    @Test
    @DisplayName("Test GetIdentityKey when SQLException occurs")
    void testGetIdentityKeyException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));
        Optional<IdentityKey> result = GetIdentityKey.invoke(mockConnection, identityId);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Test AddStat with invalid key logic")
    void testAddStatInvalidKey() throws SQLException {
        AddStat.invoke(mockConnection, identityId, "invalid_column", 10);
        // Utility has internal check for valid stat keys
        verify(mockConnection, never()).prepareStatement(anyString());
    }

    @Test
    @DisplayName("Test GetStats when record is missing")
    void testGetStatsMissing() throws SQLException {
        when(mockResultSet.next()).thenReturn(false);
        Optional<IdentityStats> result = GetStats.invoke(mockConnection, identityId);
        assertFalse(result.isPresent());
    }
}

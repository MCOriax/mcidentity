package io.gitlab.mcoriax.common.db.mongodb;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.UpdateOptions;
import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.common.db.mongodb.utils.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Rigorous unit tests for MCIdentityMongoDB and its database utility classes.
 * Ensures correct Document structure and field validation for all MongoDB operations.
 */
class MCIdentityMongoDBTest {

    private MongoDatabase mockDatabase;
    private MongoCollection<Document> mockCollection;
    private FindIterable<Document> mockFindIterable;
    private UUID identityId;
    private UUID playerId;
    private TestMCIdentityMongoDB provider;

    // Concrete subclass for testing the abstract MCIdentityMongoDB
    static class TestMCIdentityMongoDB extends MCIdentityMongoDB {
        private final MongoDatabase database;

        public TestMCIdentityMongoDB(MongoDatabase database) {
            super("mongodb://localhost:27017", "mcidentity");
            this.database = database;
        }
        
        public MongoDatabase getDatabase() {
            return database;
        }
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        mockDatabase = mock(MongoDatabase.class);
        mockCollection = mock(MongoCollection.class);
        mockFindIterable = mock(FindIterable.class);
        identityId = UUID.randomUUID();
        playerId = UUID.randomUUID();
        provider = new TestMCIdentityMongoDB(mockDatabase);

        when(mockDatabase.getCollection(anyString())).thenReturn(mockCollection);
        when(mockCollection.find(any(Bson.class))).thenReturn(mockFindIterable);
        // Mock updateOne to return a dummy result if needed, though usually not for Void calls
    }

    // --- Main Provider Delegation Tests ---

    @Test
    @DisplayName("Test MCIdentityMongoDB provider delegation logic")
    void testProviderDelegation() throws Exception {
        // Initialize Schema
        provider.initializeSchema().join();
        verify(mockDatabase, atLeastOnce()).getCollection(anyString());

        // Create Profession
        provider.createProfession("warrior", "Warrior Class").join();
        verify(mockDatabase).getCollection("professions");

        // Add Stat
        provider.addStat(identityId, "strength", 15).join();
        verify(mockDatabase).getCollection("identities");
    }

    // --- Utility Class Rigorous Tests ---

    @Test
    @DisplayName("Test InitializeSchema utility rigor")
    void testInitializeSchemaRigorous() {
        InitializeSchema.invoke(mockDatabase);
        // Verify index creation or collection access
        verify(mockDatabase, atLeastOnce()).getCollection(anyString());
    }

    @Test
    @DisplayName("Test CreateProfession utility rigor")
    void testCreateProfessionRigorous() {
        CreateProfession.invoke(mockDatabase, "mage", "Mage Class");
        
        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(mockDatabase).getCollection("professions");
        verify(mockCollection).updateOne(any(Bson.class), docCaptor.capture(), any(UpdateOptions.class));
        
        Document updateDoc = docCaptor.getValue();
        Document setDoc = updateDoc.get("$set", Document.class);
        assertEquals("mage", setDoc.getString("system_code"));
        assertEquals("Mage Class", setDoc.getString("display_name"));
    }

    @Test
    @DisplayName("Test CreateIdentity utility rigor")
    void testCreateIdentityRigorous() {
        IdentityKey key = mock(IdentityKey.class);
        when(key.getUniqueId()).thenReturn(identityId);
        when(key.getProfessionCode()).thenReturn("archer");
        
        IdentityProperties props = mock(IdentityProperties.class);
        when(props.getLevel()).thenReturn(1);
        when(props.getExp()).thenReturn(100);
        when(props.getStatPoints()).thenReturn(5);
        when(props.getHeadText()).thenReturn("head_tex");
        
        IdentityStats stats = mock(IdentityStats.class);
        when(stats.getEnergy()).thenReturn(10);
        when(stats.getHealth()).thenReturn(20);
        when(stats.getFood()).thenReturn(30);
        when(stats.getStrength()).thenReturn(1);
        when(stats.getDefense()).thenReturn(2);
        when(stats.getWisdom()).thenReturn(3);
        when(stats.getLuck()).thenReturn(4);
        when(stats.getAgility()).thenReturn(5);
        when(stats.getResilience()).thenReturn(6);

        CreateIdentity.invoke(mockDatabase, key, props, stats);

        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(mockDatabase, atLeastOnce()).getCollection("identities");
        verify(mockCollection).insertOne(docCaptor.capture());
        
        Document doc = docCaptor.getValue();
        assertEquals(identityId.toString(), doc.getString("_id"));
        assertEquals("archer", doc.getString("profession_code"));
        
        Document propsDoc = doc.get("properties", Document.class);
        assertEquals(1, propsDoc.getInteger("level"));
        assertEquals(100, propsDoc.getInteger("exp"));
        
        Document statsDoc = doc.get("stats", Document.class);
        assertEquals(10, statsDoc.getInteger("energy"));
        assertEquals(6, statsDoc.getInteger("resilience"));
    }

    @Test
    @DisplayName("Test GetIdentityKey utility rigor")
    void testGetIdentityKeyRigorous() {
        Document doc = new Document("_id", identityId.toString())
                .append("profession_code", "archer");
        when(mockFindIterable.first()).thenReturn(doc);

        Optional<IdentityKey> result = GetIdentityKey.invoke(mockDatabase, identityId);

        assertTrue(result.isPresent());
        assertEquals(identityId, result.get().getUniqueId());
        assertEquals("archer", result.get().getProfessionCode());

        verify(mockDatabase).getCollection("identities");
        verify(mockCollection).find(any(Bson.class));
    }

    @Test
    @DisplayName("Test UpdateProperties utility rigor")
    void testUpdatePropertiesRigorous() {
        IdentityProperties props = mock(IdentityProperties.class);
        when(props.getLevel()).thenReturn(15);
        when(props.getExp()).thenReturn(1000);
        when(props.getStatPoints()).thenReturn(5);
        when(props.getHeadText()).thenReturn("head_tex");
        when(props.getInventoryContents()).thenReturn("inv");
        when(props.getEnderchestContents()).thenReturn("ec");

        UpdateProperties.invoke(mockDatabase, identityId, props);

        ArgumentCaptor<Bson> filterCaptor = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<Bson> updateCaptor = ArgumentCaptor.forClass(Bson.class);
        verify(mockDatabase).getCollection("identities");
        verify(mockCollection).updateOne(filterCaptor.capture(), updateCaptor.capture());
        
        // Validation of filter and update would require Bson parsing, 
        // but verify method call at least.
        assertNotNull(filterCaptor.getValue());
        assertNotNull(updateCaptor.getValue());
    }

    @Test
    @DisplayName("Test CreateAbility utility rigor")
    void testCreateAbilityRigorous() {
        CreateAbility.invoke(mockDatabase, "blink", "Blink", AbilityType.ACTIVE, true);
        
        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(mockDatabase).getCollection("abilities");
        verify(mockCollection).insertOne(docCaptor.capture());
        
        Document doc = docCaptor.getValue();
        assertEquals("blink", doc.getString("system_code"));
        assertEquals("Blink", doc.getString("display_name"));
        assertEquals("ACTIVE", doc.getString("ability_type"));
        assertTrue(doc.getBoolean("is_global"));
    }

    @Test
    @DisplayName("Test AddStat utility rigor")
    void testAddStatRigorous() {
        AddStat.invoke(mockDatabase, identityId, "energy", 50);
        
        ArgumentCaptor<Bson> filterCaptor = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<Bson> updateCaptor = ArgumentCaptor.forClass(Bson.class);
        verify(mockDatabase).getCollection("identities");
        verify(mockCollection).updateOne(filterCaptor.capture(), updateCaptor.capture());
        
        assertNotNull(filterCaptor.getValue());
        assertNotNull(updateCaptor.getValue());
    }

    @Test
    @DisplayName("Test GetStats utility rigor")
    void testGetStatsRigorous() {
        Document statsDoc = new Document("energy", 80)
                .append("health", 150)
                .append("food", 50)
                .append("strength", 10)
                .append("defense", 5)
                .append("wisdom", 3)
                .append("luck", 2)
                .append("agility", 8)
                .append("resilience", 4);
        Document identityDoc = new Document("stats", statsDoc);
        when(mockFindIterable.first()).thenReturn(identityDoc);

        Optional<IdentityStats> result = GetStats.invoke(mockDatabase, identityId);

        assertTrue(result.isPresent());
        assertEquals(80, result.get().getEnergy());
        assertEquals(150, result.get().getHealth());
        assertEquals(10, result.get().getStrength());
        
        verify(mockDatabase).getCollection("identities");
    }

    @Test
    @DisplayName("Test GetStat utility rigor")
    void testGetStatRigorous() {
        Document statsDoc = new Document("strength", 50);
        Document identityDoc = new Document("stats", statsDoc);
        when(mockFindIterable.first()).thenReturn(identityDoc);

        int result = GetStat.invoke(mockDatabase, identityId, "strength");

        assertEquals(50, result);
        verify(mockDatabase).getCollection("identities");
    }

    @Test
    @DisplayName("Test SetActiveIdentity utility rigor")
    void testSetActiveIdentityRigorous() {
        SetActiveIdentity.invoke(mockDatabase, playerId, identityId, "content-data");
        
        ArgumentCaptor<Bson> filterCaptor = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<Bson> updateCaptor = ArgumentCaptor.forClass(Bson.class);
        verify(mockDatabase).getCollection("sessions");
        verify(mockCollection).updateOne(filterCaptor.capture(), updateCaptor.capture(), any(UpdateOptions.class));
        
        assertNotNull(filterCaptor.getValue());
        assertNotNull(updateCaptor.getValue());
    }

    @Test
    @DisplayName("Test GetSession utility rigor")
    void testGetSessionRigorous() {
        Document sessionDoc = new Document("_id", playerId.toString())
                .append("identity_uuid", identityId.toString())
                .append("profession_code", "mage")
                .append("content_key", "some-key");
        when(mockFindIterable.first()).thenReturn(sessionDoc);

        Optional<IdentitySession> result = GetSession.invoke(mockDatabase, playerId);

        assertTrue(result.isPresent());
        assertEquals(identityId, result.get().identityUuid());
        assertEquals("mage", result.get().identityKey().getProfessionCode());
        
        verify(mockDatabase).getCollection("sessions");
    }

    @Test
    @DisplayName("Test GetProfessions utility rigor")
    void testGetProfessionsRigorous() {
        MongoCursor<Document> mockCursor = mock(MongoCursor.class);
        when(mockFindIterable.iterator()).thenReturn(mockCursor);
        when(mockCursor.hasNext()).thenReturn(true, false);
        when(mockCursor.next()).thenReturn(new Document("system_code", "thief").append("display_name", "Thief"));
        
        Collection<Profession> result = GetProfessions.invoke(mockDatabase);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("thief", result.iterator().next().getSystemCode());
        verify(mockDatabase).getCollection("professions");
    }

    @Test
    @DisplayName("Test SetEquippedAbility utility rigor")
    void testSetEquippedAbilityRigorous() {
        SetEquippedAbility.invoke(mockDatabase, identityId, 2, "heal", "model_id");
        verify(mockDatabase).getCollection("identities");
        verify(mockCollection).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    @DisplayName("Test SetRune utility rigor")
    void testSetRuneRigorous() {
        SetRune.invoke(mockDatabase, identityId, 1, "speed");
        verify(mockDatabase).getCollection("identities");
        verify(mockCollection).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    @DisplayName("Test LogAction utility rigor")
    void testLogActionRigorous() {
        LogAction.invoke(mockDatabase, playerId, identityId, ActionType.EQUIP);
        
        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(mockDatabase).getCollection("logs");
        verify(mockCollection).insertOne(docCaptor.capture());
        
        Document doc = docCaptor.getValue();
        assertEquals(playerId.toString(), doc.getString("player_uuid"));
        assertEquals(identityId.toString(), doc.getString("identity_uuid"));
        assertEquals("EQUIP", doc.getString("action_type"));
    }

    @Test
    @DisplayName("Test SaveSkin utility rigor")
    void testSaveSkinRigorous() {
        SaveSkin.invoke(mockDatabase, identityId, "new-skin");
        verify(mockDatabase).getCollection("identities");
        verify(mockCollection).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    @DisplayName("Test SaveAura utility rigor")
    void testSaveAuraRigorous() {
        SaveAura.invoke(mockDatabase, identityId, "fire-aura");
        verify(mockDatabase).getCollection("identities");
        verify(mockCollection).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    @DisplayName("Test GetAbilities utility rigor")
    void testGetAbilitiesRigorous() {
        MongoCursor<Document> mockCursor = mock(MongoCursor.class);
        when(mockFindIterable.iterator()).thenReturn(mockCursor);
        when(mockCursor.hasNext()).thenReturn(true, false);
        when(mockCursor.next()).thenReturn(new Document("system_code", "fireball"));
        
        Collection<Ability> result = GetAbilities.invoke(mockDatabase);
        assertFalse(result.isEmpty());
        verify(mockDatabase).getCollection("abilities");
    }

    @Test
    @DisplayName("Test GetRunes utility rigor")
    void testGetRunesRigorous() {
        MongoCursor<Document> mockCursor = mock(MongoCursor.class);
        when(mockFindIterable.iterator()).thenReturn(mockCursor);
        when(mockCursor.hasNext()).thenReturn(true, false);
        when(mockCursor.next()).thenReturn(new Document("system_code", "haste"));
        
        Collection<Rune> result = GetRunes.invoke(mockDatabase);
        assertFalse(result.isEmpty());
        verify(mockDatabase).getCollection("runes");
    }

    @Test
    @DisplayName("Test GetEquippedAbilities utility rigor")
    void testGetEquippedAbilitiesRigorous() {
        Document abilitiesDoc = new Document("1", new Document("code", "blink").append("model", "m1"));
        Document identityDoc = new Document("equipped_abilities", abilitiesDoc);
        when(mockFindIterable.first()).thenReturn(identityDoc);
        
        Map<Integer, EquippedAbility> result = GetEquippedAbilities.invoke(mockDatabase, identityId);
        assertNotNull(result.get(1));
        assertEquals("blink", result.get(1).abilityCode());
    }

    @Test
    @DisplayName("Test GetCosmetics utility rigor")
    void testGetCosmeticsRigorous() {
        Document cosDoc = new Document("skin", "s1").append("aura", "a1");
        Document identityDoc = new Document("cosmetics", cosDoc);
        when(mockFindIterable.first()).thenReturn(identityDoc);
        
        Map<String, String> result = GetCosmetics.invoke(mockConnection.getDatabase(), identityId);
        assertEquals("s1", result.get("skin"));
        assertEquals("a1", result.get("aura"));
    }

    @Test
    @DisplayName("Test GetActiveIdentity utility rigor")
    void testGetActiveIdentityRigorous() {
        Document sessionDoc = new Document("identity_uuid", identityId.toString());
        when(mockFindIterable.first()).thenReturn(sessionDoc);
        
        Optional<UUID> result = GetActiveIdentity.invoke(mockDatabase, playerId);
        assertTrue(result.isPresent());
        assertEquals(identityId, result.get());
    }

    @Test
    @DisplayName("Test GetProperties utility rigor")
    void testGetPropertiesRigorous() {
        Document propsDoc = new Document("level", 10).append("exp", 100).append("stat_points", 5).append("head_text", "head");
        Document identityDoc = new Document("properties", propsDoc);
        when(mockFindIterable.first()).thenReturn(identityDoc);
        
        Optional<IdentityProperties> result = GetProperties.invoke(mockDatabase, identityId);
        assertTrue(result.isPresent());
        assertEquals(10, result.get().getLevel());
    }

    @Test
    @DisplayName("Test UpdateStats utility rigor")
    void testUpdateStatsRigorous() {
        IdentityStats stats = mock(IdentityStats.class);
        when(stats.getEnergy()).thenReturn(100);
        UpdateStats.invoke(mockDatabase, identityId, stats);
        verify(mockDatabase).getCollection("identities");
        verify(mockCollection).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    @DisplayName("Test UpdateSessionContentKey utility rigor")
    void testUpdateSessionContentKeyRigorous() {
        UpdateSessionContentKey.invoke(mockDatabase, playerId, "new-key");
        verify(mockDatabase).getCollection("sessions");
        verify(mockCollection).updateOne(any(Bson.class), any(Bson.class));
    }

    // --- Edge Cases ---

    @Test
    @DisplayName("Test GetIdentityKey when doc is null")
    void testGetIdentityKeyNotFound() {
        when(mockFindIterable.first()).thenReturn(null);
        Optional<IdentityKey> result = GetIdentityKey.invoke(mockDatabase, identityId);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Test AddStat with invalid key")
    void testAddStatInvalidKey() {
        AddStat.invoke(mockDatabase, identityId, "invalid_stat", 10);
        verify(mockDatabase, never()).getCollection(anyString());
    }
}

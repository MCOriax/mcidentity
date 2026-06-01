package io.gitlab.mcoriax.common.db.mongodb;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCursor;
import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.common.db.mongodb.utils.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
    }

    // --- Utility Tests ---

    @Test
    @DisplayName("Test InitializeSchema utility")
    void testInitializeSchema() {
        InitializeSchema.invoke(mockDatabase);
        // MongoDB initialize schema often creates indexes or does nothing if dynamic
        verify(mockDatabase, atLeastOnce()).getCollection(anyString());
    }

    @Test
    @DisplayName("Test CreateProfession utility")
    void testCreateProfession() {
        CreateProfession.invoke(mockDatabase, "mage", "Mage Class");
        verify(mockDatabase).getCollection("professions");
        verify(mockCollection).updateOne(any(Bson.class), any(Document.class), any());
    }

    @Test
    @DisplayName("Test CreateIdentity utility")
    void testCreateIdentity() {
        IdentityKey key = mock(IdentityKey.class);
        when(key.getUniqueId()).thenReturn(identityId);
        when(key.getProfessionCode()).thenReturn("warrior");
        IdentityProperties props = mock(IdentityProperties.class);
        IdentityStats stats = mock(IdentityStats.class);

        CreateIdentity.invoke(mockDatabase, key, props, stats);

        verify(mockDatabase, atLeastOnce()).getCollection("identities");
        verify(mockCollection).insertOne(any(Document.class));
    }

    @Test
    @DisplayName("Test GetIdentityKey utility")
    void testGetIdentityKey() {
        Document doc = new Document("_id", identityId.toString())
                .append("profession_code", "archer");
        when(mockFindIterable.first()).thenReturn(doc);

        Optional<IdentityKey> result = GetIdentityKey.invoke(mockDatabase, identityId);

        assertTrue(result.isPresent());
        assertEquals(identityId, result.get().getUniqueId());
        assertEquals("archer", result.get().getProfessionCode());

        verify(mockDatabase).getCollection("identities");
    }

    @Test
    @DisplayName("Test UpdateProperties utility")
    void testUpdateProperties() {
        IdentityProperties props = mock(IdentityProperties.class);
        when(props.getLevel()).thenReturn(15);
        when(props.getExp()).thenReturn(1000);

        UpdateProperties.invoke(mockDatabase, identityId, props);

        verify(mockDatabase).getCollection("identities");
        verify(mockCollection).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    @DisplayName("Test CreateAbility utility")
    void testCreateAbility() {
        CreateAbility.invoke(mockDatabase, "blink", "Blink", AbilityType.ACTIVE, true);
        verify(mockDatabase).getCollection("abilities");
        verify(mockCollection).insertOne(any(Document.class));
    }

    @Test
    @DisplayName("Test AddStat utility")
    void testAddStat() {
        AddStat.invoke(mockDatabase, identityId, "energy", 50);
        verify(mockDatabase).getCollection("identities");
        verify(mockCollection).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    @DisplayName("Test GetStats utility")
    void testGetStats() {
        Document statsDoc = new Document("energy", 80).append("health", 150);
        Document identityDoc = new Document("stats", statsDoc);
        when(mockFindIterable.first()).thenReturn(identityDoc);

        Optional<IdentityStats> result = GetStats.invoke(mockDatabase, identityId);

        assertTrue(result.isPresent());
        assertEquals(80, result.get().getEnergy());
        assertEquals(150, result.get().getHealth());
    }

    @Test
    @DisplayName("Test GetStat utility")
    void testGetStat() {
        Document statsDoc = new Document("strength", 50);
        Document identityDoc = new Document("stats", statsDoc);
        when(mockFindIterable.first()).thenReturn(identityDoc);

        int result = GetStat.invoke(mockDatabase, identityId, "strength");

        assertEquals(50, result);
        verify(mockDatabase).getCollection("identities");
    }

    @Test
    @DisplayName("Test SetActiveIdentity utility")
    void testSetActiveIdentity() {
        SetActiveIdentity.invoke(mockDatabase, playerId, identityId, "content-data");
        verify(mockDatabase).getCollection("sessions");
        verify(mockCollection).updateOne(any(Bson.class), any(Bson.class), any());
    }

    @Test
    @DisplayName("Test GetSession utility")
    void testGetSession() {
        Document sessionDoc = new Document("_id", playerId.toString())
                .append("identity_uuid", identityId.toString())
                .append("profession_code", "mage")
                .append("content_key", "some-key");
        when(mockFindIterable.first()).thenReturn(sessionDoc);

        Optional<IdentitySession> result = GetSession.invoke(mockDatabase, playerId);

        assertTrue(result.isPresent());
        assertEquals(identityId, result.get().identityUuid());
        assertEquals("mage", result.get().identityKey().getProfessionCode());
    }

    @Test
    @DisplayName("Test GetProfessions utility")
    void testGetProfessions() {
        MongoCursor<Document> mockCursor = mock(MongoCursor.class);
        when(mockFindIterable.iterator()).thenReturn(mockCursor);
        when(mockCursor.hasNext()).thenReturn(true, false);
        when(mockCursor.next()).thenReturn(new Document("system_code", "thief").append("display_name", "Thief"));
        
        Collection<Profession> result = GetProfessions.invoke(mockDatabase);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(mockDatabase).getCollection("professions");
    }

    @Test
    @DisplayName("Test SetEquippedAbility utility")
    void testSetEquippedAbility() {
        SetEquippedAbility.invoke(mockDatabase, identityId, 2, "heal", "model_id");
        verify(mockDatabase).getCollection("identities");
        verify(mockCollection).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    @DisplayName("Test SetRune utility")
    void testSetRune() {
        SetRune.invoke(mockDatabase, identityId, 1, "speed");
        verify(mockDatabase).getCollection("identities");
        verify(mockCollection).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    @DisplayName("Test LogAction utility")
    void testLogAction() {
        LogAction.invoke(mockDatabase, playerId, identityId, ActionType.EQUIP);
        verify(mockDatabase).getCollection("logs");
        verify(mockCollection).insertOne(any(Document.class));
    }

    @Test
    @DisplayName("Test SaveSkin utility")
    void testSaveSkin() {
        SaveSkin.invoke(mockDatabase, identityId, "new-skin");
        verify(mockDatabase).getCollection("identities");
        verify(mockCollection).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    @DisplayName("Test SaveAura utility")
    void testSaveAura() {
        SaveAura.invoke(mockDatabase, identityId, "fire-aura");
        verify(mockDatabase).getCollection("identities");
        verify(mockCollection).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    @DisplayName("Test GetAbilities utility")
    void testGetAbilities() {
        MongoCursor<Document> mockCursor = mock(MongoCursor.class);
        when(mockFindIterable.iterator()).thenReturn(mockCursor);
        when(mockCursor.hasNext()).thenReturn(true, false);
        when(mockCursor.next()).thenReturn(new Document("system_code", "fireball"));
        
        Collection<Ability> result = GetAbilities.invoke(mockDatabase);
        assertFalse(result.isEmpty());
        verify(mockDatabase).getCollection("abilities");
    }

    @Test
    @DisplayName("Test GetRunes utility")
    void testGetRunes() {
        MongoCursor<Document> mockCursor = mock(MongoCursor.class);
        when(mockFindIterable.iterator()).thenReturn(mockCursor);
        when(mockCursor.hasNext()).thenReturn(true, false);
        when(mockCursor.next()).thenReturn(new Document("system_code", "haste"));
        
        Collection<Rune> result = GetRunes.invoke(mockDatabase);
        assertFalse(result.isEmpty());
        verify(mockDatabase).getCollection("runes");
    }

    @Test
    @DisplayName("Test GetEquippedAbilities utility")
    void testGetEquippedAbilities() {
        Document abilitiesDoc = new Document("1", new Document("code", "blink").append("model", "m1"));
        Document identityDoc = new Document("equipped_abilities", abilitiesDoc);
        when(mockFindIterable.first()).thenReturn(identityDoc);
        
        Map<Integer, EquippedAbility> result = GetEquippedAbilities.invoke(mockDatabase, identityId);
        assertEquals("blink", result.get(1).abilityCode());
    }

    @Test
    @DisplayName("Test GetCosmetics utility")
    void testGetCosmetics() {
        Document cosDoc = new Document("skin", "s1").append("aura", "a1");
        Document identityDoc = new Document("cosmetics", cosDoc);
        when(mockFindIterable.first()).thenReturn(identityDoc);
        
        Map<String, String> result = GetCosmetics.invoke(mockDatabase, identityId);
        assertEquals("s1", result.get("skin"));
        assertEquals("a1", result.get("aura"));
    }

    @Test
    @DisplayName("Test GetActiveIdentity utility")
    void testGetActiveIdentity() {
        Document sessionDoc = new Document("identity_uuid", identityId.toString());
        when(mockFindIterable.first()).thenReturn(sessionDoc);
        
        Optional<UUID> result = GetActiveIdentity.invoke(mockDatabase, playerId);
        assertTrue(result.isPresent());
        assertEquals(identityId, result.get());
    }

    @Test
    @DisplayName("Test GetProperties utility")
    void testGetProperties() {
        Document propsDoc = new Document("level", 10);
        Document identityDoc = new Document("properties", propsDoc);
        when(mockFindIterable.first()).thenReturn(identityDoc);
        
        Optional<IdentityProperties> result = GetProperties.invoke(mockDatabase, identityId);
        assertTrue(result.isPresent());
        assertEquals(10, result.get().getLevel());
    }

    @Test
    @DisplayName("Test UpdateStats utility")
    void testUpdateStats() {
        IdentityStats stats = mock(IdentityStats.class);
        UpdateStats.invoke(mockDatabase, identityId, stats);
        verify(mockDatabase).getCollection("identities");
        verify(mockCollection).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    @DisplayName("Test UpdateSessionContentKey utility")
    void testUpdateSessionContentKey() {
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

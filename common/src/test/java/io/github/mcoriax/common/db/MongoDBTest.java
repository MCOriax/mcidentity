package io.github.mcoriax.common.db;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import io.github.mcoriax.api.model.EquippedAbility;
import io.github.mcoriax.api.model.IdentityStats;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Delegation tests for the MongoDB backend over a mocked database handle.
 */
class MongoDBTest {

    /**
     * Backend wired to the mocked database handle.
     */
    static class TestMongoDB extends MongoDB {
        TestMongoDB(MongoDatabase database) {
            super(database);
        }
    }

    private MongoDatabase mockDatabase;
    private MongoCollection<Document> mockCollection;
    private FindIterable<Document> mockFind;
    private UUID identityId;
    private TestMongoDB db;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUpFixtures() {
        mockDatabase = mock(MongoDatabase.class);
        mockCollection = mock(MongoCollection.class);
        mockFind = mock(FindIterable.class);
        identityId = UUID.randomUUID();
        db = new TestMongoDB(mockDatabase);

        when(mockDatabase.getCollection(anyString())).thenReturn(mockCollection);
        when(mockCollection.find(any(Bson.class))).thenReturn(mockFind);
    }

    @Test
    @DisplayName("Professions upsert by system code")
    void professionUpsert() {
        db.createProfession("warrior", "Warrior").join();

        ArgumentCaptor<Bson> update = ArgumentCaptor.forClass(Bson.class);
        verify(mockCollection).updateOne(any(Bson.class), update.capture(), any(UpdateOptions.class));
        Document set = (Document) ((Document) update.getValue()).get("$set");
        assertEquals("warrior", set.getString("system_code"));
        assertEquals("Warrior", set.getString("display_name"));
    }

    @Test
    @DisplayName("Equipped abilities parse both document and legacy string forms")
    void equippedAbilityParsing() {
        Document equipped = new Document()
            .append("1", new Document("system_code", "dash").append("model_id", "m1"))
            .append("2", "fireball");
        Document identity = new Document("_id", identityId.toString()).append("equipped_abilities", equipped);
        when(mockFind.first()).thenReturn(identity);

        Map<Integer, EquippedAbility> slots = db.getEquippedAbilities(identityId).join();
        assertEquals(new EquippedAbility("dash", "m1"), slots.get(1));
        assertEquals(new EquippedAbility("fireball", null), slots.get(2));
    }

    @Test
    @DisplayName("Clearing an ability slot unsets the embedded field")
    void abilityUnset() {
        db.setEquippedAbility(identityId, 3, null, null).join();
        verify(mockCollection).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    @DisplayName("Stats read from the embedded document")
    void statsRead() {
        Document stats = new Document("energy", 90).append("health", 25).append("strength", 8);
        when(mockFind.first()).thenReturn(new Document("_id", identityId.toString()).append("stats", stats));

        IdentityStats result = db.getStats(identityId).join().orElseThrow();
        assertEquals(90, result.getEnergy());
        assertEquals(25, result.getHealth());
        assertEquals(8, result.getStrength());
        assertEquals(20, result.getFood());
        assertEquals(0, db.getStat(identityId, "agility").join());
    }

    @Test
    @DisplayName("Stat keys outside the allow-list never reach the driver")
    void statAllowList() {
        db.addStat(identityId, "$where", 1).join();
        db.getStat(identityId, "stats.$inc").join();
        verify(mockCollection, never()).updateOne(any(Bson.class), any(Bson.class));
        verify(mockCollection, never()).find(any(Bson.class));
    }

    @Test
    @DisplayName("Reads fall back to neutral values when the driver fails")
    void readFallbacks() {
        when(mockCollection.find(any(Bson.class))).thenThrow(new RuntimeException("down"));
        when(mockDatabase.getCollection(anyString())).thenThrow(new RuntimeException("down"));

        assertTrue(db.getProfessions().join().isEmpty());
        assertFalse(db.getIdentityKey(identityId).join().isPresent());
        assertEquals(0, db.getStat(identityId, "energy").join());
        assertTrue(db.getCosmetics(identityId).join().isEmpty());
    }
}

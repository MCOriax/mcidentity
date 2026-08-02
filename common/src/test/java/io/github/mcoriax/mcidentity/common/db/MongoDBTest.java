package io.github.mcoriax.mcidentity.common.db;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import io.github.mcoriax.mcidentity.api.model.IdentityStats;
import io.github.mcoriax.mcidentity.common.model.MutableIdentityProperties;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
    @DisplayName("Alias lookups match on the folded mirror field")
    void aliasLookupFoldsCase() {
        when(mockFind.first()).thenReturn(new Document("_id", identityId.toString()));

        assertEquals(identityId, db.getIdentityByAlias("Alex").join().orElseThrow());

        ArgumentCaptor<Bson> filter = ArgumentCaptor.forClass(Bson.class);
        verify(mockCollection).find(filter.capture());
        assertEquals("alex", render(filter.getValue()).getString("properties.alias_comparable").getValue());
    }

    @Test
    @DisplayName("Claiming an alias stores the original casing next to the folded form")
    void aliasWriteKeepsOriginalCasing() {
        when(mockFind.first()).thenReturn(null);
        when(mockCollection.updateOne(any(Bson.class), any(Bson.class))).thenReturn(matchedUpdate());

        assertTrue(db.setAlias(identityId, "Alex").join());

        ArgumentCaptor<Bson> update = ArgumentCaptor.forClass(Bson.class);
        verify(mockCollection).updateOne(any(Bson.class), update.capture());
        BsonDocument set = render(update.getValue()).getDocument("$set");
        assertEquals("Alex", set.getString("properties.alias").getValue());
        assertEquals("alex", set.getString("properties.alias_comparable").getValue());
    }

    @Test
    @DisplayName("An alias another identity already holds is refused")
    void aliasUniqueness() {
        when(mockFind.first()).thenReturn(new Document("_id", UUID.randomUUID().toString()));

        assertFalse(db.setAlias(identityId, "alex").join());
        verify(mockCollection, never()).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    @DisplayName("Malformed aliases never reach the driver")
    void aliasAllowList() {
        assertTrue(db.getIdentityByAlias("not valid").join().isEmpty());
        assertFalse(db.setAlias(identityId, "$where").join());
        verify(mockCollection, never()).find(any(Bson.class));
        verify(mockCollection, never()).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    @DisplayName("A property write sets fields individually so the alias survives")
    void propertyWriteKeepsAlias() {
        when(mockCollection.updateOne(any(Bson.class), any(Bson.class))).thenReturn(matchedUpdate());

        db.updateProperties(identityId, new MutableIdentityProperties("Alex", 4, 10, 2, "head", null, null)).join();

        ArgumentCaptor<Bson> update = ArgumentCaptor.forClass(Bson.class);
        verify(mockCollection).updateOne(any(Bson.class), update.capture());
        BsonDocument set = render(update.getValue()).getDocument("$set");
        assertEquals(4, set.getInt32("properties.level").getValue());
        assertFalse(set.containsKey("properties"), "the subdocument must not be replaced wholesale");
        assertFalse(set.containsKey("properties.alias"), "a property write must not touch the alias");
    }

    /**
     * Renders an update or filter into the document the driver would send.
     * @param bson The update or filter to render.
     * @return The rendered document.
     */
    private static BsonDocument render(Bson bson) {
        return bson.toBsonDocument(Document.class, MongoClientSettings.getDefaultCodecRegistry());
    }

    /**
     * An update result reporting that one document matched.
     * @return The stubbed result.
     */
    private static UpdateResult matchedUpdate() {
        return UpdateResult.acknowledged(1, 1L, null);
    }

    @Test
    @DisplayName("Reads fall back to neutral values when the driver fails")
    void readFallbacks() {
        when(mockCollection.find(any(Bson.class))).thenThrow(new RuntimeException("down"));
        when(mockDatabase.getCollection(anyString())).thenThrow(new RuntimeException("down"));

        assertTrue(db.getProfessions().join().isEmpty());
        assertFalse(db.getIdentityKey(identityId).join().isPresent());
        assertEquals(0, db.getStat(identityId, "energy").join());
        assertEquals(0, db.getBaseIdentitySlots(identityId).join());
    }

    @Test
    @DisplayName("Base identity slots read from their own collection")
    void baseIdentitySlotsRead() {
        when(mockFind.first()).thenReturn(new Document("_id", identityId.toString()).append("slots", 4));
        assertEquals(4, db.getBaseIdentitySlots(identityId).join());
    }

    @Test
    @DisplayName("Adding base identity slots increments with an upsert and returns the new total")
    void baseIdentitySlotsAdd() {
        when(mockCollection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class)))
            .thenReturn(new Document("_id", identityId.toString()).append("slots", 3));
        assertEquals(3, db.addBaseIdentitySlots(identityId, 1).join());
    }

    @Test
    @DisplayName("Adding base identity slots clamps negative totals to zero")
    void baseIdentitySlotsClamp() {
        when(mockCollection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class)))
            .thenReturn(new Document("_id", identityId.toString()).append("slots", -2));
        assertEquals(0, db.addBaseIdentitySlots(identityId, -5).join());
        verify(mockCollection).updateOne(any(Bson.class), any(Bson.class));
    }
}

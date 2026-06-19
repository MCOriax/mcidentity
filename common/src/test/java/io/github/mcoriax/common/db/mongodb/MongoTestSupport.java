package io.github.mcoriax.common.db.mongodb;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Shared Mockito fixtures for the MongoDB database utility tests.
 * <p>
 * The MongoDB suite is split across several focused subclasses so that no single
 * test file grows too large. They all share this one consistent driver mock setup.
 * </p>
 */
abstract class MongoTestSupport {

    protected MongoDatabase mockDatabase;
    protected MongoCollection<Document> mockCollection;
    protected FindIterable<Document> mockFindIterable;
    protected UUID identityId;
    protected UUID playerId;
    protected TestMCIdentityMongoDB provider;

    /**
     * Concrete subclass of {@link MCIdentityMongoDB} for testing the abstract base.
     */
    static class TestMCIdentityMongoDB extends MCIdentityMongoDB {
        TestMCIdentityMongoDB(MongoDatabase database) {
            super(database);
        }
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUpFixtures() {
        mockDatabase = mock(MongoDatabase.class);
        mockCollection = mock(MongoCollection.class);
        mockFindIterable = mock(FindIterable.class);
        identityId = UUID.randomUUID();
        playerId = UUID.randomUUID();
        provider = new TestMCIdentityMongoDB(mockDatabase);

        when(mockDatabase.getCollection(anyString())).thenReturn(mockCollection);
        when(mockCollection.find()).thenReturn(mockFindIterable);
        when(mockCollection.find(any(Bson.class))).thenReturn(mockFindIterable);
        when(mockFindIterable.iterator()).thenReturn(mock(MongoCursor.class));
    }
}

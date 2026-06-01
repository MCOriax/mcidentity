package io.gitlab.mcoriax.common.db.mongodb;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.gitlab.mcoriax.api.model.IdentityKey;
import io.gitlab.mcoriax.common.db.mongodb.utils.GetIdentityKey;
import io.gitlab.mcoriax.api.model.IdentityProperties;
import io.gitlab.mcoriax.common.db.mongodb.utils.UpdateProperties;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MCIdentityMongoDBTest {

    private MongoDatabase mockDatabase;
    private MongoCollection<Document> mockCollection;
    private FindIterable<Document> mockFindIterable;
    private UUID identityId;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        mockDatabase = mock(MongoDatabase.class);
        mockCollection = mock(MongoCollection.class);
        mockFindIterable = mock(FindIterable.class);
        identityId = UUID.randomUUID();

        when(mockDatabase.getCollection(anyString())).thenReturn(mockCollection);
        when(mockCollection.find(any(Bson.class))).thenReturn(mockFindIterable);
    }

    @Test
    @DisplayName("Test GetIdentityKey utility (MongoDB)")
    void testGetIdentityKey() {
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
    @DisplayName("Test GetIdentityKey when not found (MongoDB)")
    void testGetIdentityKeyNotFound() {
        when(mockFindIterable.first()).thenReturn(null);

        Optional<IdentityKey> result = GetIdentityKey.invoke(mockDatabase, identityId);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Test UpdateProperties utility (MongoDB)")
    void testUpdateProperties() {
        IdentityProperties props = mock(IdentityProperties.class);
        when(props.getLevel()).thenReturn(15);
        when(props.getExp()).thenReturn(1000);
        when(props.getStatPoints()).thenReturn(10);
        when(props.getHeadText()).thenReturn("head-texture-mongo");
        when(props.getInventoryContents()).thenReturn("inv-mongo");
        when(props.getEnderchestContents()).thenReturn("ec-mongo");

        UpdateProperties.invoke(mockDatabase, identityId, props);

        verify(mockDatabase).getCollection("identities");
        verify(mockCollection).updateOne(any(Bson.class), any(Bson.class));
    }
}

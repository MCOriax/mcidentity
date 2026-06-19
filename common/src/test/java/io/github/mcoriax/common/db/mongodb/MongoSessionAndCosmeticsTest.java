package io.github.mcoriax.common.db.mongodb;

import com.mongodb.client.model.UpdateOptions;
import io.github.mcoriax.api.model.*;
import io.github.mcoriax.common.db.mongodb.utils.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Session, cosmetics and action-logging tests for MongoDB.
 */
class MongoSessionAndCosmeticsTest extends MongoTestSupport {

    @Test
    @DisplayName("Test SetActiveIdentity utility rigor")
    void testSetActiveIdentityRigorous() {
        SetActiveIdentity.invoke(mockDatabase, playerId, identityId, "content-data");

        ArgumentCaptor<Bson> filterCaptor = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<Bson> updateCaptor = ArgumentCaptor.forClass(Bson.class);
        verify(mockDatabase).getCollection("identity_sessions");
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
        assertEquals(identityId, result.get().getIdentityUniqueId());

        verify(mockDatabase).getCollection("identity_sessions");
    }

    @Test
    @DisplayName("Test GetActiveIdentity utility rigor")
    void testGetActiveIdentityRigorous() {
        Document sessionDoc = new Document("identity_uuid", identityId.toString());
        when(mockFindIterable.first()).thenReturn(sessionDoc);

        Optional<UUID> result = GetActiveIdentity.invoke(mockDatabase, playerId);
        assertTrue(result.isPresent());
        assertEquals(identityId, result.get());
        verify(mockDatabase).getCollection("identity_sessions");
    }

    @Test
    @DisplayName("Test UpdateSessionContentKey utility rigor")
    void testUpdateSessionContentKeyRigorous() {
        UpdateSessionContentKey.invoke(mockDatabase, playerId, "new-key");
        verify(mockDatabase).getCollection("identity_sessions");
        verify(mockCollection).updateOne(any(Bson.class), any(Bson.class));
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
    @DisplayName("Test GetCosmetics utility rigor")
    void testGetCosmeticsRigorous() {
        Document cosDoc = new Document("skin", "s1").append("aura", "a1");
        Document identityDoc = new Document("cosmetics", cosDoc);
        when(mockFindIterable.first()).thenReturn(identityDoc);

        Map<String, String> result = GetCosmetics.invoke(mockDatabase, identityId);
        assertEquals("s1", result.get("skin"));
        assertEquals("a1", result.get("aura"));

        verify(mockDatabase).getCollection("identities");
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
}

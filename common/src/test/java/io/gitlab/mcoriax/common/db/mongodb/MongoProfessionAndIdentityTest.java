package io.gitlab.mcoriax.common.db.mongodb;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.UpdateOptions;
import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.common.db.mongodb.utils.*;
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
 * Profession and identity-document creation/lookup tests for MongoDB.
 */
class MongoProfessionAndIdentityTest extends MongoTestSupport {

    @Test
    @DisplayName("Test CreateProfession utility rigor")
    void testCreateProfessionRigorous() {
        CreateProfession.invoke(mockDatabase, "mage", "Mage Class");

        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(mockDatabase).getCollection("identity_professions");
        verify(mockCollection).updateOne(any(Bson.class), docCaptor.capture(), any(UpdateOptions.class));

        Document updateDoc = docCaptor.getValue();
        Document setDoc = updateDoc.get("$set", Document.class);
        assertEquals("mage", setDoc.getString("system_code"));
        assertEquals("Mage Class", setDoc.getString("display_name"));
    }

    @Test
    @DisplayName("Test GetProfessions utility rigor")
    @SuppressWarnings("unchecked")
    void testGetProfessionsRigorous() {
        MongoCursor<Document> mockCursor = mock(MongoCursor.class);
        when(mockFindIterable.iterator()).thenReturn(mockCursor);
        when(mockCursor.hasNext()).thenReturn(true, false);
        when(mockCursor.next()).thenReturn(new Document("system_code", "thief").append("display_name", "Thief"));

        Collection<Profession> result = GetProfessions.invoke(mockDatabase);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("thief", result.iterator().next().getSystemCode());
        verify(mockDatabase).getCollection("identity_professions");
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
}

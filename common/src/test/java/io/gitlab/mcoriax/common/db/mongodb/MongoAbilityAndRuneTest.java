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
 * Ability and rune catalog/equipped-slot tests for MongoDB.
 */
class MongoAbilityAndRuneTest extends MongoTestSupport {

    @Test
    @DisplayName("Test CreateAbility utility rigor")
    void testCreateAbilityRigorous() {
        CreateAbility.invoke(mockDatabase, "blink", "Blink", AbilityType.ACTIVE, true);

        ArgumentCaptor<Bson> filterCaptor = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(mockDatabase).getCollection("identity_abilities");
        verify(mockCollection).updateOne(filterCaptor.capture(), docCaptor.capture(), any(UpdateOptions.class));

        assertNotNull(filterCaptor.getValue());
        Document updateDoc = docCaptor.getValue();
        Document setDoc = updateDoc.get("$set", Document.class);
        assertEquals("blink", setDoc.getString("system_code"));
        assertEquals("Blink", setDoc.getString("display_name"));
        assertEquals("ACTIVE", setDoc.getString("ability_type"));
        assertTrue(setDoc.getBoolean("is_global"));
    }

    @Test
    @DisplayName("Test GetAbilities utility rigor")
    @SuppressWarnings("unchecked")
    void testGetAbilitiesRigorous() {
        MongoCursor<Document> mockCursor = mock(MongoCursor.class);
        when(mockFindIterable.iterator()).thenReturn(mockCursor);
        when(mockCursor.hasNext()).thenReturn(true, false);
        when(mockCursor.next()).thenReturn(new Document("system_code", "fireball"));

        Collection<Ability> result = GetAbilities.invoke(mockDatabase);
        assertFalse(result.isEmpty());
        verify(mockDatabase).getCollection("identity_abilities");
    }

    @Test
    @DisplayName("Test SetEquippedAbility utility rigor")
    void testSetEquippedAbilityRigorous() {
        SetEquippedAbility.invoke(mockDatabase, identityId, 2, "heal", "model_id");
        verify(mockDatabase).getCollection("identities");
        verify(mockCollection).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    @DisplayName("Test GetEquippedAbilities utility rigor")
    void testGetEquippedAbilitiesRigorous() {
        Document abilitiesDoc = new Document("1", new Document("system_code", "blink").append("model_id", "m1"));
        Document identityDoc = new Document("equipped_abilities", abilitiesDoc);
        when(mockFindIterable.first()).thenReturn(identityDoc);

        Map<Integer, EquippedAbility> result = GetEquippedAbilities.invoke(mockDatabase, identityId);
        assertNotNull(result.get(1));
        assertEquals("blink", result.get(1).systemCode());
    }

    @Test
    @DisplayName("Test SetRune utility rigor")
    void testSetRuneRigorous() {
        SetRune.invoke(mockDatabase, identityId, 1, "speed");
        verify(mockDatabase).getCollection("identities");
        verify(mockCollection).updateOne(any(Bson.class), any(Bson.class));
    }

    @Test
    @DisplayName("Test GetRunes utility rigor")
    @SuppressWarnings("unchecked")
    void testGetRunesRigorous() {
        MongoCursor<Document> mockCursor = mock(MongoCursor.class);
        when(mockFindIterable.iterator()).thenReturn(mockCursor);
        when(mockCursor.hasNext()).thenReturn(true, false);
        when(mockCursor.next()).thenReturn(new Document("system_code", "haste"));

        Collection<Rune> result = GetRunes.invoke(mockDatabase);
        assertFalse(result.isEmpty());
        verify(mockDatabase).getCollection("identity_runes");
    }
}

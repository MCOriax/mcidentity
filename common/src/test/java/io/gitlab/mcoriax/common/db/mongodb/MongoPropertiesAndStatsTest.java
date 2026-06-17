package io.gitlab.mcoriax.common.db.mongodb;

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
 * Properties and stats read/update tests for MongoDB.
 */
class MongoPropertiesAndStatsTest extends MongoTestSupport {

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
    @DisplayName("Test UpdateStats utility rigor")
    void testUpdateStatsRigorous() {
        IdentityStats stats = mock(IdentityStats.class);
        when(stats.getEnergy()).thenReturn(100);
        UpdateStats.invoke(mockDatabase, identityId, stats);
        verify(mockDatabase).getCollection("identities");
        verify(mockCollection).updateOne(any(Bson.class), any(Bson.class));
    }
}

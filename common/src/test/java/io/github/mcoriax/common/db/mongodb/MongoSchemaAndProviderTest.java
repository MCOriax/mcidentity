package io.github.mcoriax.common.db.mongodb;

import io.github.mcoriax.api.model.*;
import io.github.mcoriax.common.db.mongodb.utils.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Schema bootstrapping, provider delegation and failure-path tests for MongoDB.
 */
class MongoSchemaAndProviderTest extends MongoTestSupport {

    @Test
    @DisplayName("Test MCIdentityMongoDB provider delegation logic")
    void testProviderDelegation() throws Exception {
        // Initialize Schema
        provider.initializeSchema().join();
        verify(mockDatabase, atLeastOnce()).getCollection(anyString());

        // Create Profession
        provider.createProfession("warrior", "Warrior Class").join();
        verify(mockDatabase, atLeastOnce()).getCollection("identity_professions");

        // Add Stat
        provider.addStat(identityId, "strength", 15).join();
        verify(mockDatabase, atLeastOnce()).getCollection("identities");
    }

    @Test
    @DisplayName("Test InitializeSchema utility rigor")
    void testInitializeSchemaRigorous() {
        InitializeSchema.invoke(mockDatabase);
        // Verify index creation or collection access
        verify(mockDatabase, atLeastOnce()).getCollection(anyString());
    }

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

    @Test
    @DisplayName("Test GetStat with invalid key is rejected before any query")
    void testGetStatInvalidKey() {
        int result = GetStat.invoke(mockDatabase, identityId, "invalid_stat");
        assertEquals(0, result);
        verify(mockDatabase, never()).getCollection(anyString());
    }
}

package io.gitlab.mcoriax.common;

import io.gitlab.mcoriax.api.db.IMCIdentityDB;
import io.gitlab.mcoriax.api.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MCIdentityProviderTest {

    private IMCIdentityDB mockDatabase;
    private MCIdentityProvider provider;
    private UUID identityId;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        mockDatabase = mock(IMCIdentityDB.class);
        provider = new MCIdentityProvider(mockDatabase);
        identityId = UUID.randomUUID();
        playerId = UUID.randomUUID();
        MCIdentityManager.clear();
    }

    @Test
    @DisplayName("Test schema initialization")
    void testInitializeSchema() {
        when(mockDatabase.initializeSchema()).thenReturn(CompletableFuture.completedFuture(null));
        provider.initializeSchema().join();
        verify(mockDatabase).initializeSchema();
    }

    @Test
    @DisplayName("Test profession creation")
    void testCreateProfession() {
        String code = "Warrior";
        String name = "The Warrior";
        when(mockDatabase.createProfession(anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        
        provider.createProfession(code, name).join();
        verify(mockDatabase).createProfession("warrior", name);
    }

    @Test
    @DisplayName("Test identity creation with fixed profession")
    void testCreateIdentityFixed() {
        IdentityKey key = mock(IdentityKey.class);
        when(key.getProfessionCode()).thenReturn("warrior");
        IdentityProperties props = mock(IdentityProperties.class);
        IdentityStats stats = mock(IdentityStats.class);

        when(mockDatabase.createIdentity(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        
        provider.createIdentity(key, props, stats).join();
        verify(mockDatabase).createIdentity(key, props, stats);
    }

    @Test
    @DisplayName("Test identity creation with random profession")
    void testCreateIdentityRandom() {
        IdentityKey key = mock(IdentityKey.class);
        when(key.getProfessionCode()).thenReturn("random");
        IdentityProperties props = mock(IdentityProperties.class);
        IdentityStats stats = mock(IdentityStats.class);

        MCIdentityManager.setProfessionDisplayName("mage", "Mage");
        
        when(mockDatabase.getProfessions()).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        when(mockDatabase.createIdentity(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        
        provider.createIdentity(key, props, stats).join();
        verify(mockDatabase).createIdentity(argThat(k -> k != null && "mage".equals(k.getProfessionCode())), eq(props), eq(stats));
    }

    @Test
    @DisplayName("Test setting active identity (equip)")
    void testSetActiveIdentityEquip() {
        String contentKey = "item-data";
        when(mockDatabase.setActiveIdentity(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        
        // Mocking the chain of calls in loadSessionToRam
        IdentityKey key = mock(IdentityKey.class);
        when(mockDatabase.getIdentityKey(identityId)).thenReturn(CompletableFuture.completedFuture(Optional.of(key)));
        
        IdentityProperties props = mock(IdentityProperties.class);
        when(mockDatabase.getProperties(identityId)).thenReturn(CompletableFuture.completedFuture(Optional.of(props)));
        
        when(mockDatabase.getCosmetics(identityId)).thenReturn(CompletableFuture.completedFuture(new HashMap<>()));
        when(mockDatabase.getEquippedAbilities(identityId)).thenReturn(CompletableFuture.completedFuture(new HashMap<>()));
        
        IdentityStats stats = mock(IdentityStats.class);
        when(stats.getHealth()).thenReturn(100);
        when(stats.getEnergy()).thenReturn(50);
        when(stats.getFood()).thenReturn(20);
        when(mockDatabase.getStats(identityId)).thenReturn(CompletableFuture.completedFuture(Optional.of(stats)));

        provider.setActiveIdentity(playerId, identityId, contentKey).join();
        
        verify(mockDatabase).setActiveIdentity(playerId, identityId, contentKey);
        assertTrue(MCIdentityManager.getSession(playerId).isPresent());
    }

    @Test
    @DisplayName("Test setting active identity (unequip)")
    void testSetActiveIdentityUnequip() {
        when(mockDatabase.setActiveIdentity(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        
        provider.setActiveIdentity(playerId, null, null).join();
        
        verify(mockDatabase).setActiveIdentity(playerId, null, null);
        assertFalse(MCIdentityManager.getSession(playerId).isPresent());
    }

    @Test
    @DisplayName("Test update session content key")
    void testUpdateSessionContentKey() {
        String oldKey = "old-key";
        String newKey = "new-key";
        
        ActiveIdentitySession session = mock(ActiveIdentitySession.class);
        when(session.playerUuid()).thenReturn(playerId);
        when(session.identityUuid()).thenReturn(identityId);
        MCIdentityManager.cacheSession(session);

        when(mockDatabase.updateSessionContentKey(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        
        provider.updateSessionContentKey(playerId, newKey).join();
        
        verify(mockDatabase).updateSessionContentKey(playerId, newKey);
        assertEquals(newKey, MCIdentityManager.getSession(playerId).get().contentKey());
    }
}

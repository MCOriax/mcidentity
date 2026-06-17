package io.gitlab.mcoriax.common;

import io.gitlab.mcoriax.api.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Active-identity session lifecycle tests for {@link MCIdentityProvider}.
 */
class MCIdentityProviderSessionTest extends ProviderTestSupport {

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

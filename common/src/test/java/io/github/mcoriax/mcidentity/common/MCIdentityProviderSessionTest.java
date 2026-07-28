package io.github.mcoriax.mcidentity.common;

import io.github.mcoriax.mcidentity.api.model.*;
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

        IdentityProperties props = mock(IdentityProperties.class);
        IdentityStats stats = mock(IdentityStats.class);
        ActiveIdentitySession session = new ActiveIdentitySession(
            playerId, identityId, mock(IdentityKey.class), props, stats,
            "old-key", 100, 50, 20);
        MCIdentityManager.cacheSession(session);

        when(mockDatabase.updateSessionContentKey(any(), any())).thenReturn(CompletableFuture.completedFuture(null));

        provider.updateSessionContentKey(playerId, newKey).join();

        verify(mockDatabase).updateSessionContentKey(playerId, newKey);
        ActiveIdentitySession updated = MCIdentityManager.getSession(playerId).get();
        assertEquals(newKey, updated.contentKey());
        // The remaining fields must be carried over unchanged by the wither.
        assertEquals(identityId, updated.identityUuid());
        assertSame(props, updated.properties());
        assertSame(stats, updated.stats());
    }
}

package io.github.mcoriax.mcidentity.common;

import io.github.mcoriax.mcidentity.api.model.ActiveIdentitySession;
import io.github.mcoriax.mcidentity.api.model.IdentityKey;
import io.github.mcoriax.mcidentity.api.model.IdentityProperties;
import io.github.mcoriax.mcidentity.api.model.IdentityStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for caching and mutating the in-RAM {@link ActiveIdentitySession} store.
 */
class MCIdentityManagerSessionTest {

    private UUID playerId;
    private UUID identityId;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        identityId = UUID.randomUUID();
        MCIdentityManager.clear();
    }

    @Test
    @DisplayName("Test session caching and retrieval")
    void testSessionCaching() {
        IdentityKey key = Mockito.mock(IdentityKey.class);
        IdentityProperties props = Mockito.mock(IdentityProperties.class);
        IdentityStats stats = Mockito.mock(IdentityStats.class);

        ActiveIdentitySession session = new ActiveIdentitySession(
            playerId, identityId, key, props, stats, "content-key",
            100.0, 50.0, 20.0
        );

        MCIdentityManager.cacheSession(session);

        Optional<ActiveIdentitySession> cached = MCIdentityManager.getSession(playerId);
        assertTrue(cached.isPresent());
        assertEquals(identityId, cached.get().identityUuid());

        Optional<ActiveIdentitySession> byIdentity = MCIdentityManager.getSessionByIdentity(identityId);
        assertTrue(byIdentity.isPresent());
        assertEquals(playerId, byIdentity.get().playerUuid());

        MCIdentityManager.removeSession(playerId);
        assertFalse(MCIdentityManager.getSession(playerId).isPresent());
    }

    @Test
    @DisplayName("Test session updates")
    void testSessionUpdates() {
        IdentityKey key = Mockito.mock(IdentityKey.class);
        IdentityProperties props = Mockito.mock(IdentityProperties.class);
        IdentityStats stats = Mockito.mock(IdentityStats.class);

        ActiveIdentitySession session = new ActiveIdentitySession(
            playerId, identityId, key, props, stats, "content-key",
            100.0, 50.0, 20.0
        );
        MCIdentityManager.cacheSession(session);

        MCIdentityManager.updateHealth(playerId, 80.0);
        assertEquals(80.0, MCIdentityManager.getSession(playerId).get().currentHealth());

        MCIdentityManager.updateEnergy(playerId, 30.0);
        assertEquals(30.0, MCIdentityManager.getSession(playerId).get().currentEnergy());

        MCIdentityManager.updateFood(playerId, 15.0);
        assertEquals(15.0, MCIdentityManager.getSession(playerId).get().currentFood());

        IdentityStats newStats = Mockito.mock(IdentityStats.class);
        MCIdentityManager.updateStats(playerId, newStats);
        assertEquals(newStats, MCIdentityManager.getSession(playerId).get().stats());
    }

}

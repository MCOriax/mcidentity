package io.github.mcoriax.mcidentity.common;

import io.github.mcoriax.mcidentity.api.model.ActiveProfessionSession;
import io.github.mcoriax.mcidentity.common.model.PlayerStatsRecord;
import io.github.mcoriax.mcidentity.common.model.ProfessionProgressRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Session loading tests for {@link MCIdentityProvider}.
 */
class MCIdentityProviderSessionTest extends ProviderTestSupport {

    private final PlayerStatsRecord stats = new PlayerStatsRecord(3, 100, 40, 20, 1, 2, 3, 4, 5, 6);

    @Test
    @DisplayName("loadSession caches the stored profession, its progress and the stats")
    void testLoadSessionWithStoredProfession() {
        when(mockDatabase.getActiveProfession(playerId)).thenReturn(CompletableFuture.completedFuture(Optional.of("miner")));
        when(mockDatabase.getProgress(playerId, "miner"))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(new ProfessionProgressRecord(6, 1))));
        when(mockDatabase.getStats(playerId)).thenReturn(CompletableFuture.completedFuture(Optional.of(stats)));

        ActiveProfessionSession session = provider.loadSession(playerId).join().orElseThrow();

        assertEquals("miner", session.professionCode());
        assertEquals(6, session.progress().getLevel());
        assertEquals(40.0, session.currentHealth());
        assertEquals(Optional.of("miner"), MCIdentityManager.getActiveProfession(playerId));
    }

    @Test
    @DisplayName("loadSession wears the base profession when none is stored")
    void testLoadSessionDefaultsToBase() {
        when(mockDatabase.getActiveProfession(playerId)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(mockDatabase.getProgress(any(), anyString())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(mockDatabase.getStats(playerId)).thenReturn(CompletableFuture.completedFuture(Optional.of(stats)));

        ActiveProfessionSession session = provider.loadSession(playerId).join().orElseThrow();

        assertEquals(BaseProfession.CODE, session.professionCode());
        assertEquals(1, session.progress().getLevel());
    }

    @Test
    @DisplayName("loadSession caches nothing when the stats row is missing")
    void testLoadSessionWithoutStats() {
        when(mockDatabase.getActiveProfession(playerId)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(mockDatabase.getProgress(any(), anyString())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(mockDatabase.getStats(playerId)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertTrue(provider.loadSession(playerId).join().isEmpty());
        assertTrue(MCIdentityManager.getSession(playerId).isEmpty());
    }
}

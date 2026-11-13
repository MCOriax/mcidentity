package io.github.mcoriax.mcidentity.common;

import io.github.mcoriax.mcidentity.api.model.ActionType;
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
 * Write-path tests for {@link MCIdentityProvider}: delegation, key lower-casing
 * and keeping the in-RAM session in sync after a successful database write.
 */
class MCIdentityProviderMutationTest extends ProviderTestSupport {

    private static PlayerStatsRecord stats(int statPoints) {
        return new PlayerStatsRecord(statPoints, 100, 20, 20, 0, 0, 0, 0, 0, 0);
    }

    private void cacheSession(String code) {
        MCIdentityManager.cacheSession(new ActiveProfessionSession(playerId, code,
            new ProfessionProgressRecord(1, 0), stats(0), 100.0, 50.0, 20.0));
    }

    @Test
    @DisplayName("addStat lowercases the stat key and delegates")
    void testAddStatLowercased() {
        when(mockDatabase.addStat(any(), anyString(), anyInt())).thenReturn(CompletableFuture.completedFuture(null));
        provider.addStat(playerId, "Energy", 5).join();
        verify(mockDatabase).addStat(playerId, "energy", 5);
    }

    @Test
    @DisplayName("logAction normalizes the profession code and delegates")
    void testLogActionDelegates() {
        when(mockDatabase.logAction(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));

        provider.logAction(playerId, " Miner ", ActionType.EQUIP).join();

        verify(mockDatabase).logAction(playerId, "miner", ActionType.EQUIP);
    }

    @Test
    @DisplayName("setActiveProfession stores the code and swaps the cached profession and progress")
    void testSetActiveProfession() {
        cacheSession("player");
        when(mockDatabase.setActiveProfession(any(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        when(mockDatabase.getProgress(playerId, "miner"))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(new ProfessionProgressRecord(5, 10))));

        provider.setActiveProfession(playerId, "Miner").join();

        verify(mockDatabase).setActiveProfession(playerId, "miner");
        ActiveProfessionSession session = MCIdentityManager.getSession(playerId).orElseThrow();
        assertEquals("miner", session.professionCode());
        assertEquals(5, session.progress().getLevel());
    }

    @Test
    @DisplayName("A profession never worn before starts at level 1")
    void testSetActiveProfessionStartsFresh() {
        cacheSession("player");
        when(mockDatabase.setActiveProfession(any(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        when(mockDatabase.getProgress(any(), anyString())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        provider.setActiveProfession(playerId, "fighter").join();

        ActiveProfessionSession session = MCIdentityManager.getSession(playerId).orElseThrow();
        assertEquals(ProfessionProgressRecord.starting().getLevel(), session.progress().getLevel());
        assertEquals(0, session.progress().getExp());
    }

    @Test
    @DisplayName("clearActiveProfession wears the base profession")
    void testClearActiveProfession() {
        when(mockDatabase.setActiveProfession(any(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        when(mockDatabase.getProgress(any(), anyString())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        provider.clearActiveProfession(playerId).join();

        verify(mockDatabase).setActiveProfession(playerId, BaseProfession.CODE);
    }

    @Test
    @DisplayName("updateProgress delegates and syncs the cached session")
    void testUpdateProgressUpdatesRam() {
        cacheSession("miner");
        ProfessionProgressRecord progress = new ProfessionProgressRecord(8, 3);
        when(mockDatabase.updateProgress(any(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        provider.updateProgress(playerId, "miner", progress).join();

        verify(mockDatabase).updateProgress(playerId, "miner", progress);
        assertEquals(8, MCIdentityManager.getSession(playerId).orElseThrow().progress().getLevel());
    }

    @Test
    @DisplayName("updateStats delegates and syncs the cached session")
    void testUpdateStatsUpdatesRam() {
        cacheSession("miner");
        PlayerStatsRecord newStats = stats(7);
        when(mockDatabase.updateStats(any(), any())).thenReturn(CompletableFuture.completedFuture(null));

        provider.updateStats(playerId, newStats).join();

        verify(mockDatabase).updateStats(playerId, newStats);
        assertSame(newStats, MCIdentityManager.getSession(playerId).orElseThrow().stats());
    }
}

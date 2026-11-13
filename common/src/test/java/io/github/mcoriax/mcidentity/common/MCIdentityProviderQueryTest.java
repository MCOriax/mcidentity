package io.github.mcoriax.mcidentity.common;

import io.github.mcoriax.mcidentity.common.model.ProfessionProgressRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Read-path delegation tests for {@link MCIdentityProvider}: the provider should
 * forward lookups to the database (lower-casing keys where documented) and pass
 * results straight through.
 */
class MCIdentityProviderQueryTest extends ProviderTestSupport {

    @Test
    @DisplayName("getStat lowercases the stat key")
    void testGetStatLowercased() {
        when(mockDatabase.getStat(eq(playerId), anyString())).thenReturn(CompletableFuture.completedFuture(42));
        assertEquals(42, provider.getStat(playerId, "Strength").join());
        verify(mockDatabase).getStat(playerId, "strength");
    }

    @Test
    @DisplayName("loadActiveProfession reads storage, not RAM")
    void testLoadActiveProfession() {
        when(mockDatabase.getActiveProfession(playerId)).thenReturn(CompletableFuture.completedFuture(Optional.of("miner")));

        assertEquals(Optional.of("miner"), provider.loadActiveProfession(playerId).join());
        verify(mockDatabase).getActiveProfession(playerId);
    }

    @Test
    @DisplayName("getProgress normalizes the profession code")
    void testGetProgressNormalized() {
        ProfessionProgressRecord progress = new ProfessionProgressRecord(2, 5);
        when(mockDatabase.getProgress(playerId, "miner")).thenReturn(CompletableFuture.completedFuture(Optional.of(progress)));

        assertSame(progress, provider.getProgress(playerId, "MINER").join().orElseThrow());
    }
}

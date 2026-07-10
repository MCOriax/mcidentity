package io.github.mcoriax.common;

import io.github.mcoriax.api.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Write-path tests for {@link MCIdentityProvider}: delegation, key lower-casing
 * and keeping the in-RAM session in sync after a successful database write.
 */
class MCIdentityProviderMutationTest extends ProviderTestSupport {

    private ActiveIdentitySession cacheSession() {
        ActiveIdentitySession session = new ActiveIdentitySession(
            playerId, identityId,
            mock(IdentityKey.class), mock(IdentityProperties.class), mock(IdentityStats.class),
            "content-key", 100.0, 50.0, 20.0, new HashMap<>()
        );
        MCIdentityManager.cacheSession(session);
        return session;
    }

    @Test
    @DisplayName("Test addStat lowercases the stat key and delegates")
    void testAddStatLowercased() {
        when(mockDatabase.addStat(any(), anyString(), anyInt())).thenReturn(CompletableFuture.completedFuture(null));
        provider.addStat(identityId, "Energy", 5).join();
        verify(mockDatabase).addStat(identityId, "energy", 5);
    }

    @Test
    @DisplayName("Test logAction, saveSkin and saveAura delegate to the database")
    void testSimpleMutationsDelegate() {
        when(mockDatabase.logAction(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(mockDatabase.saveSkin(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(mockDatabase.saveAura(any(), any())).thenReturn(CompletableFuture.completedFuture(null));

        provider.logAction(playerId, identityId, ActionType.EQUIP).join();
        provider.saveSkin(identityId, "skin").join();
        provider.saveAura(identityId, "aura").join();

        verify(mockDatabase).logAction(playerId, identityId, ActionType.EQUIP);
        verify(mockDatabase).saveSkin(identityId, "skin");
        verify(mockDatabase).saveAura(identityId, "aura");
    }

    @Test
    @DisplayName("Test setRune lowercases the rune code and delegates")
    void testSetRuneLowercased() {
        when(mockDatabase.setRune(any(), anyInt(), any())).thenReturn(CompletableFuture.completedFuture(null));
        provider.setRune(identityId, 1, "Haste").join();
        verify(mockDatabase).setRune(identityId, 1, "haste");
    }

    @Test
    @DisplayName("Test setEquippedAbility lowercases the code and updates RAM")
    void testSetEquippedAbilityUpdatesRam() {
        cacheSession();
        when(mockDatabase.setEquippedAbility(any(), anyInt(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(null));

        provider.setEquippedAbility(identityId, 1, "FireBall", "m1").join();

        verify(mockDatabase).setEquippedAbility(identityId, 1, "fireball", "m1");
        ActiveIdentitySession updated = MCIdentityManager.getSession(playerId).orElseThrow();
        assertEquals("fireball", updated.equippedAbilities().get(1).abilityCode());
    }

    @Test
    @DisplayName("Test setEquippedAbility with a null code clears the RAM slot")
    void testSetEquippedAbilityClearsRam() {
        ActiveIdentitySession session = new ActiveIdentitySession(
            playerId, identityId,
            mock(IdentityKey.class), mock(IdentityProperties.class), mock(IdentityStats.class),
            "content-key", 100.0, 50.0, 20.0, new HashMap<>(Map.of(1, new EquippedAbility("fireball", "m1")))
        );
        MCIdentityManager.cacheSession(session);
        when(mockDatabase.setEquippedAbility(any(), anyInt(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(null));

        provider.setEquippedAbility(identityId, 1, null, null).join();

        verify(mockDatabase).setEquippedAbility(identityId, 1, null, null);
        assertNull(MCIdentityManager.getSession(playerId).orElseThrow().equippedAbilities().get(1));
    }

    @Test
    @DisplayName("Test updateProperties delegates and syncs the cached session")
    void testUpdatePropertiesUpdatesRam() {
        cacheSession();
        IdentityProperties newProps = mock(IdentityProperties.class);
        when(mockDatabase.updateProperties(any(), any())).thenReturn(CompletableFuture.completedFuture(null));

        provider.updateProperties(identityId, newProps).join();

        verify(mockDatabase).updateProperties(identityId, newProps);
        assertSame(newProps, MCIdentityManager.getSession(playerId).orElseThrow().properties());
    }

    @Test
    @DisplayName("Test updateStats delegates and syncs the cached session")
    void testUpdateStatsUpdatesRam() {
        cacheSession();
        IdentityStats newStats = mock(IdentityStats.class);
        when(mockDatabase.updateStats(any(), any())).thenReturn(CompletableFuture.completedFuture(null));

        provider.updateStats(identityId, newStats).join();

        verify(mockDatabase).updateStats(identityId, newStats);
        assertSame(newStats, MCIdentityManager.getSession(playerId).orElseThrow().stats());
    }
}

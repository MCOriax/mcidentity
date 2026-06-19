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
 * Profession/ability creation and identity-creation tests for {@link MCIdentityProvider}.
 */
class MCIdentityProviderIdentityTest extends ProviderTestSupport {

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
    @DisplayName("Test ability creation lowercases the system code")
    void testCreateAbilityLowercased() {
        when(mockDatabase.createAbility(anyString(), anyString(), any(), anyBoolean()))
            .thenReturn(CompletableFuture.completedFuture(null));

        provider.createAbility("FireBall", "Fireball", AbilityType.ACTIVE, true).join();
        verify(mockDatabase).createAbility("fireball", "Fireball", AbilityType.ACTIVE, true);
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
        // The random path resolves to one of the professions registered in memory.
        verify(mockDatabase).createIdentity(
            argThat(k -> k != null && MCIdentityManager.getRegisteredProfessions().containsKey(k.getProfessionCode())),
            eq(props), eq(stats));
    }
}

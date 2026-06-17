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
 * Read-path delegation tests for {@link MCIdentityProvider}: the provider should
 * forward lookups to the database (lower-casing keys where documented) and pass
 * results straight through.
 */
class MCIdentityProviderQueryTest extends ProviderTestSupport {

    @Test
    @DisplayName("Test catalog getters delegate to the database")
    void testCatalogGetters() {
        Collection<Profession> professions = List.of();
        Collection<Ability> abilities = List.of();
        Collection<Rune> runes = List.of();
        when(mockDatabase.getProfessions()).thenReturn(CompletableFuture.completedFuture(professions));
        when(mockDatabase.getAbilities()).thenReturn(CompletableFuture.completedFuture(abilities));
        when(mockDatabase.getRunes()).thenReturn(CompletableFuture.completedFuture(runes));

        assertSame(professions, provider.getProfessions().join());
        assertSame(abilities, provider.getAbilities().join());
        assertSame(runes, provider.getRunes().join());

        verify(mockDatabase).getProfessions();
        verify(mockDatabase).getAbilities();
        verify(mockDatabase).getRunes();
    }

    @Test
    @DisplayName("Test getStat lowercases the stat key")
    void testGetStatLowercased() {
        when(mockDatabase.getStat(eq(identityId), anyString())).thenReturn(CompletableFuture.completedFuture(42));
        assertEquals(42, provider.getStat(identityId, "Strength").join());
        verify(mockDatabase).getStat(identityId, "strength");
    }

    @Test
    @DisplayName("Test identity read getters delegate to the database")
    void testIdentityGetters() {
        IdentityKey key = mock(IdentityKey.class);
        IdentityProperties props = mock(IdentityProperties.class);
        IdentityStats stats = mock(IdentityStats.class);
        Map<Integer, EquippedAbility> equipped = new HashMap<>();
        Map<Integer, String> runes = new HashMap<>();

        when(mockDatabase.getIdentityKey(identityId)).thenReturn(CompletableFuture.completedFuture(Optional.of(key)));
        when(mockDatabase.getProperties(identityId)).thenReturn(CompletableFuture.completedFuture(Optional.of(props)));
        when(mockDatabase.getStats(identityId)).thenReturn(CompletableFuture.completedFuture(Optional.of(stats)));
        when(mockDatabase.getEquippedAbilities(identityId)).thenReturn(CompletableFuture.completedFuture(equipped));
        when(mockDatabase.getRunes(identityId)).thenReturn(CompletableFuture.completedFuture(runes));

        assertSame(key, provider.getIdentityKey(identityId).join().orElseThrow());
        assertSame(props, provider.getProperties(identityId).join().orElseThrow());
        assertSame(stats, provider.getStats(identityId).join().orElseThrow());
        assertSame(equipped, provider.getEquippedAbilities(identityId).join());
        assertSame(runes, provider.getRunes(identityId).join());
    }

    @Test
    @DisplayName("Test session read getters delegate to the database")
    void testSessionGetters() {
        IdentitySession session = mock(IdentitySession.class);
        Map<String, String> cosmetics = new HashMap<>();
        when(mockDatabase.getActiveIdentity(playerId)).thenReturn(CompletableFuture.completedFuture(Optional.of(identityId)));
        when(mockDatabase.getSession(playerId)).thenReturn(CompletableFuture.completedFuture(Optional.of(session)));
        when(mockDatabase.getCosmetics(identityId)).thenReturn(CompletableFuture.completedFuture(cosmetics));

        assertEquals(Optional.of(identityId), provider.getActiveIdentity(playerId).join());
        assertSame(session, provider.getSession(playerId).join().orElseThrow());
        assertSame(cosmetics, provider.getCosmetics(identityId).join());

        verify(mockDatabase).getActiveIdentity(playerId);
        verify(mockDatabase).getSession(playerId);
        verify(mockDatabase).getCosmetics(identityId);
    }

    @Test
    @DisplayName("Test storage getters derive from properties")
    void testStorageGetters() {
        IdentityProperties props = mock(IdentityProperties.class);
        when(props.getInventoryContents()).thenReturn("inv-data");
        when(props.getEnderchestContents()).thenReturn("ec-data");
        when(mockDatabase.getProperties(identityId)).thenReturn(CompletableFuture.completedFuture(Optional.of(props)));

        assertEquals("inv-data", provider.getIdentityInventory(identityId).join());
        assertEquals("ec-data", provider.getIdentityEnderchest(identityId).join());
    }

    @Test
    @DisplayName("Test storage getters return null when the identity has no properties")
    void testStorageGettersMissing() {
        when(mockDatabase.getProperties(identityId)).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertNull(provider.getIdentityInventory(identityId).join());
        assertNull(provider.getIdentityEnderchest(identityId).join());
    }
}

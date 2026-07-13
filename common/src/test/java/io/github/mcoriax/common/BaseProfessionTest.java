package io.github.mcoriax.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Tests for {@link BaseProfession}, covering the primary identity binding and
 * the deterministic UUIDs of alternative base identities.
 */
class BaseProfessionTest {

    private final UUID playerId = UUID.randomUUID();

    @Test
    @DisplayName("The primary identity keeps the player's own UUID")
    void primaryIdentityUuid() {
        assertEquals(playerId, BaseProfession.identityUuid(playerId));
        assertEquals(playerId, BaseProfession.identityUuid(playerId, BaseProfession.PRIMARY_INDEX));
    }

    @Test
    @DisplayName("Alternative identity UUIDs are deterministic name-based UUIDs")
    void alternativeIdentityUuids() {
        UUID expected = UUID.nameUUIDFromBytes((playerId + "-3").getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, BaseProfession.identityUuid(playerId, 3));
        // The derivation is stable: the same pair always yields the same UUID.
        assertEquals(BaseProfession.identityUuid(playerId, 3), BaseProfession.identityUuid(playerId, 3));
    }

    @Test
    @DisplayName("Different indexes and players yield different alternative UUIDs")
    void alternativeIdentityUuidsAreDistinct() {
        assertNotEquals(BaseProfession.identityUuid(playerId, 1), BaseProfession.identityUuid(playerId, 2));
        assertNotEquals(BaseProfession.identityUuid(playerId, 1), BaseProfession.identityUuid(UUID.randomUUID(), 1));
        assertNotEquals(playerId, BaseProfession.identityUuid(playerId, 1));
    }

    @Test
    @DisplayName("identityIndex maps base identity UUIDs back to their index")
    void identityIndexRoundTrip() {
        assertEquals(BaseProfession.PRIMARY_INDEX, BaseProfession.identityIndex(playerId, playerId, 0));
        assertEquals(2, BaseProfession.identityIndex(playerId, BaseProfession.identityUuid(playerId, 2), 5));
        // Indexes beyond the unlocked slots are not recognised.
        assertEquals(-1, BaseProfession.identityIndex(playerId, BaseProfession.identityUuid(playerId, 6), 5));
        assertEquals(-1, BaseProfession.identityIndex(playerId, UUID.randomUUID(), 5));
        assertEquals(-1, BaseProfession.identityIndex(playerId, null, 5));
        assertEquals(-1, BaseProfession.identityIndex(null, playerId, 5));
    }
}

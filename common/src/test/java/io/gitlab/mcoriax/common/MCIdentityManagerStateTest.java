package io.gitlab.mcoriax.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the transient per-player state tracked by {@link MCIdentityManager}:
 * swap cooldowns and pending-equip flags.
 */
class MCIdentityManagerStateTest {

    private UUID playerId;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        MCIdentityManager.clear();
        MCIdentityManager.clearSwapCooldown(playerId);
        MCIdentityManager.setEquipPending(playerId, false);
    }

    @Test
    @DisplayName("Test swap cooldown logic")
    void testSwapCooldown() throws InterruptedException {
        MCIdentityManager.setIdentitySwapCooldownMinutes(1);
        assertFalse(MCIdentityManager.isOnSwapCooldown(playerId));

        MCIdentityManager.setSwapCooldown(playerId);
        assertTrue(MCIdentityManager.isOnSwapCooldown(playerId));

        long remaining = MCIdentityManager.getRemainingSwapCooldownMillis(playerId);
        assertTrue(remaining > 0 && remaining <= 60000);

        MCIdentityManager.clearSwapCooldown(playerId);
        assertFalse(MCIdentityManager.isOnSwapCooldown(playerId));
        assertEquals(0, MCIdentityManager.getRemainingSwapCooldownMillis(playerId));
    }

    @Test
    @DisplayName("Test pending equipment state")
    void testPendingEquip() {
        assertFalse(MCIdentityManager.isEquipPending(playerId));
        MCIdentityManager.setEquipPending(playerId, true);
        assertTrue(MCIdentityManager.isEquipPending(playerId));
        MCIdentityManager.setEquipPending(playerId, false);
        assertFalse(MCIdentityManager.isEquipPending(playerId));
    }
}

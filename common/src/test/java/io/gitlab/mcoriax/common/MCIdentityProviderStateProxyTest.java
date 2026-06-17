package io.gitlab.mcoriax.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link MCIdentityProvider}'s lifecycle helpers and the thin proxies
 * it exposes over {@link MCIdentityManager}'s in-RAM state.
 */
class MCIdentityProviderStateProxyTest extends ProviderTestSupport {

    @Test
    @DisplayName("Test hasDatabase reflects an initialized provider and shutdown delegates")
    void testHasDatabaseAndShutdown() {
        assertTrue(MCIdentityProvider.hasDatabase());

        provider.shutdown();
        verify(mockDatabase).shutdown();
    }

    @Test
    @DisplayName("Test swap cooldown proxies delegate to the manager")
    void testSwapCooldownProxies() {
        MCIdentityManager.setIdentitySwapCooldownMinutes(1);
        provider.clearSwapCooldown(playerId);
        assertFalse(provider.isOnSwapCooldown(playerId));

        provider.setSwapCooldown(playerId);
        assertTrue(provider.isOnSwapCooldown(playerId));
        assertTrue(provider.getRemainingSwapCooldownMillis(playerId) > 0);

        provider.clearSwapCooldown(playerId);
        assertFalse(provider.isOnSwapCooldown(playerId));
    }

    @Test
    @DisplayName("Test pending-equip proxies delegate to the manager")
    void testEquipPendingProxies() {
        provider.setEquipPending(playerId, true);
        assertTrue(provider.isEquipPending(playerId));
        provider.setEquipPending(playerId, false);
        assertFalse(provider.isEquipPending(playerId));
    }

    @Test
    @DisplayName("Test formatTime proxy delegates to the manager")
    void testFormatTimeProxy() {
        assertEquals("1m", provider.formatTime(60000));
        assertEquals("0s", provider.formatTime(0));
    }
}

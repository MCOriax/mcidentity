package io.gitlab.mcoriax.common;

import io.gitlab.mcoriax.api.model.ActiveIdentitySession;
import io.gitlab.mcoriax.api.model.IdentityKey;
import io.gitlab.mcoriax.api.model.IdentityProperties;
import io.gitlab.mcoriax.api.model.IdentityStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MCIdentityManagerTest {

    private UUID playerId;
    private UUID identityId;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        identityId = UUID.randomUUID();
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

    @Test
    @DisplayName("Test profession configuration storage")
    void testProfessionConfigs() {
        String profCode = "warrior";
        String displayName = "Warrior Class";
        
        MCIdentityManager.setProfessionDisplayName(profCode, displayName);
        assertEquals(displayName, MCIdentityManager.getProfessionDisplayName(profCode));
        assertEquals(displayName, MCIdentityManager.getProfessionDisplayName(profCode.toUpperCase()));

        MCIdentityManager.setProfessionLevelConfig(profCode, 50, 5000);
        MCIdentityManager.ProfessionLevelConfig config = MCIdentityManager.getProfessionLevelConfig(profCode);
        assertEquals(50, config.maxLevel());
        assertEquals(5000, config.expUp());
    }

    @Test
    @DisplayName("Test session caching and retrieval")
    void testSessionCaching() {
        IdentityKey key = Mockito.mock(IdentityKey.class);
        IdentityProperties props = Mockito.mock(IdentityProperties.class);
        IdentityStats stats = Mockito.mock(IdentityStats.class);
        
        ActiveIdentitySession session = new ActiveIdentitySession(
            playerId, identityId, key, props, stats, "content-key",
            100.0, 50.0, 20.0, Collections.emptyMap()
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
            100.0, 50.0, 20.0, Collections.emptyMap()
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

    @Test
    @DisplayName("Test time formatting")
    void testFormatTime() {
        assertEquals("0s", MCIdentityManager.formatTime(0));
        assertEquals("1s", MCIdentityManager.formatTime(1000));
        assertEquals("1m", MCIdentityManager.formatTime(60000));
        assertEquals("1m 1s", MCIdentityManager.formatTime(61000));
        assertEquals("59s", MCIdentityManager.formatTime(59000));
    }
}

package io.github.mcoriax.mcidentity.common;

import io.github.mcoriax.mcidentity.api.model.ActiveProfessionSession;
import io.github.mcoriax.mcidentity.common.model.PlayerStatsRecord;
import io.github.mcoriax.mcidentity.common.model.ProfessionProgressRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for caching and mutating the in-RAM {@link ActiveProfessionSession} store.
 */
class MCIdentityManagerSessionTest {

    private UUID playerId;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        MCIdentityManager.clear();
    }

    private static PlayerStatsRecord stats(int statPoints) {
        return new PlayerStatsRecord(statPoints, 100, 20, 20, 0, 0, 0, 0, 0, 0);
    }

    private ActiveProfessionSession session(String code) {
        return new ActiveProfessionSession(playerId, code, new ProfessionProgressRecord(3, 40), stats(5),
            100.0, 50.0, 20.0);
    }

    @Test
    @DisplayName("A cached session is read back by player UUID and removed on quit")
    void testSessionCaching() {
        MCIdentityManager.cacheSession(session("miner"));

        Optional<ActiveProfessionSession> cached = MCIdentityManager.getSession(playerId);
        assertTrue(cached.isPresent());
        assertEquals("miner", cached.get().professionCode());
        assertEquals(Optional.of("miner"), MCIdentityManager.getActiveProfession(playerId));

        MCIdentityManager.removeSession(playerId);
        assertFalse(MCIdentityManager.getSession(playerId).isPresent());
        assertTrue(MCIdentityManager.getActiveProfession(playerId).isEmpty());
    }

    @Test
    @DisplayName("Current values and stats update in place")
    void testSessionUpdates() {
        MCIdentityManager.cacheSession(session("miner"));

        MCIdentityManager.updateHealth(playerId, 80.0);
        assertEquals(80.0, MCIdentityManager.getSession(playerId).get().currentHealth());

        MCIdentityManager.updateEnergy(playerId, 30.0);
        assertEquals(30.0, MCIdentityManager.getSession(playerId).get().currentEnergy());

        MCIdentityManager.updateFood(playerId, 15.0);
        assertEquals(15.0, MCIdentityManager.getSession(playerId).get().currentFood());

        PlayerStatsRecord newStats = stats(9);
        MCIdentityManager.updateStats(playerId, newStats);
        assertEquals(newStats, MCIdentityManager.getSession(playerId).get().stats());
    }

    @Test
    @DisplayName("Progress updates only apply to the profession that is worn")
    void testProgressOnlyForWornProfession() {
        MCIdentityManager.cacheSession(session("miner"));

        MCIdentityManager.updateProgress(playerId, "fighter", new ProfessionProgressRecord(9, 0));
        assertEquals(3, MCIdentityManager.getSession(playerId).get().progress().getLevel());

        MCIdentityManager.updateProgress(playerId, "miner", new ProfessionProgressRecord(4, 1));
        assertEquals(4, MCIdentityManager.getSession(playerId).get().progress().getLevel());
    }

    @Test
    @DisplayName("Changing profession swaps the code and progress and keeps the stats")
    void testUpdateProfession() {
        MCIdentityManager.cacheSession(session("miner"));

        MCIdentityManager.updateProfession(playerId, "fighter", new ProfessionProgressRecord(7, 2));

        ActiveProfessionSession updated = MCIdentityManager.getSession(playerId).get();
        assertEquals("fighter", updated.professionCode());
        assertEquals(7, updated.progress().getLevel());
        assertEquals(5, updated.stats().getStatPoints());
    }
}

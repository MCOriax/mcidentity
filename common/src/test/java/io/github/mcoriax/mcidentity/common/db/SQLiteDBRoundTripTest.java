package io.github.mcoriax.mcidentity.common.db;

import io.github.mcoriax.mcidentity.api.model.ActionType;
import io.github.mcoriax.mcidentity.api.model.PlayerStats;
import io.github.mcoriax.mcidentity.api.model.Profession;
import io.github.mcoriax.mcidentity.api.model.ProfessionProgress;
import io.github.mcoriax.mcidentity.common.model.PlayerStatsRecord;
import io.github.mcoriax.mcidentity.common.model.ProfessionProgressRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests running {@link SQLiteDB} against a real database file
 * through the SQLite JDBC driver, covering schema creation and every data
 * flow the plugin uses.
 */
class SQLiteDBRoundTripTest {

    @TempDir
    Path tempDir;

    private Path file;
    private SQLiteDB db;
    private UUID playerId;

    @BeforeEach
    void openDatabase() {
        file = tempDir.resolve("test.db");
        db = new SQLiteDB(file.toString());
        db.initializeSchema().join();
        playerId = UUID.randomUUID();
    }

    @AfterEach
    void closeDatabase() {
        db.shutdown();
    }

    private static PlayerStatsRecord stats(int statPoints) {
        return new PlayerStatsRecord(statPoints, 100, 20, 20, 7, 4, 1, 2, 3, 5);
    }

    @Test
    @DisplayName("Schema initialization can run twice")
    void schemaIsIdempotent() {
        assertDoesNotThrow(() -> db.initializeSchema().join());
    }

    @Test
    @DisplayName("Professions are registered once and listed")
    void professions() {
        db.createProfession("miner", "Miner").join();
        db.createProfession("miner", "Miner again").join();

        Collection<Profession> professions = db.getProfessions().join();
        assertEquals(1, professions.stream().filter(p -> p.getSystemCode().equals("miner")).count());
    }

    @Test
    @DisplayName("createPlayer stores the starting stats once and never overwrites them")
    void createPlayerIsInsertOnly() {
        db.createPlayer(playerId, stats(0)).join();
        db.createPlayer(playerId, stats(99)).join();

        PlayerStats stored = db.getStats(playerId).join().orElseThrow();
        assertEquals(0, stored.getStatPoints());
        assertEquals(7, stored.getStrength());
        assertEquals(5, stored.getResilience());
    }

    @Test
    @DisplayName("Stats update as a whole and one stat at a time")
    void statsUpdate() {
        db.createPlayer(playerId, stats(0)).join();

        db.updateStats(playerId, stats(12)).join();
        assertEquals(12, db.getStats(playerId).join().orElseThrow().getStatPoints());

        db.addStat(playerId, "strength", 3).join();
        assertEquals(10, db.getStat(playerId, "strength").join());
        assertEquals(0, db.getStat(playerId, "not_a_stat").join());
    }

    @Test
    @DisplayName("The worn profession is empty until set, then replaced in place")
    void activeProfession() {
        assertTrue(db.getActiveProfession(playerId).join().isEmpty());

        db.setActiveProfession(playerId, "miner").join();
        assertEquals(Optional.of("miner"), db.getActiveProfession(playerId).join());

        db.setActiveProfession(playerId, "fighter").join();
        assertEquals(Optional.of("fighter"), db.getActiveProfession(playerId).join());
    }

    @Test
    @DisplayName("Progress is kept per profession")
    void progressPerProfession() {
        assertTrue(db.getProgress(playerId, "miner").join().isEmpty());

        db.updateProgress(playerId, "miner", new ProfessionProgressRecord(4, 30)).join();
        db.updateProgress(playerId, "fighter", new ProfessionProgressRecord(2, 5)).join();
        db.updateProgress(playerId, "miner", new ProfessionProgressRecord(5, 1)).join();

        ProfessionProgress miner = db.getProgress(playerId, "miner").join().orElseThrow();
        ProfessionProgress fighter = db.getProgress(playerId, "fighter").join().orElseThrow();
        assertEquals(5, miner.getLevel());
        assertEquals(1, miner.getExp());
        assertEquals(2, fighter.getLevel());
    }

    @Test
    @DisplayName("Profession changes are logged with the player UUID")
    void logAction() throws Exception {
        db.logAction(playerId, "miner", ActionType.EQUIP).join();
        db.logAction(playerId, "miner", ActionType.UNEQUIP).join();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + file);
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM profession_logs WHERE player_uuid = ? AND profession_code = 'miner'")) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1));
            }
        }
    }
}

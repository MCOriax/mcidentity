package io.github.mcoriax.mcidentity.common.db;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import io.github.mcoriax.mcidentity.api.model.ActionType;
import io.github.mcoriax.mcidentity.api.model.PlayerStats;
import io.github.mcoriax.mcidentity.common.model.PlayerStatsRecord;
import io.github.mcoriax.mcidentity.common.model.ProfessionProgressRecord;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Delegation tests for the MongoDB backend over a mocked database handle.
 */
class MongoDBTest {

    /**
     * Backend wired to the mocked database handle.
     */
    static class TestMongoDB extends MongoDB {
        TestMongoDB(MongoDatabase database) {
            super(database);
        }
    }

    private MongoDatabase mockDatabase;
    private MongoCollection<Document> mockCollection;
    private FindIterable<Document> mockFind;
    private UUID playerId;
    private TestMongoDB db;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUpFixtures() {
        mockDatabase = mock(MongoDatabase.class);
        mockCollection = mock(MongoCollection.class);
        mockFind = mock(FindIterable.class);
        playerId = UUID.randomUUID();
        db = new TestMongoDB(mockDatabase);

        when(mockDatabase.getCollection(anyString())).thenReturn(mockCollection);
        when(mockCollection.find(any(Bson.class))).thenReturn(mockFind);
    }

    @AfterEach
    void shutdown() {
        db.shutdown();
    }

    @Test
    @DisplayName("Professions upsert by system code")
    void professionUpsert() {
        db.createProfession("warrior", "Warrior").join();

        ArgumentCaptor<Bson> update = ArgumentCaptor.forClass(Bson.class);
        verify(mockCollection).updateOne(any(Bson.class), update.capture(), any(UpdateOptions.class));
        Document set = (Document) ((Document) update.getValue()).get("$set");
        assertEquals("warrior", set.getString("system_code"));
        assertEquals("Warrior", set.getString("display_name"));
    }

    @Test
    @DisplayName("A new player's stats are inserted only when absent")
    void createPlayerSetsOnInsert() {
        db.createPlayer(playerId, new PlayerStatsRecord(0, 100, 20, 20, 1, 2, 3, 4, 5, 6)).join();

        verify(mockDatabase).getCollection("player_stats");
        ArgumentCaptor<Bson> update = ArgumentCaptor.forClass(Bson.class);
        verify(mockCollection).updateOne(any(Bson.class), update.capture(), any(UpdateOptions.class));
        assertTrue(((Document) update.getValue()).containsKey("$setOnInsert"));
    }

    @Test
    @DisplayName("Stats read from the player's document")
    void statsRead() {
        Document stats = new Document("stat_points", 7).append("energy", 90).append("health", 25).append("strength", 8);
        when(mockFind.first()).thenReturn(stats);

        PlayerStats read = db.getStats(playerId).join().orElseThrow();
        assertEquals(7, read.getStatPoints());
        assertEquals(90, read.getEnergy());
        assertEquals(8, read.getStrength());
    }

    @Test
    @DisplayName("The worn profession is read from player_professions")
    void activeProfessionRead() {
        when(mockFind.first()).thenReturn(new Document("active_profession", "miner"));

        assertEquals(Optional.of("miner"), db.getActiveProfession(playerId).join());
        verify(mockDatabase).getCollection("player_professions");
    }

    @Test
    @DisplayName("Progress is upserted per player and profession")
    void progressUpsert() {
        db.updateProgress(playerId, "miner", new ProfessionProgressRecord(3, 9)).join();

        verify(mockDatabase).getCollection("player_profession_progress");
        verify(mockCollection).updateOne(any(Bson.class), any(Bson.class), any(UpdateOptions.class));
    }

    @Test
    @DisplayName("Logs go to profession_logs")
    void logAction() {
        db.logAction(playerId, "miner", ActionType.EQUIP).join();

        verify(mockDatabase).getCollection("profession_logs");
        verify(mockCollection).insertOne(any(Document.class));
    }

    @Test
    @DisplayName("Stat keys outside the allow-list never reach MongoDB")
    void statAllowList() {
        db.addStat(playerId, "$where", 1).join();
        assertEquals(0, db.getStat(playerId, "1=1").join());
        verify(mockDatabase, never()).getCollection(anyString());
    }
}

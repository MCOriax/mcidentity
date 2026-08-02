package io.github.mcoriax.mcidentity.common.db;

import io.github.mcoriax.mcidentity.api.model.ActionType;
import io.github.mcoriax.mcidentity.api.model.IdentityKey;
import io.github.mcoriax.mcidentity.api.model.IdentityProperties;
import io.github.mcoriax.mcidentity.api.model.IdentitySession;
import io.github.mcoriax.mcidentity.api.model.IdentityStats;
import io.github.mcoriax.mcidentity.common.model.MutableIdentityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests running {@link SQLiteDB} against a real database file
 * through the SQLite JDBC driver, covering schema creation and every data
 * flow the plugin uses.
 */
class SQLiteDBRoundTripTest {

    @TempDir
    Path tempDir;

    private SQLiteDB db;
    private UUID identityId;
    private UUID playerId;

    @BeforeEach
    void openDatabase() {
        db = new SQLiteDB(tempDir.resolve("test.db").toString());
        db.initializeSchema().join();
        identityId = UUID.randomUUID();
        playerId = UUID.randomUUID();
    }

    @AfterEach
    void closeDatabase() {
        db.shutdown();
    }

    private IdentityKey key(UUID uuid, String profession) {
        return new IdentityKey() {
            @Override public UUID getUniqueId() { return uuid; }
            @Override public String getProfessionCode() { return profession; }
        };
    }

    private IdentityProperties properties() {
        return new MutableIdentityProperties(null, 3, 250, 5, "head", null, null);
    }

    private IdentityStats stats() {
        return new IdentityStats() {
            @Override public int getEnergy() { return 100; }
            @Override public int getHealth() { return 20; }
            @Override public int getFood() { return 20; }
            @Override public int getStrength() { return 7; }
            @Override public int getDefense() { return 4; }
            @Override public int getWisdom() { return 1; }
            @Override public int getLuck() { return 2; }
            @Override public int getAgility() { return 3; }
            @Override public int getResilience() { return 6; }
        };
    }

    @Test
    @DisplayName("Schema initialization is idempotent")
    void schemaIsIdempotent() {
        db.initializeSchema().join();
    }

    @Test
    @DisplayName("Professions register once and list back")
    void professionRoundTrip() {
        db.createProfession("archer", "Archer").join();
        db.createProfession("archer", "Renamed").join();
        assertEquals(1, db.getProfessions().join().size());
        assertEquals("Archer", db.getProfessions().join().iterator().next().getDisplayName());
    }

    @Test
    @DisplayName("Identity creation persists key, properties, and stats")
    void identityRoundTrip() {
        db.createProfession("archer", "Archer").join();
        db.createIdentity(key(identityId, "archer"), properties(), stats()).join();

        assertEquals("archer", db.getIdentityKey(identityId).join().orElseThrow().getProfessionCode());
        IdentityProperties props = db.getProperties(identityId).join().orElseThrow();
        assertEquals(3, props.getLevel());
        assertEquals(250, props.getExp());
        assertEquals(5, props.getStatPoints());
        assertEquals("head", props.getHeadText());
        assertEquals(7, db.getStats(identityId).join().orElseThrow().getStrength());
        assertEquals(7, db.getStat(identityId, "strength").join());
    }

    @Test
    @DisplayName("Creating a duplicate identity fails exceptionally")
    void duplicateIdentityFails() {
        db.createIdentity(key(identityId, "archer"), properties(), stats()).join();
        assertThrows(Exception.class,
            () -> db.createIdentity(key(identityId, "archer"), properties(), stats()).join());
    }

    @Test
    @DisplayName("Invalid profession codes are rejected")
    void invalidProfessionRejected() {
        assertThrows(Exception.class,
            () -> db.createIdentity(key(identityId, "random"), properties(), stats()).join());
        assertThrows(Exception.class,
            () -> db.createIdentity(key(identityId, "  "), properties(), stats()).join());
    }

    @Test
    @DisplayName("Stats update, single-stat add, and allow-list enforcement")
    void statFlows() {
        db.createIdentity(key(identityId, "archer"), properties(), stats()).join();

        db.addStat(identityId, "strength", 5).join();
        assertEquals(12, db.getStat(identityId, "strength").join());

        // Unknown keys never reach SQL and resolve to their neutral values.
        db.addStat(identityId, "strength; DROP TABLE identity_stats", 5).join();
        assertEquals(0, db.getStat(identityId, "energy; --").join());
        assertEquals(12, db.getStat(identityId, "strength").join());

        db.updateStats(identityId, stats()).join();
        assertEquals(7, db.getStat(identityId, "strength").join());
    }

    @Test
    @DisplayName("Aliases store the original casing and resolve back in any casing")
    void aliasRoundTrip() {
        db.createIdentity(key(identityId, "archer"), properties(), stats()).join();

        assertNull(db.getProperties(identityId).join().orElseThrow().getAlias());
        assertTrue(db.setAlias(identityId, "Alex").join());

        assertEquals("Alex", db.getProperties(identityId).join().orElseThrow().getAlias());
        assertEquals(identityId, db.getIdentityByAlias("Alex").join().orElseThrow());
        assertEquals(identityId, db.getIdentityByAlias("alex").join().orElseThrow());
        assertEquals(identityId, db.getIdentityByAlias("ALEX").join().orElseThrow());
    }

    @Test
    @DisplayName("Aliases are unique across identities regardless of casing")
    void aliasUniquenessIgnoresCase() {
        UUID otherId = UUID.randomUUID();
        db.createIdentity(key(identityId, "archer"), properties(), stats()).join();
        db.createIdentity(key(otherId, "archer"), properties(), stats()).join();

        assertTrue(db.setAlias(identityId, "Alex").join());
        assertFalse(db.setAlias(otherId, "alex").join());
        assertFalse(db.setAlias(otherId, "ALEX").join());
        assertNull(db.getProperties(otherId).join().orElseThrow().getAlias());

        // Reclaiming its own alias in a different casing is a rename, not a clash.
        assertTrue(db.setAlias(identityId, "ALEX").join());
        assertEquals("ALEX", db.getProperties(identityId).join().orElseThrow().getAlias());

        // Releasing it frees the name for the other identity.
        assertTrue(db.setAlias(identityId, null).join());
        assertTrue(db.setAlias(otherId, "alex").join());
    }

    @Test
    @DisplayName("Aliases outside the alphanumeric rule are rejected")
    void aliasValidation() {
        db.createIdentity(key(identityId, "archer"), properties(), stats()).join();

        assertFalse(db.setAlias(identityId, "has space").join());
        assertFalse(db.setAlias(identityId, "under_score").join());
        assertFalse(db.setAlias(identityId, "").join());
        assertFalse(db.setAlias(identityId, "a".repeat(33)).join());
        assertNull(db.getProperties(identityId).join().orElseThrow().getAlias());

        // A malformed alias never reaches SQL, so it resolves to nothing.
        assertTrue(db.getIdentityByAlias("' OR 1=1 --").join().isEmpty());
    }

    @Test
    @DisplayName("A property write leaves the alias untouched")
    void propertiesUpdateKeepsAlias() {
        db.createIdentity(key(identityId, "archer"), properties(), stats()).join();
        db.setAlias(identityId, "Alex").join();

        db.updateProperties(identityId, new MutableIdentityProperties(null, 9, 1, 2, "newhead", "inv", "ender")).join();

        assertEquals("Alex", db.getProperties(identityId).join().orElseThrow().getAlias());
    }

    @Test
    @DisplayName("Properties update round trip")
    void propertiesUpdate() {
        db.createIdentity(key(identityId, "archer"), properties(), stats()).join();
        db.updateProperties(identityId, new MutableIdentityProperties(null, 9, 1, 2, "newhead", "inv", "ender")).join();

        IdentityProperties props = db.getProperties(identityId).join().orElseThrow();
        assertEquals(9, props.getLevel());
        assertEquals("newhead", props.getHeadText());
        assertEquals("inv", props.getInventoryContents());
        assertEquals("ender", props.getEnderchestContents());
    }

    @Test
    @DisplayName("Sessions equip, read back, rewrite content key, and clear")
    void sessionFlows() {
        db.createIdentity(key(identityId, "archer"), properties(), stats()).join();

        db.setActiveIdentity(playerId, identityId, "content").join();
        IdentitySession session = db.getSession(playerId).join().orElseThrow();
        assertEquals(identityId, session.getIdentityUniqueId());
        assertEquals("content", session.getContentKey());
        assertEquals(identityId, db.getActiveIdentity(playerId).join().orElseThrow());

        db.updateSessionContentKey(playerId, "rewritten").join();
        assertEquals("rewritten", db.getSession(playerId).join().orElseThrow().getContentKey());

        db.setActiveIdentity(playerId, null, null).join();
        assertFalse(db.getSession(playerId).join().isPresent());
    }

    @Test
    @DisplayName("Tables retired by the add-on split are dropped on upgrade")
    void retiredAddonTablesAreDropped() throws Exception {
        Path legacy = tempDir.resolve("legacy.db");
        String url = "jdbc:sqlite:" + legacy;
        try (Connection conn = DriverManager.getConnection(url); Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE TABLE identity_runes (system_code TEXT PRIMARY KEY)");
            st.executeUpdate("CREATE TABLE identity_equipped_runes (identity_uuid TEXT, slot INTEGER)");
            st.executeUpdate("CREATE TABLE identity_equipped_cosmetics (identity_uuid TEXT PRIMARY KEY)");
        }

        SQLiteDB upgraded = new SQLiteDB(legacy.toString());
        try {
            upgraded.initializeSchema().join();
        } finally {
            upgraded.shutdown();
        }

        try (Connection conn = DriverManager.getConnection(url); Statement st = conn.createStatement()) {
            for (String table : new String[]{"identity_runes", "identity_equipped_runes", "identity_equipped_cosmetics"}) {
                try (ResultSet rs = st.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='" + table + "'")) {
                    assertFalse(rs.next(), table + " should have been dropped");
                }
            }
            // The tables the core still owns survive the upgrade untouched.
            try (ResultSet rs = st.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='identity_keys'")) {
                assertTrue(rs.next(), "identity_keys should still exist");
            }
        }
    }

    @Test
    @DisplayName("A fresh schema carries the alias directly after identity_uuid")
    void aliasColumnFollowsIdentityUuid() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + tempDir.resolve("test.db"));
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(identity_properties)")) {
            assertTrue(rs.next());
            assertEquals("identity_uuid", rs.getString("name"));
            assertTrue(rs.next());
            assertEquals("alias", rs.getString("name"));
        }
    }

    @Test
    @DisplayName("A database predating aliases gains the column on the next start")
    void aliasColumnIsAddedToAnExistingDatabase() throws Exception {
        Path legacy = tempDir.resolve("legacy-properties.db");
        String url = "jdbc:sqlite:" + legacy;
        try (Connection conn = DriverManager.getConnection(url); Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE TABLE identity_properties (identity_uuid VARCHAR(36) PRIMARY KEY, "
                + "current_level INT DEFAULT 1, exp INT DEFAULT 0, stat_points INT DEFAULT 0, "
                + "head_text TEXT DEFAULT NULL, inventory_contents TEXT DEFAULT NULL, "
                + "enderchest_contents TEXT DEFAULT NULL)");
            st.executeUpdate("INSERT INTO identity_properties (identity_uuid) VALUES ('" + identityId + "')");
        }

        SQLiteDB upgraded = new SQLiteDB(legacy.toString());
        try {
            upgraded.initializeSchema().join();
            // The pre-existing row survives and can claim an alias afterwards.
            assertNull(upgraded.getProperties(identityId).join().orElseThrow().getAlias());
            assertTrue(upgraded.setAlias(identityId, "Legacy1").join());
            assertEquals(identityId, upgraded.getIdentityByAlias("legacy1").join().orElseThrow());
        } finally {
            upgraded.shutdown();
        }
    }

    @Test
    @DisplayName("Audit log inserts succeed")
    void logActions() {
        db.createIdentity(key(identityId, "archer"), properties(), stats()).join();
        db.logAction(playerId, identityId, ActionType.EQUIP).join();
    }

    @Test
    @DisplayName("Base identity slots accumulate and never drop below zero")
    void baseIdentitySlotsRoundTrip() {
        assertEquals(0, db.getBaseIdentitySlots(playerId).join());
        assertEquals(2, db.addBaseIdentitySlots(playerId, 2).join());
        assertEquals(5, db.addBaseIdentitySlots(playerId, 3).join());
        assertEquals(5, db.getBaseIdentitySlots(playerId).join());
        assertEquals(0, db.addBaseIdentitySlots(playerId, -100).join());
        assertEquals(0, db.getBaseIdentitySlots(playerId).join());
    }
}

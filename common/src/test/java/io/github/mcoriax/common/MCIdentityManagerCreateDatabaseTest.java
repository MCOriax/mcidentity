package io.github.mcoriax.common;

import io.github.mcoriax.api.db.IMCIdentityDB;
import io.github.mcoriax.common.db.MySQLDB;
import io.github.mcoriax.common.db.PostgreSQLDB;
import io.github.mcoriax.common.db.SQLiteDB;
import io.github.mcoriax.common.db.central.MCIdentityCentralServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the {@code database.type} dispatch in
 * {@link MCIdentityManager#createDatabase(String, Map)}.
 */
class MCIdentityManagerCreateDatabaseTest {

    @Test
    @DisplayName("sqlite type builds the SQLite backend")
    void sqliteType() {
        IMCIdentityDB db = MCIdentityManager.createDatabase("sqlite", Map.of("file", "test.db"));
        assertInstanceOf(SQLiteDB.class, db);
        db.shutdown();
    }

    @Test
    @DisplayName("mysql type builds the MySQL backend")
    void mysqlType() {
        IMCIdentityDB db = MCIdentityManager.createDatabase("MySQL",
            Map.of("host", "localhost", "port", 3306, "database", "db", "username", "u", "password", "p"));
        assertInstanceOf(MySQLDB.class, db);
        db.shutdown();
    }

    @Test
    @DisplayName("postgresql type and its alias build the PostgreSQL backend")
    void postgresType() {
        IMCIdentityDB db = MCIdentityManager.createDatabase("postgresql", Map.of());
        assertInstanceOf(PostgreSQLDB.class, db);
        db.shutdown();

        IMCIdentityDB alias = MCIdentityManager.createDatabase("postgres", Map.of("port", "5433"));
        assertInstanceOf(PostgreSQLDB.class, alias);
        alias.shutdown();
    }

    @Test
    @DisplayName("mongodb type builds the MongoDB backend")
    void mongoType() {
        IMCIdentityDB db = MCIdentityManager.createDatabase("mongodb",
            Map.of("uri", "mongodb://localhost:27017", "database", "db"));
        assertInstanceOf(io.github.mcoriax.common.db.MongoDB.class, db);
        db.shutdown();
    }

    @Test
    @DisplayName("central type builds the central server client")
    void centralType() {
        IMCIdentityDB db = MCIdentityManager.createDatabase("central",
            Map.of("url", "https://server.com", "token", "t", "timeout_seconds", 5));
        assertInstanceOf(MCIdentityCentralServer.class, db);
        db.shutdown();
    }

    @Test
    @DisplayName("Unknown types are rejected")
    void unknownType() {
        assertThrows(IllegalArgumentException.class, () -> MCIdentityManager.createDatabase("oracle", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> MCIdentityManager.createDatabase(null, Map.of()));
    }
}

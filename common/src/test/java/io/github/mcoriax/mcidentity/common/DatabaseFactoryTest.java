package io.github.mcoriax.mcidentity.common;

import io.github.mcoriax.mcidentity.api.db.IMCIdentityDB;
import io.github.mcoriax.mcidentity.common.db.MySQLDB;
import io.github.mcoriax.mcidentity.common.db.PostgreSQLDB;
import io.github.mcoriax.mcidentity.common.db.SQLiteDB;
import io.github.mcoriax.mcidentity.common.central.MCIdentityCentralServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@code database.type} dispatch in {@link DatabaseFactory}, the
 * one place in the codebase that names a concrete backend.
 */
class DatabaseFactoryTest {

    @Test
    @DisplayName("sqlite type builds the SQLite backend")
    void sqliteType() {
        IMCIdentityDB db = DatabaseFactory.create("sqlite", Map.of("file", "test.db"));
        assertInstanceOf(SQLiteDB.class, db);
        db.shutdown();
    }

    @Test
    @DisplayName("mysql type builds the MySQL backend")
    void mysqlType() {
        IMCIdentityDB db = DatabaseFactory.create("MySQL",
            Map.of("host", "localhost", "port", 3306, "database", "db", "username", "u", "password", "p"));
        assertInstanceOf(MySQLDB.class, db);
        db.shutdown();
    }

    @Test
    @DisplayName("postgresql type and its alias build the PostgreSQL backend")
    void postgresType() {
        IMCIdentityDB db = DatabaseFactory.create("postgresql", Map.of());
        assertInstanceOf(PostgreSQLDB.class, db);
        db.shutdown();

        IMCIdentityDB alias = DatabaseFactory.create("postgres", Map.of("port", "5433"));
        assertInstanceOf(PostgreSQLDB.class, alias);
        alias.shutdown();
    }

    @Test
    @DisplayName("mongodb type builds the MongoDB backend")
    void mongoType() {
        IMCIdentityDB db = DatabaseFactory.create("mongodb",
            Map.of("uri", "mongodb://localhost:27017", "database", "db"));
        assertInstanceOf(io.github.mcoriax.mcidentity.common.db.MongoDB.class, db);
        db.shutdown();
    }

    @Test
    @DisplayName("central type builds the central server client")
    void centralType() {
        IMCIdentityDB db = DatabaseFactory.create("central",
            Map.of("url", "https://server.com", "token", "t", "timeout_seconds", 5));
        assertInstanceOf(MCIdentityCentralServer.class, db);
        db.shutdown();
    }

    @Test
    @DisplayName("Unknown types are rejected")
    void unknownType() {
        assertThrows(IllegalArgumentException.class, () -> DatabaseFactory.create("oracle", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> DatabaseFactory.create(null, Map.of()));
    }

    @Test
    @DisplayName("Only the central server backend counts as remote storage")
    void remoteClassification() {
        IMCIdentityDB local = DatabaseFactory.create("sqlite", Map.of("file", "test.db"));
        IMCIdentityDB remote = DatabaseFactory.create("central", Map.of("url", "https://server.com"));
        try {
            assertFalse(DatabaseFactory.isRemote(local));
            assertTrue(DatabaseFactory.isRemote(remote));
            // A local backend has nothing to wake, so the probe resolves true
            // without any network call.
            assertTrue(DatabaseFactory.ping(local).join());
        } finally {
            local.shutdown();
            remote.shutdown();
        }
    }

    @Test
    @DisplayName("The provider factory installs the singleton without exposing the backend")
    void providerCreateInstallsSingleton() {
        MCIdentityProvider provider = MCIdentityProvider.create("sqlite", Map.of("file", "test.db"));
        try {
            assertSame(provider, MCIdentityProvider.instance);
            assertTrue(MCIdentityProvider.hasDatabase());
            assertFalse(provider.isRemoteStorage());
        } finally {
            provider.shutdown();
        }
    }

    @Test
    @DisplayName("The deprecated manager entry point still delegates to the factory")
    @SuppressWarnings("deprecation")
    void deprecatedManagerDelegates() {
        IMCIdentityDB db = MCIdentityManager.createDatabase("sqlite", Map.of("file", "test.db"));
        assertInstanceOf(SQLiteDB.class, db);
        db.shutdown();
    }
}

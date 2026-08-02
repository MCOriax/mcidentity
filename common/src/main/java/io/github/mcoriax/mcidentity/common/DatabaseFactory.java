package io.github.mcoriax.mcidentity.common;

import io.github.mcoriax.mcidentity.api.db.IMCIdentityDB;
import io.github.mcoriax.mcidentity.common.central.MCIdentityCentralServer;
import io.github.mcoriax.mcidentity.common.db.MongoDB;
import io.github.mcoriax.mcidentity.common.db.MySQLDB;
import io.github.mcoriax.mcidentity.common.db.PostgreSQLDB;
import io.github.mcoriax.mcidentity.common.db.SQLiteDB;

import java.util.Locale;
import java.util.Map;

/**
 * Builds the storage backend named by {@code database.type}.
 *
 * <p><strong>Internal.</strong> This class is package-private on purpose: it is
 * the only place in the codebase that names a concrete backend, so the
 * {@code common.db} and {@code common.central} implementations stay reachable
 * from exactly one call site. Everything outside {@code common} obtains storage
 * through {@link MCIdentityProvider#create(String, Map)} and then talks to
 * {@link MCIdentityProvider}, never to a backend directly.</p>
 */
final class DatabaseFactory {

    /**
     * Prevents instantiation of this factory holder.
     */
    private DatabaseFactory() {
    }

    /**
     * Creates the database backend matching the configured
     * {@code database.type} value.
     *
     * <p>Supported types are {@code sqlite} (the default), {@code mysql},
     * {@code postgresql}, {@code mongodb}, and {@code central} for the
     * MCIdentity central REST server. The options map carries the connection
     * settings of the selected backend's configuration section, e.g.
     * {@code file} for SQLite; {@code host}, {@code port}, {@code database},
     * {@code username}, and {@code password} for MySQL and PostgreSQL;
     * {@code uri} and {@code database} for MongoDB; and {@code url},
     * {@code token}, and {@code timeout_seconds} for the central server.</p>
     *
     * @param type The configured database type.
     * @param options The connection settings for that type.
     * @return The database backend, not yet schema-initialized.
     * @throws IllegalArgumentException When the type is unknown.
     */
    static IMCIdentityDB create(String type, Map<String, Object> options) {
        String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "sqlite" -> new SQLiteDB(
                stringOption(options, "file", "mcidentity.db"));
            case "mysql", "mariadb" -> new MySQLDB(
                stringOption(options, "host", "localhost"),
                intOption(options, "port", 3306),
                stringOption(options, "database", "mcidentity"),
                stringOption(options, "username", "root"),
                stringOption(options, "password", ""));
            case "postgresql", "postgres" -> new PostgreSQLDB(
                stringOption(options, "host", "localhost"),
                intOption(options, "port", 5432),
                stringOption(options, "database", "mcidentity"),
                stringOption(options, "username", "postgres"),
                stringOption(options, "password", ""));
            case "mongodb", "mongo" -> new MongoDB(
                stringOption(options, "uri", "mongodb://localhost:27017"),
                stringOption(options, "database", "mcidentity"));
            case "central", "central_server" -> new MCIdentityCentralServer(
                stringOption(options, "url", "https://server.com"),
                stringOption(options, "token", ""),
                intOption(options, "timeout_seconds", 10));
            default -> throw new IllegalArgumentException("Unknown database.type: " + type);
        };
    }

    /**
     * Reports whether a backend reaches a remote MCIdentity central server
     * rather than a database this process can talk to directly.
     *
     * <p>Lives here because it is the one other place that has to know a
     * concrete backend type; asking it through {@link MCIdentityProvider} keeps
     * that knowledge out of the platform modules.</p>
     * @param database The backend to classify.
     * @return true when the backend is the central server client.
     */
    static boolean isRemote(IMCIdentityDB database) {
        return database instanceof MCIdentityCentralServer;
    }

    /**
     * Probes a remote backend's liveness, waking a cold-started deployment in
     * the process.
     * @param database The backend to probe.
     * @return A future resolving to whether the backend answered, or to true
     *         immediately when the backend is not remote and therefore has
     *         nothing to wake.
     */
    static java.util.concurrent.CompletableFuture<Boolean> ping(IMCIdentityDB database) {
        if (database instanceof MCIdentityCentralServer centralServer) {
            return centralServer.pingHealth();
        }
        return java.util.concurrent.CompletableFuture.completedFuture(true);
    }

    /**
     * Reads a string connection option.
     * @param options The connection settings.
     * @param key The option key.
     * @param fallback The value used when the option is absent or null.
     * @return The option value.
     */
    private static String stringOption(Map<String, Object> options, String key, String fallback) {
        Object value = options.get(key);
        return value != null ? value.toString() : fallback;
    }

    /**
     * Reads an integer connection option.
     * @param options The connection settings.
     * @param key The option key.
     * @param fallback The value used when the option is absent or not a number.
     * @return The option value.
     */
    private static int intOption(Map<String, Object> options, String key, int fallback) {
        Object value = options.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }
}

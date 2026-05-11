package io.gitlab.mcoriax.common.db.postgresql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.gitlab.mcoriax.api.db.IMCIdentityDB;
import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.common.db.postgresql.utils.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * PostgreSQL implementation of IMCIdentityDB.
 */
public abstract class MCIdentityPostgreSQL implements IMCIdentityDB {

    /**
     * Hikari data source for connection pooling.
     */
    private final HikariDataSource dataSource;

    /**
     * Initializes a new PostgreSQL database provider with connection pooling.
     * @param host The host address.
     * @param port The port number.
     * @param database The database name.
     * @param username The username.
     * @param password The password.
     */
    public MCIdentityPostgreSQL(String host, int port, String database, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, database));
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(10);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("MCIdentityPostgreSQLPool");

        // PostgreSQL specific performance tweaks
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);
    }

    /**
     * Gets a connection from the Hikari connection pool.
     * @return The database connection.
     * @throws SQLException If a connection error occurs.
     */
    protected Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Closes the Hikari data source.
     */
    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public CompletableFuture<Void> initializeSchema() {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                InitializeSchema.invoke(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> createProfession(String systemCode, String displayName) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                CreateProfession.invoke(conn, systemCode, displayName);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> createAbility(String systemCode, String displayName, AbilityType type, boolean isGlobal) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                CreateAbility.invoke(conn, systemCode, displayName, type, isGlobal);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Collection<Profession>> getProfessions() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                return GetProfessions.invoke(conn);
            } catch (SQLException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        });
    }

    @Override
    public CompletableFuture<Collection<Ability>> getAbilities() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                return GetAbilities.invoke(conn);
            } catch (SQLException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        });
    }

    @Override
    public CompletableFuture<Collection<Rune>> getRunes() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                return GetRunes.invoke(conn);
            } catch (SQLException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        });
    }

    @Override
    public CompletableFuture<Void> createIdentity(IdentityKey key, IdentityProperties properties, IdentityStats stats) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                CreateIdentity.invoke(conn, key, properties, stats);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Optional<IdentityKey>> getIdentityKey(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                return GetIdentityKey.invoke(conn, uuid);
            } catch (SQLException e) {
                e.printStackTrace();
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<Optional<IdentityProperties>> getProperties(UUID identityUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                return GetProperties.invoke(conn, identityUuid);
            } catch (SQLException e) {
                e.printStackTrace();
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<Optional<IdentityStats>> getStats(UUID identityUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                return GetStats.invoke(conn, identityUuid);
            } catch (SQLException e) {
                e.printStackTrace();
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<Integer> getStat(UUID identityUuid, String statKey) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                return GetStat.invoke(conn, identityUuid, statKey);
            } catch (SQLException e) {
                e.printStackTrace();
                return 0;
            }
        });
    }

    @Override
    public CompletableFuture<Map<Integer, String>> getEquippedAbilities(UUID identityUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                return GetEquippedAbilities.invoke(conn, identityUuid);
            } catch (SQLException e) {
                e.printStackTrace();
                return Collections.emptyMap();
            }
        });
    }

    @Override
    public CompletableFuture<Void> setEquippedAbility(UUID identityUuid, int slot, String abilityCode) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                SetEquippedAbility.invoke(conn, identityUuid, slot, abilityCode);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Map<Integer, String>> getRunes(UUID identityUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                return GetRunes.invoke(conn, identityUuid);
            } catch (SQLException e) {
                e.printStackTrace();
                return Collections.emptyMap();
            }
        });
    }

    @Override
    public CompletableFuture<Void> setRune(UUID identityUuid, int slot, String runeCode) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                SetRune.invoke(conn, identityUuid, slot, runeCode);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Optional<UUID>> getActiveIdentity(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                return GetActiveIdentity.invoke(conn, playerUuid);
            } catch (SQLException e) {
                e.printStackTrace();
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<Optional<IdentitySession>> getSession(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                return GetSession.invoke(conn, playerUuid);
            } catch (SQLException e) {
                e.printStackTrace();
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<Void> setActiveIdentity(UUID playerUuid, UUID identityUuid, String contentKey) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                SetActiveIdentity.invoke(conn, playerUuid, identityUuid, contentKey);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> logAction(UUID playerUuid, UUID identityUuid, ActionType actionType) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                LogAction.invoke(conn, playerUuid, identityUuid, actionType);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateProperties(UUID identityUuid, IdentityProperties properties) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                UpdateProperties.invoke(conn, identityUuid, properties);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateStats(UUID identityUuid, IdentityStats stats) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                UpdateStats.invoke(conn, identityUuid, stats);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> addStat(UUID identityUuid, String statKey, int amount) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                AddStat.invoke(conn, identityUuid, statKey, amount);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Map<String, String>> getCosmetics(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                return GetCosmetics.invoke(conn, uuid);
            } catch (SQLException e) {
                e.printStackTrace();
                return Collections.emptyMap();
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveSkin(UUID uuid, String skin) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                SaveSkin.invoke(conn, uuid, skin);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveAura(UUID uuid, String aura) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                SaveAura.invoke(conn, uuid, aura);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}

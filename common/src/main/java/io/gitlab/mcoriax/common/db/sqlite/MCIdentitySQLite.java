package io.gitlab.mcoriax.common.db.sqlite;

import io.gitlab.mcoriax.api.db.IMCIdentityDB;
import io.gitlab.mcoriax.api.model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * SQLite implementation of IMCIdentityDB.
 */
public abstract class MCIdentitySQLite implements IMCIdentityDB {

    /**
     * Path to the SQLite database file.
     */
    private final String dbPath;

    /**
     * Initializes a new SQLite database provider.
     * @param dbPath The file path to the database.
     */
    public MCIdentitySQLite(String dbPath) {
        this.dbPath = dbPath;
    }

    /**
     * SQL to select stats for an identity.
     */
    private static final String SELECT_STATS = "SELECT energy, health, strength, defense, wisdom, luck, agility, resilience FROM identity_stats WHERE identity_uuid = ?";
    /**
     * SQL to increment a specific stat.
     */
    private static final String UPDATE_STAT = "UPDATE identity_stats SET %s = %s + ? WHERE identity_uuid = ?";
    /**
     * SQL to select properties for an identity.
     */
    private static final String SELECT_PROPERTIES = "SELECT current_level, exp, stat_points, head_text, model FROM identity_properties WHERE identity_uuid = ?";
    /**
     * SQL to update properties for an identity.
     */
    private static final String UPDATE_PROPERTIES = "UPDATE identity_properties SET current_level = ?, exp = ?, stat_points = ?, head_text = ?, model = ? WHERE identity_uuid = ?";

    /**
     * Gets a connection to the SQLite database.
     * @return The database connection.
     * @throws SQLException If a connection error occurs.
     */
    protected abstract Connection getConnection() throws SQLException;

    /**
     * {@inheritDoc}
     * Retrieves an identity key from the SQLite database.
     * @param uuid The UUID of the identity key.
     * @return A CompletableFuture containing the identity key if found.
     */
    @Override
    public CompletableFuture<Optional<IdentityKey>> getIdentityKey(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT sigil_id, profession_id FROM identity_keys WHERE uuid = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        // Mapping results...
                        return Optional.empty();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return Optional.empty();
        });
    }

    /**
     * {@inheritDoc}
     * Retrieves identity properties from the SQLite database.
     * @param identityUuid The UUID of the identity.
     * @return A CompletableFuture containing the properties if found.
     */
    @Override
    public CompletableFuture<Optional<IdentityProperties>> getProperties(UUID identityUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(SELECT_PROPERTIES)) {
                ps.setString(1, identityUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        // Mapping results...
                        return Optional.empty();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return Optional.empty();
        });
    }

    /**
     * {@inheritDoc}
     * Retrieves identity stats from the SQLite database.
     * @param identityUuid The UUID of the identity.
     * @return A CompletableFuture containing the stats if found.
     */
    @Override
    public CompletableFuture<Optional<IdentityStats>> getStats(UUID identityUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(SELECT_STATS)) {
                ps.setString(1, identityUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        // Mapping results...
                        return Optional.empty();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return Optional.empty();
        });
    }

    /**
     * {@inheritDoc}
     * Retrieves a specific stat value from the SQLite database.
     * @param identityUuid The UUID of the identity.
     * @param statKey The system name of the stat.
     * @return A CompletableFuture containing the stat value.
     */
    @Override
    public CompletableFuture<Integer> getStat(UUID identityUuid, String statKey) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isValidStatKey(statKey)) return 0;
            
            String sql = "SELECT " + statKey + " FROM identity_stats WHERE identity_uuid = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, identityUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(statKey);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return 0;
        });
    }

    /**
     * {@inheritDoc}
     * Retrieves equipped abilities from the SQLite database.
     * @param identityUuid The UUID of the identity.
     * @return A CompletableFuture containing a map of slot to ability ID.
     */
    @Override
    public CompletableFuture<Map<Integer, String>> getEquippedAbilities(UUID identityUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT ability_id, slot_number FROM identity_abilities WHERE identity_uuid = ?";
            Map<Integer, String> abilities = new HashMap<>();
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, identityUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        abilities.put(rs.getInt("slot_number"), String.valueOf(rs.getInt("ability_id")));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return abilities;
        });
    }

    /**
     * {@inheritDoc}
     * Retrieves runes from the SQLite database.
     * @param identityUuid The UUID of the identity.
     * @return A CompletableFuture containing a map of slot to rune ID.
     */
    @Override
    public CompletableFuture<Map<Integer, String>> getRunes(UUID identityUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT rune_id, slot_number FROM identity_runes WHERE identity_uuid = ?";
            Map<Integer, String> runes = new HashMap<>();
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, identityUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        runes.put(rs.getInt("slot_number"), String.valueOf(rs.getInt("rune_id")));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return runes;
        });
    }

    /**
     * {@inheritDoc}
     * Gets the active identity for a player from the SQLite database.
     * @param playerUuid The UUID of the player.
     * @return A CompletableFuture containing the active identity UUID if any.
     */
    @Override
    public CompletableFuture<Optional<UUID>> getActiveIdentity(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT identity_uuid FROM identity_sessions WHERE player_uuid = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(UUID.fromString(rs.getString("identity_uuid")));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return Optional.empty();
        });
    }

    /**
     * {@inheritDoc}
     * Logs an identity action to the SQLite database.
     * @param playerUuid The UUID of the player.
     * @param identityUuid The UUID of the identity.
     * @param actionType The type of action performed.
     * @return A CompletableFuture that completes when logged.
     */
    @Override
    public CompletableFuture<Void> logAction(UUID playerUuid, UUID identityUuid, ActionType actionType) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO identity_logs (player_uuid, identity_uuid, action_type) VALUES (?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, identityUuid.toString());
                ps.setString(3, actionType.name());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * {@inheritDoc}
     * Updates identity properties in the SQLite database.
     * @param identityUuid The UUID of the identity.
     * @param properties The new properties to save.
     * @return A CompletableFuture that completes when updated.
     */
    @Override
    public CompletableFuture<Void> updateProperties(UUID identityUuid, IdentityProperties properties) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(UPDATE_PROPERTIES)) {
                ps.setInt(1, properties.getLevel());
                ps.setInt(2, properties.getExp());
                ps.setInt(3, properties.getStatPoints());
                ps.setString(4, properties.getHeadText());
                ps.setString(5, properties.getModel());
                ps.setString(6, identityUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * {@inheritDoc}
     * Increments a specific stat in the SQLite database.
     * @param identityUuid The UUID of the identity.
     * @param statKey The system name of the stat.
     * @param amount The amount to add.
     * @return A CompletableFuture that completes when updated.
     */
    @Override
    public CompletableFuture<Void> addStat(UUID identityUuid, String statKey, int amount) {
        return CompletableFuture.runAsync(() -> {
            if (!isValidStatKey(statKey)) return;
            
            String sql = String.format("UPDATE identity_stats SET %s = %s + ? WHERE identity_uuid = ?", statKey, statKey);
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, amount);
                ps.setString(2, identityUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Validates a stat key against a whitelist to prevent SQL injection.
     * @param key The stat key to validate.
     * @return True if valid.
     */
    private boolean isValidStatKey(String key) {
        return key != null && (key.equals("energy") || key.equals("health") || key.equals("strength") || 
               key.equals("defense") || key.equals("wisdom") || key.equals("luck") || 
               key.equals("agility") || key.equals("resilience"));
    }
}

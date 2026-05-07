package io.gitlab.mcoriax.common.db.postgresql;

import io.gitlab.mcoriax.api.db.IMCIdentityDB;
import io.gitlab.mcoriax.api.model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * PostgreSQL implementation of IMCIdentityDB.
 */
public abstract class MCIdentityPostgreSQL implements IMCIdentityDB {

    /**
     * Connection host.
     */
    private final String host;
    /**
     * Connection port.
     */
    private final int port;
    /**
     * Database name.
     */
    private final String database;
    /**
     * Authentication username.
     */
    private final String username;
    /**
     * Authentication password.
     */
    private final String password;

    /**
     * Initializes a new PostgreSQL database provider.
     * @param host The host address.
     * @param port The port number.
     * @param database The database name.
     * @param username The username.
     * @param password The password.
     */
    public MCIdentityPostgreSQL(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
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
     * SQL to update stats for an identity.
     */
    private static final String UPDATE_STATS = "UPDATE identity_stats SET energy = ?, health = ?, strength = ?, defense = ?, wisdom = ?, luck = ?, agility = ?, resilience = ? WHERE identity_uuid = ?";

    /**
     * Gets a connection to the PostgreSQL database.
     * @return The database connection.
     * @throws SQLException If a connection error occurs.
     */
    protected abstract Connection getConnection() throws SQLException;

    @Override
    public CompletableFuture<Collection<Sigil>> getSigils() {
        return CompletableFuture.supplyAsync(() -> {
            List<Sigil> sigils = new ArrayList<>();
            String sql = "SELECT system_code, display_name FROM sigils";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("system_code");
                    String name = rs.getString("display_name");
                    sigils.add(new Sigil() {
                        @Override public String getSystemCode() { return code; }
                        @Override public String getDisplayName() { return name; }
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return sigils;
        });
    }

    @Override
    public CompletableFuture<Collection<Profession>> getProfessions() {
        return CompletableFuture.supplyAsync(() -> {
            List<Profession> professions = new ArrayList<>();
            String sql = "SELECT system_code, display_name FROM professions";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("system_code");
                    String name = rs.getString("display_name");
                    professions.add(new Profession() {
                        @Override public String getSystemCode() { return code; }
                        @Override public String getDisplayName() { return name; }
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return professions;
        });
    }

    @Override
    public CompletableFuture<Collection<Ability>> getAbilities() {
        return CompletableFuture.supplyAsync(() -> {
            List<Ability> abilities = new ArrayList<>();
            String sql = "SELECT system_code, display_name, ability_type, is_global FROM abilities";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("system_code");
                    String name = rs.getString("display_name");
                    AbilityType type = AbilityType.valueOf(rs.getString("ability_type"));
                    boolean global = rs.getBoolean("is_global");
                    abilities.add(new Ability() {
                        @Override public String getSystemCode() { return code; }
                        @Override public String getDisplayName() { return name; }
                        @Override public AbilityType getAbilityType() { return type; }
                        @Override public boolean isGlobal() { return global; }
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return abilities;
        });
    }

    @Override
    public CompletableFuture<Collection<Rune>> getRunes() {
        return CompletableFuture.supplyAsync(() -> {
            List<Rune> runes = new ArrayList<>();
            String sql = "SELECT system_code, display_name FROM runes";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("system_code");
                    String name = rs.getString("display_name");
                    runes.add(new Rune() {
                        @Override public String getSystemCode() { return code; }
                        @Override public String getDisplayName() { return name; }
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return runes;
        });
    }

    @Override
    public CompletableFuture<Void> createIdentity(IdentityKey key, IdentityProperties properties, IdentityStats stats) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // 1. Insert Identity Key
                    String sqlKey = "INSERT INTO identity_keys (uuid, sigil_id, profession_id) VALUES (?, (SELECT id FROM sigils WHERE system_code = ?), (SELECT id FROM professions WHERE system_code = ?))";
                    try (PreparedStatement ps = conn.prepareStatement(sqlKey)) {
                        ps.setString(1, key.getUniqueId().toString());
                        ps.setString(2, key.getSigilCode());
                        ps.setString(3, key.getProfessionCode());
                        ps.executeUpdate();
                    }

                    // 2. Insert Properties
                    String sqlProps = "INSERT INTO identity_properties (identity_uuid, current_level, exp, stat_points, head_text, model) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sqlProps)) {
                        ps.setString(1, key.getUniqueId().toString());
                        ps.setInt(2, properties.getLevel());
                        ps.setInt(3, properties.getExp());
                        ps.setInt(4, properties.getStatPoints());
                        ps.setString(5, properties.getHeadText());
                        ps.setString(6, properties.getModel());
                        ps.executeUpdate();
                    }

                    // 3. Insert Stats
                    String sqlStats = "INSERT INTO identity_stats (identity_uuid, energy, health, strength, defense, wisdom, luck, agility, resilience) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sqlStats)) {
                        ps.setString(1, key.getUniqueId().toString());
                        ps.setInt(2, stats.getEnergy());
                        ps.setInt(3, stats.getHealth());
                        ps.setInt(4, stats.getStrength());
                        ps.setInt(5, stats.getDefense());
                        ps.setInt(6, stats.getWisdom());
                        ps.setInt(7, stats.getLuck());
                        ps.setInt(8, stats.getAgility());
                        ps.setInt(9, stats.getResilience());
                        ps.executeUpdate();
                    }

                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * {@inheritDoc}
     * Retrieves an identity key from the PostgreSQL database.
     * @param uuid The UUID of the identity key.
     * @return A CompletableFuture containing the identity key if found.
     */
    @Override
    public CompletableFuture<Optional<IdentityKey>> getIdentityKey(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT s.system_code as sigil, p.system_code as profession FROM identity_keys k " +
                         "JOIN sigils s ON k.sigil_id = s.id " +
                         "JOIN professions p ON k.profession_id = p.id WHERE k.uuid = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String sigil = rs.getString("sigil");
                        String profession = rs.getString("profession");
                        return Optional.of(new IdentityKey() {
                            @Override public UUID getUniqueId() { return uuid; }
                            @Override public String getSigilCode() { return sigil; }
                            @Override public String getProfessionCode() { return profession; }
                        });
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
     * Retrieves identity properties from the PostgreSQL database.
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
                        int level = rs.getInt("current_level");
                        int exp = rs.getInt("exp");
                        int points = rs.getInt("stat_points");
                        String head = rs.getString("head_text");
                        String model = rs.getString("model");
                        return Optional.of(new IdentityProperties() {
                            @Override public int getLevel() { return level; }
                            @Override public int getExp() { return exp; }
                            @Override public int getStatPoints() { return points; }
                            @Override public String getHeadText() { return head; }
                            @Override public String getModel() { return model; }
                        });
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
     * Retrieves identity stats from the PostgreSQL database.
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
                        int energy = rs.getInt("energy");
                        int health = rs.getInt("health");
                        int strength = rs.getInt("strength");
                        int defense = rs.getInt("defense");
                        int wisdom = rs.getInt("wisdom");
                        int luck = rs.getInt("luck");
                        int agility = rs.getInt("agility");
                        int resilience = rs.getInt("resilience");
                        return Optional.of(new IdentityStats() {
                            @Override public int getEnergy() { return energy; }
                            @Override public int getHealth() { return health; }
                            @Override public int getStrength() { return strength; }
                            @Override public int getDefense() { return defense; }
                            @Override public int getWisdom() { return wisdom; }
                            @Override public int getLuck() { return luck; }
                            @Override public int getAgility() { return agility; }
                            @Override public int getResilience() { return resilience; }
                        });
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
     * Retrieves a specific stat value from the PostgreSQL database.
     * @param identityUuid The UUID of the identity.
     * @param statKey The system name of the stat.
     * @return A CompletableFuture containing the stat value.
     */
    @Override
    public CompletableFuture<Integer> getStat(UUID identityUuid, String statKey) {
        return CompletableFuture.supplyAsync(() -> {
            // White-list statKey to prevent SQL injection since it can't be a parameter
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
     * Retrieves equipped abilities from the PostgreSQL database.
     * @param identityUuid The UUID of the identity.
     * @return A CompletableFuture containing a map of slot to ability ID.
     */
    @Override
    public CompletableFuture<Map<Integer, String>> getEquippedAbilities(UUID identityUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT a.system_code, ia.slot_number FROM identity_abilities ia " +
                         "JOIN abilities a ON ia.ability_id = a.id WHERE ia.identity_uuid = ?";
            Map<Integer, String> abilities = new HashMap<>();
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, identityUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        abilities.put(rs.getInt("slot_number"), rs.getString("system_code"));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return abilities;
        });
    }

    @Override
    public CompletableFuture<Void> setEquippedAbility(UUID identityUuid, int slot, String abilityCode) {
        return CompletableFuture.runAsync(() -> {
            if (abilityCode == null) {
                String sql = "DELETE FROM identity_abilities WHERE identity_uuid = ? AND slot_number = ?";
                try (Connection conn = getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, identityUuid.toString());
                    ps.setInt(2, slot);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                String sql = "INSERT INTO identity_abilities (identity_uuid, ability_id, slot_number) " +
                             "VALUES (?, (SELECT id FROM abilities WHERE system_code = ?), ?) " +
                             "ON CONFLICT (identity_uuid, slot_number) DO UPDATE SET ability_id = EXCLUDED.ability_id";
                try (Connection conn = getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, identityUuid.toString());
                    ps.setString(2, abilityCode);
                    ps.setInt(3, slot);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     * Retrieves runes from the PostgreSQL database.
     * @param identityUuid The UUID of the identity.
     * @return A CompletableFuture containing a map of slot to rune ID.
     */
    @Override
    public CompletableFuture<Map<Integer, String>> getRunes(UUID identityUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT r.system_code, ir.slot_number FROM identity_runes ir " +
                         "JOIN runes r ON ir.rune_id = r.id WHERE ir.identity_uuid = ?";
            Map<Integer, String> runes = new HashMap<>();
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, identityUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        runes.put(rs.getInt("slot_number"), rs.getString("system_code"));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return runes;
        });
    }

    @Override
    public CompletableFuture<Void> setRune(UUID identityUuid, int slot, String runeCode) {
        return CompletableFuture.runAsync(() -> {
            if (runeCode == null) {
                String sql = "DELETE FROM identity_runes WHERE identity_uuid = ? AND slot_number = ?";
                try (Connection conn = getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, identityUuid.toString());
                    ps.setInt(2, slot);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                String sql = "INSERT INTO identity_runes (identity_uuid, rune_id, slot_number) " +
                             "VALUES (?, (SELECT id FROM runes WHERE system_code = ?), ?) " +
                             "ON CONFLICT (identity_uuid, slot_number) DO UPDATE SET rune_id = EXCLUDED.rune_id";
                try (Connection conn = getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, identityUuid.toString());
                    ps.setString(2, runeCode);
                    ps.setInt(3, slot);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     * Gets the active identity for a player from the PostgreSQL database.
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

    @Override
    public CompletableFuture<Void> setActiveIdentity(UUID playerUuid, UUID identityUuid, String contentKey) {
        return CompletableFuture.runAsync(() -> {
            if (identityUuid == null) {
                String sql = "DELETE FROM identity_sessions WHERE player_uuid = ?";
                try (Connection conn = getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, playerUuid.toString());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                String sql = "INSERT INTO identity_sessions (player_uuid, identity_uuid, content_key) VALUES (?, ?, ?) " +
                             "ON CONFLICT (player_uuid) DO UPDATE SET identity_uuid = EXCLUDED.identity_uuid, content_key = EXCLUDED.content_key";
                try (Connection conn = getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, playerUuid.toString());
                    ps.setString(2, identityUuid.toString());
                    ps.setString(3, contentKey);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     * Logs an identity action to the PostgreSQL database.
     * @param playerUuid The UUID of the player.
     * @param identityUuid The UUID of the identity.
     * @param actionType The type of action performed.
     * @return A CompletableFuture that completes when logged.
     */
    @Override
    public CompletableFuture<Void> logAction(UUID playerUuid, UUID identityUuid, ActionType actionType) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO identity_logs (player_uuid, identity_uuid, action_type) VALUES (?, ?, ?::action_type_enum)";
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
     * Updates identity properties in the PostgreSQL database.
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

    @Override
    public CompletableFuture<Void> updateStats(UUID identityUuid, IdentityStats stats) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(UPDATE_STATS)) {
                ps.setInt(1, stats.getEnergy());
                ps.setInt(2, stats.getHealth());
                ps.setInt(3, stats.getStrength());
                ps.setInt(4, stats.getDefense());
                ps.setInt(5, stats.getWisdom());
                ps.setInt(6, stats.getLuck());
                ps.setInt(7, stats.getAgility());
                ps.setInt(8, stats.getResilience());
                ps.setString(9, identityUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * {@inheritDoc}
     * Increments a specific stat in the PostgreSQL database.
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

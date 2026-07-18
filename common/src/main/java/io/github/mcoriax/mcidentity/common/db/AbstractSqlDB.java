package io.github.mcoriax.mcidentity.common.db;

import io.github.mcoriax.mcidentity.api.db.IMCIdentityDB;
import io.github.mcoriax.mcidentity.api.model.*;
import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import io.github.mcoriax.mcidentity.common.model.MutableIdentityProperties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for JDBC-backed {@link IMCIdentityDB} implementations.
 *
 * <p>All query logic shared by the SQLite, MySQL, and PostgreSQL backends
 * lives here; concrete subclasses only supply their connection and the few
 * dialect-specific statements (schema DDL, insert-ignore, and upserts).
 * Every operation runs asynchronously on a dedicated daemon executor and
 * opens a short-lived connection through {@link #openConnection()}, so no
 * external connection pool is required and the game thread is never
 * blocked.</p>
 *
 * <p>Failure semantics mirror the central server client: reads resolve to
 * their neutral fallback (empty optional, empty collection, or zero) and
 * writes complete normally after logging, while {@link #createIdentity} and
 * {@link #initializeSchema} complete exceptionally so callers can react.</p>
 */
public abstract class AbstractSqlDB implements IMCIdentityDB {

    /**
     * Logger for connection and statement failures.
     */
    private static final Logger LOGGER = Logger.getLogger(AbstractSqlDB.class.getName());

    /**
     * Dedicated executor running every database operation off the game thread.
     */
    private final ExecutorService executor;

    /**
     * Creates the backend with its own asynchronous executor.
     * @param poolSize The number of worker threads. SQLite must use 1 so file
     *                 writes never contend; client-server databases may use more.
     * @param poolName The base name given to the worker threads.
     */
    protected AbstractSqlDB(int poolSize, String poolName) {
        AtomicInteger counter = new AtomicInteger(1);
        this.executor = Executors.newFixedThreadPool(poolSize, runnable -> {
            Thread thread = new Thread(runnable, poolName + "-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Opens a new connection to the database.
     * @return The open connection; the caller closes it after the operation.
     * @throws SQLException If the connection cannot be established.
     */
    protected abstract Connection openConnection() throws SQLException;

    /**
     * Registers a JDBC driver class, logging a warning when it is missing so
     * misconfigured servers get an actionable message instead of a later
     * "no suitable driver" failure.
     * @param driverClassName The fully qualified driver class name.
     */
    protected static void loadDriver(String driverClassName) {
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            LOGGER.warning("JDBC driver " + driverClassName + " is not on the classpath; database calls will fail until it is provided");
        }
    }

    // --- Dialect hooks ---

    /**
     * Supplies the ordered DDL statements creating the schema.
     * @return The dialect-specific schema statements.
     */
    protected abstract String[] schemaStatements();

    /**
     * Decides whether a schema statement failure may be ignored, e.g. a
     * "type already exists" error on re-run.
     * @param e The schema failure.
     * @return true when initialization should continue past the failure.
     */
    protected boolean isIgnorableSchemaError(SQLException e) {
        return false;
    }

    /**
     * Supplies the insert-if-absent statement for professions with
     * (system_code, display_name) parameters.
     * @return The dialect-specific SQL.
     */
    protected abstract String insertProfessionIgnoreSql();

    /**
     * Supplies the insert-if-absent statement for abilities with
     * (system_code, display_name, ability_skin) parameters.
     * @return The dialect-specific SQL.
     */
    protected abstract String insertAbilityIgnoreSql();

    /**
     * Supplies the audit log insert with (player_uuid, identity_uuid,
     * action_type) parameters.
     * @return The dialect-specific SQL.
     */
    protected String insertLogSql() {
        return "INSERT INTO identity_logs (player_uuid, identity_uuid, action_type) VALUES (?, ?, ?)";
    }

    /**
     * Supplies the session upsert with (player_uuid, identity_uuid,
     * content_key) parameters.
     * @return The dialect-specific SQL.
     */
    protected abstract String upsertSessionSql();

    /**
     * Supplies the equipped-ability upsert with (identity_uuid, ability
     * system_code, slot_number, ability_skin) parameters.
     * @return The dialect-specific SQL.
     */
    protected abstract String upsertEquippedAbilitySql();

    /**
     * Supplies the equipped-rune upsert with (identity_uuid, rune
     * system_code, slot_number) parameters.
     * @return The dialect-specific SQL.
     */
    protected abstract String upsertEquippedRuneSql();

    /**
     * Supplies the cosmetics upsert for one column with (identity_uuid,
     * value) parameters.
     * @param column The cosmetics column, either "skin" or "aura".
     * @return The dialect-specific SQL.
     */
    protected abstract String upsertCosmeticSql(String column);

    // --- IMCIdentityDB ---

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> initializeSchema() {
        return runAsyncOrThrow(conn -> {
            for (String sql : schemaStatements()) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.executeUpdate();
                } catch (SQLException e) {
                    if (!isIgnorableSchemaError(e)) {
                        throw e;
                    }
                }
            }
        }, "Failed to initialize the database schema");
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> createProfession(String systemCode, String displayName) {
        return runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(insertProfessionIgnoreSql())) {
                ps.setString(1, systemCode);
                ps.setString(2, displayName);
                ps.executeUpdate();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Collection<Profession>> getProfessions() {
        return supplyAsync(Collections.emptyList(), conn -> {
            List<Profession> professions = new ArrayList<>();
            String sql = "SELECT system_code, display_name FROM identity_professions";
            try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    professions.add(toProfession(rs.getString("system_code"), rs.getString("display_name")));
                }
            }
            return professions;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> createAbility(String abilityCode, String displayName, String abilitySkin) {
        return runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(insertAbilityIgnoreSql())) {
                ps.setString(1, abilityCode);
                ps.setString(2, displayName);
                ps.setString(3, abilitySkin);
                ps.executeUpdate();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Collection<Ability>> getAbilities() {
        return supplyAsync(Collections.emptyList(), conn -> {
            List<Ability> abilities = new ArrayList<>();
            String sql = "SELECT system_code, display_name, ability_skin FROM identity_abilities";
            try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("system_code");
                    String name = rs.getString("display_name");
                    String skin = rs.getString("ability_skin");
                    abilities.add(new Ability() {
                        @Override public String getAbilityCode() { return code; }
                        @Override public String getDisplayName() { return name; }
                        @Override public String getAbilitySkin() { return skin; }
                    });
                }
            }
            return abilities;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Collection<Rune>> getRunes() {
        return supplyAsync(Collections.emptyList(), conn -> {
            List<Rune> runes = new ArrayList<>();
            String sql = "SELECT system_code, display_name FROM identity_runes";
            try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("system_code");
                    String name = rs.getString("display_name");
                    runes.add(new Rune() {
                        @Override public String getSystemCode() { return code; }
                        @Override public String getDisplayName() { return name; }
                    });
                }
            }
            return runes;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> createIdentity(IdentityKey key, IdentityProperties properties, IdentityStats stats) {
        String professionCode = key.getProfessionCode();
        if (professionCode == null || professionCode.trim().isEmpty() || professionCode.equalsIgnoreCase("random")) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid profession code: " + professionCode));
        }
        return runAsyncOrThrow(conn -> {
            try {
                conn.setAutoCommit(false);
                try (PreparedStatement ps = conn.prepareStatement(insertProfessionIgnoreSql())) {
                    ps.setString(1, professionCode);
                    ps.setString(2, professionCode);
                    ps.executeUpdate();
                }
                String sqlKey = "INSERT INTO identity_keys (uuid, profession_id) VALUES (?, (SELECT id FROM identity_professions WHERE system_code = ?))";
                try (PreparedStatement ps = conn.prepareStatement(sqlKey)) {
                    ps.setString(1, key.getUniqueId().toString());
                    ps.setString(2, professionCode);
                    ps.executeUpdate();
                }
                String sqlProps = "INSERT INTO identity_properties (identity_uuid, current_level, exp, stat_points, head_text) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlProps)) {
                    ps.setString(1, key.getUniqueId().toString());
                    ps.setInt(2, properties.getLevel());
                    ps.setInt(3, properties.getExp());
                    ps.setInt(4, properties.getStatPoints());
                    ps.setString(5, properties.getHeadText());
                    ps.executeUpdate();
                }
                String sqlStats = "INSERT INTO identity_stats (identity_uuid, energy, health, food, strength, defense, wisdom, luck, agility, resilience) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlStats)) {
                    ps.setString(1, key.getUniqueId().toString());
                    ps.setInt(2, stats.getEnergy());
                    ps.setInt(3, stats.getHealth());
                    ps.setInt(4, stats.getFood());
                    ps.setInt(5, stats.getStrength());
                    ps.setInt(6, stats.getDefense());
                    ps.setInt(7, stats.getWisdom());
                    ps.setInt(8, stats.getLuck());
                    ps.setInt(9, stats.getAgility());
                    ps.setInt(10, stats.getResilience());
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        }, "Failed to create identity");
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<IdentityKey>> getIdentityKey(UUID uuid) {
        return supplyAsync(Optional.empty(), conn -> {
            String sql = "SELECT p.system_code AS profession FROM identity_keys k " +
                         "JOIN identity_professions p ON k.profession_id = p.id WHERE k.uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String profession = rs.getString("profession");
                        return Optional.of(new IdentityKey() {
                            @Override public UUID getUniqueId() { return uuid; }
                            @Override public String getProfessionCode() { return profession; }
                        });
                    }
                }
            }
            return Optional.empty();
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<IdentityProperties>> getProperties(UUID identityUuid) {
        return supplyAsync(Optional.empty(), conn -> {
            String sql = "SELECT current_level, exp, stat_points, head_text, inventory_contents, enderchest_contents FROM identity_properties WHERE identity_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, identityUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(new MutableIdentityProperties(
                            rs.getInt("current_level"),
                            rs.getInt("exp"),
                            rs.getInt("stat_points"),
                            rs.getString("head_text"),
                            null,
                            null,
                            rs.getString("inventory_contents"),
                            rs.getString("enderchest_contents")
                        ));
                    }
                }
            }
            return Optional.empty();
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<IdentityStats>> getStats(UUID identityUuid) {
        return supplyAsync(Optional.empty(), conn -> {
            String sql = "SELECT energy, health, food, strength, defense, wisdom, luck, agility, resilience FROM identity_stats WHERE identity_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, identityUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(toStats(rs));
                    }
                }
            }
            return Optional.empty();
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Integer> getStat(UUID identityUuid, String statKey) {
        if (!StatKeys.isValid(statKey)) {
            return CompletableFuture.completedFuture(0);
        }
        return supplyAsync(0, conn -> {
            String sql = "SELECT " + statKey + " FROM identity_stats WHERE identity_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, identityUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(statKey);
                    }
                }
            }
            return 0;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Map<Integer, EquippedAbility>> getEquippedAbilities(UUID identityUuid) {
        return supplyAsync(Collections.emptyMap(), conn -> {
            String sql = "SELECT a.system_code, ia.slot_number, ia.ability_skin FROM identity_equipped_abilities ia " +
                         "JOIN identity_abilities a ON ia.ability_id = a.id WHERE ia.identity_uuid = ?";
            Map<Integer, EquippedAbility> slots = new HashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, identityUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        slots.put(rs.getInt("slot_number"),
                            new EquippedAbility(rs.getString("system_code"), rs.getString("ability_skin")));
                    }
                }
            }
            return slots;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> setEquippedAbility(UUID identityUuid, int slot, String abilityCode, String abilitySkin) {
        return runAsync(conn -> {
            if (abilityCode == null) {
                String sql = "DELETE FROM identity_equipped_abilities WHERE identity_uuid = ? AND slot_number = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, identityUuid.toString());
                    ps.setInt(2, slot);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(upsertEquippedAbilitySql())) {
                    ps.setString(1, identityUuid.toString());
                    ps.setString(2, abilityCode);
                    ps.setInt(3, slot);
                    ps.setString(4, abilitySkin);
                    ps.executeUpdate();
                }
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Map<Integer, String>> getRunes(UUID identityUuid) {
        return supplyAsync(Collections.emptyMap(), conn -> {
            String sql = "SELECT r.system_code, ir.slot_number FROM identity_equipped_runes ir " +
                         "JOIN identity_runes r ON ir.rune_id = r.id WHERE ir.identity_uuid = ?";
            Map<Integer, String> slots = new HashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, identityUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        slots.put(rs.getInt("slot_number"), rs.getString("system_code"));
                    }
                }
            }
            return slots;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> setRune(UUID identityUuid, int slot, String runeCode) {
        return runAsync(conn -> {
            if (runeCode == null) {
                String sql = "DELETE FROM identity_equipped_runes WHERE identity_uuid = ? AND slot_number = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, identityUuid.toString());
                    ps.setInt(2, slot);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(upsertEquippedRuneSql())) {
                    ps.setString(1, identityUuid.toString());
                    ps.setString(2, runeCode);
                    ps.setInt(3, slot);
                    ps.executeUpdate();
                }
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<UUID>> getActiveIdentity(UUID playerUuid) {
        return supplyAsync(Optional.empty(), conn -> {
            String sql = "SELECT identity_uuid FROM identity_sessions WHERE player_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String id = rs.getString("identity_uuid");
                        return id != null ? Optional.of(UUID.fromString(id)) : Optional.empty();
                    }
                }
            }
            return Optional.empty();
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Optional<IdentitySession>> getSession(UUID playerUuid) {
        return supplyAsync(Optional.empty(), conn -> {
            String sql = "SELECT identity_uuid, content_key FROM identity_sessions WHERE player_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String idStr = rs.getString("identity_uuid");
                        if (idStr == null) {
                            return Optional.empty();
                        }
                        UUID identityUuid = UUID.fromString(idStr);
                        String contentKey = rs.getString("content_key");
                        return Optional.of(new IdentitySession() {
                            @Override public UUID getPlayerUniqueId() { return playerUuid; }
                            @Override public UUID getIdentityUniqueId() { return identityUuid; }
                            @Override public String getContentKey() { return contentKey; }
                        });
                    }
                }
            }
            return Optional.empty();
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> setActiveIdentity(UUID playerUuid, UUID identityUuid, String contentKey) {
        return runAsync(conn -> {
            if (identityUuid == null) {
                String sql = "DELETE FROM identity_sessions WHERE player_uuid = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, playerUuid.toString());
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(upsertSessionSql())) {
                    ps.setString(1, playerUuid.toString());
                    ps.setString(2, identityUuid.toString());
                    ps.setString(3, contentKey);
                    ps.executeUpdate();
                }
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> logAction(UUID playerUuid, UUID identityUuid, ActionType actionType) {
        return runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(insertLogSql())) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, identityUuid.toString());
                ps.setString(3, actionType.name());
                ps.executeUpdate();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateProperties(UUID identityUuid, IdentityProperties properties) {
        return runAsync(conn -> {
            String sql = "UPDATE identity_properties SET current_level = ?, exp = ?, stat_points = ?, head_text = ?, inventory_contents = ?, enderchest_contents = ? WHERE identity_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, properties.getLevel());
                ps.setInt(2, properties.getExp());
                ps.setInt(3, properties.getStatPoints());
                ps.setString(4, properties.getHeadText());
                ps.setString(5, properties.getInventoryContents());
                ps.setString(6, properties.getEnderchestContents());
                ps.setString(7, identityUuid.toString());
                ps.executeUpdate();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateStats(UUID identityUuid, IdentityStats stats) {
        return runAsync(conn -> {
            String sql = "UPDATE identity_stats SET energy = ?, health = ?, food = ?, strength = ?, defense = ?, wisdom = ?, luck = ?, agility = ?, resilience = ? WHERE identity_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, stats.getEnergy());
                ps.setInt(2, stats.getHealth());
                ps.setInt(3, stats.getFood());
                ps.setInt(4, stats.getStrength());
                ps.setInt(5, stats.getDefense());
                ps.setInt(6, stats.getWisdom());
                ps.setInt(7, stats.getLuck());
                ps.setInt(8, stats.getAgility());
                ps.setInt(9, stats.getResilience());
                ps.setString(10, identityUuid.toString());
                ps.executeUpdate();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> addStat(UUID identityUuid, String statKey, int amount) {
        if (!StatKeys.isValid(statKey)) {
            return CompletableFuture.completedFuture(null);
        }
        return runAsync(conn -> {
            String sql = "UPDATE identity_stats SET " + statKey + " = " + statKey + " + ? WHERE identity_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, amount);
                ps.setString(2, identityUuid.toString());
                ps.executeUpdate();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Map<String, String>> getCosmetics(UUID uuid) {
        return supplyAsync(Collections.emptyMap(), conn -> {
            Map<String, String> cosmetics = new HashMap<>();
            String sql = "SELECT skin, aura FROM identity_equipped_cosmetics WHERE identity_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        cosmetics.put("skin", rs.getString("skin"));
                        cosmetics.put("aura", rs.getString("aura"));
                    }
                }
            }
            return cosmetics;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> saveSkin(UUID uuid, String skin) {
        return saveCosmetic(uuid, "skin", skin);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> saveAura(UUID uuid, String aura) {
        return saveCosmetic(uuid, "aura", aura);
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> updateSessionContentKey(UUID playerUuid, String contentKey) {
        return runAsync(conn -> {
            String sql = "UPDATE identity_sessions SET content_key = ? WHERE player_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, contentKey);
                ps.setString(2, playerUuid.toString());
                ps.executeUpdate();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Integer> getBaseIdentitySlots(UUID playerUuid) {
        return supplyAsync(0, conn -> {
            String sql = "SELECT slots FROM player_base_slots WHERE player_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("slots");
                    }
                }
            }
            return 0;
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Integer> addBaseIdentitySlots(UUID playerUuid, int amount) {
        // Read-modify-write inside one transaction keeps the statement
        // dialect-free, so all three SQL backends share this implementation.
        return supplyAsyncOrThrow("Failed to update base identity slots", conn -> {
            try {
                conn.setAutoCommit(false);
                Integer current = null;
                String select = "SELECT slots FROM player_base_slots WHERE player_uuid = ?";
                try (PreparedStatement ps = conn.prepareStatement(select)) {
                    ps.setString(1, playerUuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            current = rs.getInt("slots");
                        }
                    }
                }
                int next = Math.max(0, (current != null ? current : 0) + amount);
                String write = current != null
                    ? "UPDATE player_base_slots SET slots = ? WHERE player_uuid = ?"
                    : "INSERT INTO player_base_slots (slots, player_uuid) VALUES (?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(write)) {
                    ps.setInt(1, next);
                    ps.setString(2, playerUuid.toString());
                    ps.executeUpdate();
                }
                conn.commit();
                return next;
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown() {
        executor.shutdown();
    }

    // --- Internal helpers ---

    /**
     * Upserts one cosmetics column for an identity.
     * @param uuid The identity UUID.
     * @param column The allow-listed column name, "skin" or "aura".
     * @param value The new value, or null to clear.
     * @return A future that completes when saved.
     */
    private CompletableFuture<Void> saveCosmetic(UUID uuid, String column, String value) {
        return runAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(upsertCosmeticSql(column))) {
                ps.setString(1, uuid.toString());
                ps.setString(2, value);
                ps.executeUpdate();
            }
        });
    }

    /**
     * Runs a connection-scoped write asynchronously, logging any SQL failure
     * without failing the returned future.
     * @param action The action to run with an open connection.
     * @return A future that completes once the write finishes.
     */
    protected CompletableFuture<Void> runAsync(SqlConsumer action) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = openConnection()) {
                action.accept(conn);
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Database write failed", e);
            }
        }, executor);
    }

    /**
     * Runs a connection-scoped write asynchronously, wrapping any SQL failure
     * in an unchecked exception so the returned future completes exceptionally.
     * @param action The action to run with an open connection.
     * @param failureMessage The message for the wrapping exception.
     * @return A future that completes once the write finishes.
     */
    protected CompletableFuture<Void> runAsyncOrThrow(SqlConsumer action, String failureMessage) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = openConnection()) {
                action.accept(conn);
            } catch (SQLException e) {
                throw new RuntimeException(failureMessage, e);
            }
        }, executor);
    }

    /**
     * Supplies a connection-scoped read asynchronously, returning a fallback
     * on failure.
     * @param fallback The value to return if the read fails.
     * @param action The action that produces the result from an open connection.
     * @param <T> The result type.
     * @return A future that completes with the result, or the fallback on failure.
     */
    @SuppressWarnings("unchecked")
    protected <T> CompletableFuture<T> supplyAsync(Object fallback, SqlFunction<T> action) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = openConnection()) {
                return action.apply(conn);
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Database read failed", e);
                return (T) fallback;
            }
        }, executor);
    }

    /**
     * Supplies a connection-scoped result asynchronously, wrapping any SQL
     * failure in an unchecked exception so the returned future completes
     * exceptionally and the caller can react.
     * @param failureMessage The message for the wrapping exception.
     * @param action The action that produces the result from an open connection.
     * @param <T> The result type.
     * @return A future that completes with the result, or exceptionally on failure.
     */
    protected <T> CompletableFuture<T> supplyAsyncOrThrow(String failureMessage, SqlFunction<T> action) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = openConnection()) {
                return action.apply(conn);
            } catch (SQLException e) {
                throw new RuntimeException(failureMessage, e);
            }
        }, executor);
    }

    /**
     * Builds a profession view whose configuration accessors read the values
     * loaded into {@link MCIdentityManager} from the local profession files.
     * @param code The profession system code.
     * @param name The profession display name.
     * @return The profession view.
     */
    private static Profession toProfession(String code, String name) {
        return new Profession() {
            @Override public String getSystemCode() { return code; }
            @Override public String getDisplayName() { return name; }
            @Override public Map<String, Object> getExpConfig() { return MCIdentityManager.getProfessionExpConfig(code); }
            @Override public Map<String, Integer> getBaseStats() { return MCIdentityManager.getProfessionBaseStats(code); }
            @Override public String getStatsRandom() { return MCIdentityManager.getProfessionStatsRandom(code); }
            @Override public Map<Integer, String> getSkins() { return MCIdentityManager.getProfessionSkins(code); }
            @Override public void setSkins(Map<Integer, String> skins) { MCIdentityManager.setProfessionSkins(code, skins); }
        };
    }

    /**
     * Deserializes a stats row into the immutable stats view.
     * @param rs The result set positioned on the stats row.
     * @return The stats view.
     * @throws SQLException If a column cannot be read.
     */
    private static IdentityStats toStats(ResultSet rs) throws SQLException {
        int energy = rs.getInt("energy");
        int health = rs.getInt("health");
        int food = rs.getInt("food");
        int strength = rs.getInt("strength");
        int defense = rs.getInt("defense");
        int wisdom = rs.getInt("wisdom");
        int luck = rs.getInt("luck");
        int agility = rs.getInt("agility");
        int resilience = rs.getInt("resilience");
        return new IdentityStats() {
            @Override public int getEnergy() { return energy; }
            @Override public int getHealth() { return health; }
            @Override public int getFood() { return food; }
            @Override public int getStrength() { return strength; }
            @Override public int getDefense() { return defense; }
            @Override public int getWisdom() { return wisdom; }
            @Override public int getLuck() { return luck; }
            @Override public int getAgility() { return agility; }
            @Override public int getResilience() { return resilience; }
        };
    }

    /**
     * A connection-scoped action that may throw {@link SQLException}.
     */
    @FunctionalInterface
    protected interface SqlConsumer {
        void accept(Connection conn) throws SQLException;
    }

    /**
     * A connection-scoped function that may throw {@link SQLException}.
     * @param <T> The result type.
     */
    @FunctionalInterface
    protected interface SqlFunction<T> {
        T apply(Connection conn) throws SQLException;
    }

    /**
     * Opens a connection through {@link DriverManager}.
     * @param jdbcUrl The JDBC URL.
     * @return The open connection.
     * @throws SQLException If the connection cannot be established.
     */
    protected static Connection connect(String jdbcUrl) throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    /**
     * Opens an authenticated connection through {@link DriverManager}.
     * @param jdbcUrl The JDBC URL.
     * @param username The database user.
     * @param password The database password.
     * @return The open connection.
     * @throws SQLException If the connection cannot be established.
     */
    protected static Connection connect(String jdbcUrl, String username, String password) throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }
}

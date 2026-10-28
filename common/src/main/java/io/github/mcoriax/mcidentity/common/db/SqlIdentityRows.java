package io.github.mcoriax.mcidentity.common.db;

import io.github.mcoriax.mcidentity.api.model.IdentityKey;
import io.github.mcoriax.mcidentity.api.model.IdentityProperties;
import io.github.mcoriax.mcidentity.api.model.IdentityStats;
import io.github.mcoriax.mcidentity.common.model.IdentityStatsRecord;
import io.github.mcoriax.mcidentity.common.model.MutableIdentityProperties;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Writes an identity's rows and reads its property and stat rows back.
 *
 * <p><strong>Internal.</strong> Package-private, used by {@link AbstractSqlDB}.
 * The statements are dialect-free; the one dialect-specific statement, the
 * insert-if-absent of the profession, is handed in.</p>
 */
final class SqlIdentityRows {

    /**
     * Columns of {@code identity_stats}, in the order the nine stats are bound.
     */
    static final String STAT_COLUMNS = "energy, health, food, strength, defense, wisdom, luck, agility, resilience";

    private SqlIdentityRows() {
    }

    /**
     * Inserts the profession if absent, then the identity's key, property and
     * stat rows. Run inside a transaction so a failure leaves nothing behind.
     * @param conn The open connection.
     * @param insertProfessionIgnoreSql The dialect's profession insert-if-absent.
     * @param key The identity key.
     * @param properties The starting properties.
     * @param stats The starting stats.
     * @throws SQLException When any insert fails.
     */
    static void insert(Connection conn, String insertProfessionIgnoreSql, IdentityKey key,
                       IdentityProperties properties, IdentityStats stats) throws SQLException {
        String professionCode = key.getProfessionCode();
        String identityUuid = key.getUniqueId().toString();
        try (PreparedStatement ps = conn.prepareStatement(insertProfessionIgnoreSql)) {
            ps.setString(1, professionCode);
            ps.setString(2, professionCode);
            ps.executeUpdate();
        }
        String sqlKey = "INSERT INTO identity_keys (uuid, profession_id) VALUES (?, (SELECT id FROM identity_professions WHERE system_code = ?))";
        try (PreparedStatement ps = conn.prepareStatement(sqlKey)) {
            ps.setString(1, identityUuid);
            ps.setString(2, professionCode);
            ps.executeUpdate();
        }
        String sqlProps = "INSERT INTO identity_properties (identity_uuid, current_level, exp, stat_points, head_text) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sqlProps)) {
            ps.setString(1, identityUuid);
            ps.setInt(2, properties.getLevel());
            ps.setInt(3, properties.getExp());
            ps.setInt(4, properties.getStatPoints());
            ps.setString(5, properties.getHeadText());
            ps.executeUpdate();
        }
        String sqlStats = "INSERT INTO identity_stats (identity_uuid, " + STAT_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sqlStats)) {
            ps.setString(1, identityUuid);
            bindStats(ps, 2, stats);
            ps.executeUpdate();
        }
    }

    /**
     * Binds the nine stats, in {@link #STAT_COLUMNS} order, from a parameter index on.
     * @param ps The statement.
     * @param first The index of the first stat parameter.
     * @param stats The stats to bind.
     * @throws SQLException When a parameter cannot be bound.
     */
    static void bindStats(PreparedStatement ps, int first, IdentityStats stats) throws SQLException {
        ps.setInt(first, stats.getEnergy());
        ps.setInt(first + 1, stats.getHealth());
        ps.setInt(first + 2, stats.getFood());
        ps.setInt(first + 3, stats.getStrength());
        ps.setInt(first + 4, stats.getDefense());
        ps.setInt(first + 5, stats.getWisdom());
        ps.setInt(first + 6, stats.getLuck());
        ps.setInt(first + 7, stats.getAgility());
        ps.setInt(first + 8, stats.getResilience());
    }

    /**
     * Reads a properties row.
     * @param rs The result set positioned on the row.
     * @return The properties.
     * @throws SQLException When a column cannot be read.
     */
    static MutableIdentityProperties readProperties(ResultSet rs) throws SQLException {
        return new MutableIdentityProperties(
            rs.getString("alias"),
            rs.getInt("current_level"),
            rs.getInt("exp"),
            rs.getInt("stat_points"),
            rs.getString("head_text"),
            rs.getString("inventory_contents"),
            rs.getString("enderchest_contents"));
    }

    /**
     * Reads a stats row.
     * @param rs The result set positioned on the row.
     * @return The stats.
     * @throws SQLException When a column cannot be read.
     */
    static IdentityStats readStats(ResultSet rs) throws SQLException {
        return new IdentityStatsRecord(
            rs.getInt("energy"), rs.getInt("health"), rs.getInt("food"),
            rs.getInt("strength"), rs.getInt("defense"), rs.getInt("wisdom"),
            rs.getInt("luck"), rs.getInt("agility"), rs.getInt("resilience"));
    }
}

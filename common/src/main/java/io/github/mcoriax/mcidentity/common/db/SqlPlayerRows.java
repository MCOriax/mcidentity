package io.github.mcoriax.mcidentity.common.db;

import io.github.mcoriax.mcidentity.api.model.PlayerStats;
import io.github.mcoriax.mcidentity.api.model.ProfessionProgress;
import io.github.mcoriax.mcidentity.common.model.PlayerStatsRecord;
import io.github.mcoriax.mcidentity.common.model.ProfessionProgressRecord;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Binds and reads the player keyed rows shared by the three SQL dialects.
 */
final class SqlPlayerRows {

    /**
     * The stat columns of {@code player_stats}, in binding order.
     */
    static final String STAT_COLUMNS = "stat_points, energy, health, food, strength, defense, wisdom, luck, agility, resilience";

    private SqlPlayerRows() {
    }

    /**
     * Binds the ten stat values in {@link #STAT_COLUMNS} order.
     * @param ps The statement.
     * @param first The index of the first parameter.
     * @param stats The stats to bind.
     * @throws SQLException When binding fails.
     */
    static void bindStats(PreparedStatement ps, int first, PlayerStats stats) throws SQLException {
        ps.setInt(first, stats.getStatPoints());
        ps.setInt(first + 1, stats.getEnergy());
        ps.setInt(first + 2, stats.getHealth());
        ps.setInt(first + 3, stats.getFood());
        ps.setInt(first + 4, stats.getStrength());
        ps.setInt(first + 5, stats.getDefense());
        ps.setInt(first + 6, stats.getWisdom());
        ps.setInt(first + 7, stats.getLuck());
        ps.setInt(first + 8, stats.getAgility());
        ps.setInt(first + 9, stats.getResilience());
    }

    /**
     * Reads the stats of the current row.
     * @param rs A result set positioned on a {@code player_stats} row.
     * @return The stats.
     * @throws SQLException When reading fails.
     */
    static PlayerStats readStats(ResultSet rs) throws SQLException {
        return new PlayerStatsRecord(
            rs.getInt("stat_points"),
            rs.getInt("energy"), rs.getInt("health"), rs.getInt("food"),
            rs.getInt("strength"), rs.getInt("defense"), rs.getInt("wisdom"),
            rs.getInt("luck"), rs.getInt("agility"), rs.getInt("resilience"));
    }

    /**
     * Reads the progress of the current row.
     * @param rs A result set positioned on a {@code player_profession_progress} row.
     * @return The progress.
     * @throws SQLException When reading fails.
     */
    static ProfessionProgress readProgress(ResultSet rs) throws SQLException {
        return new ProfessionProgressRecord(rs.getInt("current_level"), rs.getInt("exp"));
    }
}

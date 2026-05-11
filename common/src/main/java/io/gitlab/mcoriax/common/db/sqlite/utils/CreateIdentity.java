package io.gitlab.mcoriax.common.db.sqlite.utils;

import io.gitlab.mcoriax.api.model.IdentityKey;
import io.gitlab.mcoriax.api.model.IdentityProperties;
import io.gitlab.mcoriax.api.model.IdentityStats;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CreateIdentity {
    public static void invoke(Connection conn, IdentityKey key, IdentityProperties properties, IdentityStats stats) throws SQLException {
        String professionCode = key.getProfessionCode();
        if (professionCode == null || professionCode.trim().isEmpty() || professionCode.equalsIgnoreCase("random")) {
            throw new IllegalArgumentException("Invalid profession code: " + professionCode);
        }

        try {
            conn.setAutoCommit(false);
            // 0. Ensure Profession exist
            String sqlEnsureProf = "INSERT OR IGNORE INTO professions (system_code, display_name) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlEnsureProf)) {
                ps.setString(1, key.getProfessionCode());
                ps.setString(2, key.getProfessionCode()); // Default name is code
                ps.executeUpdate();
            }

            // 1. Insert Identity Key
            String sqlKey = "INSERT INTO identity_keys (uuid, profession_id) VALUES (?, (SELECT id FROM professions WHERE system_code = ?))";
            try (PreparedStatement ps = conn.prepareStatement(sqlKey)) {
                ps.setString(1, key.getUniqueId().toString());
                ps.setString(2, key.getProfessionCode());
                ps.executeUpdate();
            }

            // 2. Insert Properties
            String sqlProps = "INSERT INTO identity_properties (identity_uuid, current_level, exp, stat_points, head_text) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlProps)) {
                ps.setString(1, key.getUniqueId().toString());
                ps.setInt(2, properties.getLevel());
                ps.setInt(3, properties.getExp());
                ps.setInt(4, properties.getStatPoints());
                ps.setString(5, properties.getHeadText());
                ps.executeUpdate();
            }

            // 3. Insert Stats
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
            } catch (SQLException ignored) {}
        }
    }
}

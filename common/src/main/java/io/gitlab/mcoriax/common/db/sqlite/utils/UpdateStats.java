package io.gitlab.mcoriax.common.db.sqlite.utils;

import io.gitlab.mcoriax.api.model.IdentityStats;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class UpdateStats {
    private static final String UPDATE_STATS = "UPDATE identity_stats SET energy = ?, health = ?, food = ?, strength = ?, defense = ?, wisdom = ?, luck = ?, agility = ?, resilience = ? WHERE identity_uuid = ?";

    public static void invoke(Connection conn, UUID identityUuid, IdentityStats stats) {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_STATS)) {
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

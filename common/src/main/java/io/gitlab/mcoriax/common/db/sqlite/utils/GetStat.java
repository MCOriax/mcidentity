package io.gitlab.mcoriax.common.db.sqlite.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class GetStat {
    public static int invoke(Connection conn, UUID identityUuid, String statKey) {
        if (!isValidStatKey(statKey)) return 0;
        
        String sql = "SELECT " + statKey + " FROM identity_stats WHERE identity_uuid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
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
    }

    private static boolean isValidStatKey(String key) {
        return key != null && (key.equals("energy") || key.equals("health") || key.equals("food") || key.equals("strength") || 
               key.equals("defense") || key.equals("wisdom") || key.equals("luck") || 
               key.equals("agility") || key.equals("resilience"));
    }
}

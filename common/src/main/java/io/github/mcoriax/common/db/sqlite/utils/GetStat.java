package io.github.mcoriax.common.db.sqlite.utils;

import io.github.mcoriax.common.db.StatKeys;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class GetStat {
    public static int invoke(Connection conn, UUID identityUuid, String statKey) {
        if (!StatKeys.isValid(statKey)) return 0;

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
}

package io.gitlab.mcoriax.common.db.postgresql.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GetCosmetics {
    private static final String SELECT_COSMETICS = "SELECT skin, aura FROM identity_cosmetics WHERE identity_uuid = ?";

    public static Map<String, String> invoke(Connection conn, UUID uuid) {
        Map<String, String> cosmetics = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_COSMETICS)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    cosmetics.put("skin", rs.getString("skin"));
                    cosmetics.put("aura", rs.getString("aura"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cosmetics;
    }
}

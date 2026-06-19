package io.github.mcoriax.common.db.postgresql.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CreateProfession {
    public static void invoke(Connection conn, String systemCode, String displayName) {
        String sql = "INSERT INTO identity_professions (system_code, display_name) VALUES (?, ?) ON CONFLICT (system_code) DO NOTHING";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, systemCode);
            ps.setString(2, displayName);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

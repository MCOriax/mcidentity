package io.github.mcoriax.common.db.sqlite.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class UpdateSessionContentKey {
    public static void invoke(Connection conn, UUID playerUuid, String contentKey) {
        String sql = "UPDATE identity_sessions SET content_key = ? WHERE player_uuid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, contentKey);
            ps.setString(2, playerUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

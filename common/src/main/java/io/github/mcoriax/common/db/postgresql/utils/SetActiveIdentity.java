package io.github.mcoriax.common.db.postgresql.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class SetActiveIdentity {
    public static void invoke(Connection conn, UUID playerUuid, UUID identityUuid, String contentKey) {
        if (identityUuid == null) {
            String sql = "DELETE FROM identity_sessions WHERE player_uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            String sql = "INSERT INTO identity_sessions (player_uuid, identity_uuid, content_key) VALUES (?, ?, ?) " +
                         "ON CONFLICT (player_uuid) DO UPDATE SET identity_uuid = EXCLUDED.identity_uuid, content_key = EXCLUDED.content_key";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, identityUuid.toString());
                ps.setString(3, contentKey);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

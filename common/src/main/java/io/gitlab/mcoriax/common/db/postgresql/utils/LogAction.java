package io.gitlab.mcoriax.common.db.postgresql.utils;

import io.gitlab.mcoriax.api.model.ActionType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class LogAction {
    public static void invoke(Connection conn, UUID playerUuid, UUID identityUuid, ActionType actionType) {
        String sql = "INSERT INTO identity_logs (player_uuid, identity_uuid, action_type) VALUES (?, ?, ?::action_type_enum)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, identityUuid.toString());
            ps.setString(3, actionType.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

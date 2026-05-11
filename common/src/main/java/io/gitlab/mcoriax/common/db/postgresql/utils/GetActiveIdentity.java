package io.gitlab.mcoriax.common.db.postgresql.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class GetActiveIdentity {
    public static Optional<UUID> invoke(Connection conn, UUID playerUuid) {
        String sql = "SELECT identity_uuid FROM identity_sessions WHERE player_uuid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String id = rs.getString("identity_uuid");
                    return id != null ? Optional.of(UUID.fromString(id)) : Optional.empty();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}

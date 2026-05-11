package io.gitlab.mcoriax.common.db.postgresql.utils;

import io.gitlab.mcoriax.api.model.IdentitySession;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class GetSession {
    public static Optional<IdentitySession> invoke(Connection conn, UUID playerUuid) {
        String sql = "SELECT identity_uuid, content_key FROM identity_sessions WHERE player_uuid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String idStr = rs.getString("identity_uuid");
                    if (idStr == null) return Optional.empty();
                    
                    UUID identityUuid = UUID.fromString(idStr);
                    String contentKey = rs.getString("content_key");
                    
                    return Optional.of(new IdentitySession() {
                        @Override public UUID getPlayerUniqueId() { return playerUuid; }
                        @Override public UUID getIdentityUniqueId() { return identityUuid; }
                        @Override public String getContentKey() { return contentKey; }
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}

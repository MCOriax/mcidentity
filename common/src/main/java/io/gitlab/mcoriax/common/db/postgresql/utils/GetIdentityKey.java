package io.gitlab.mcoriax.common.db.postgresql.utils;

import io.gitlab.mcoriax.api.model.IdentityKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class GetIdentityKey {
    public static Optional<IdentityKey> invoke(Connection conn, UUID uuid) {
        String sql = "SELECT p.system_code as profession FROM identity_keys k " +
                     "JOIN identity_professions p ON k.profession_id = p.id WHERE k.uuid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String profession = rs.getString("profession");
                    return Optional.of(new IdentityKey() {
                        @Override public UUID getUniqueId() { return uuid; }
                        @Override public String getProfessionCode() { return profession; }
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}

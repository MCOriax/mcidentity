package io.gitlab.mcoriax.common.db.sqlite.utils;

import io.gitlab.mcoriax.api.model.IdentityProperties;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class GetProperties {
    private static final String SELECT_PROPERTIES = "SELECT current_level, exp, stat_points, head_text FROM identity_properties WHERE identity_uuid = ?";

    public static Optional<IdentityProperties> invoke(Connection conn, UUID identityUuid) {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_PROPERTIES)) {
            ps.setString(1, identityUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int level = rs.getInt("current_level");
                    int exp = rs.getInt("exp");
                    int points = rs.getInt("stat_points");
                    String head = rs.getString("head_text");
                    return Optional.of(new IdentityProperties() {
                        private String skin;
                        private String aura;
                        @Override public int getLevel() { return level; }
                        @Override public int getExp() { return exp; }
                        @Override public int getStatPoints() { return points; }
                        @Override public String getHeadText() { return head; }
                        @Override public String getSkin() { return skin; }
                        @Override public void setSkin(String skin) { this.skin = skin; }
                        @Override public String getAura() { return aura; }
                        @Override public void setAura(String aura) { this.aura = aura; }
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}

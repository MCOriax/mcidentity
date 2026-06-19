package io.github.mcoriax.common.db.sqlite.utils;

import io.github.mcoriax.api.model.IdentityProperties;
import io.github.mcoriax.common.model.MutableIdentityProperties;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class GetProperties {
    private static final String SELECT_PROPERTIES = "SELECT current_level, exp, stat_points, head_text, inventory_contents, enderchest_contents FROM identity_properties WHERE identity_uuid = ?";

    public static Optional<IdentityProperties> invoke(Connection conn, UUID identityUuid) {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_PROPERTIES)) {
            ps.setString(1, identityUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int level = rs.getInt("current_level");
                    int exp = rs.getInt("exp");
                    int points = rs.getInt("stat_points");
                    String head = rs.getString("head_text");
                    String inv = rs.getString("inventory_contents");
                    String ec = rs.getString("enderchest_contents");

                    return Optional.of(new MutableIdentityProperties(level, exp, points, head, null, null, inv, ec));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}

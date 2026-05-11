package io.gitlab.mcoriax.common.db.postgresql.utils;

import io.gitlab.mcoriax.api.model.IdentityProperties;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class UpdateProperties {
    private static final String UPDATE_PROPERTIES = "UPDATE identity_properties SET current_level = ?, exp = ?, stat_points = ?, head_text = ? WHERE identity_uuid = ?";

    public static void invoke(Connection conn, UUID identityUuid, IdentityProperties properties) {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_PROPERTIES)) {
            ps.setInt(1, properties.getLevel());
            ps.setInt(2, properties.getExp());
            ps.setInt(3, properties.getStatPoints());
            ps.setString(4, properties.getHeadText());
            ps.setString(5, identityUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

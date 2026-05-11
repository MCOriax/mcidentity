package io.gitlab.mcoriax.common.db.postgresql.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class SaveSkin {
    private static final String SAVE_SKIN = "INSERT INTO identity_cosmetics (identity_uuid, skin) VALUES (?, ?) ON CONFLICT (identity_uuid) DO UPDATE SET skin = EXCLUDED.skin";

    public static void invoke(Connection conn, UUID uuid, String skin) {
        try (PreparedStatement ps = conn.prepareStatement(SAVE_SKIN)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, skin);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

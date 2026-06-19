package io.github.mcoriax.common.db.postgresql.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class SaveAura {
    private static final String SAVE_AURA = "INSERT INTO identity_equipped_cosmetics (identity_uuid, aura) VALUES (?, ?) ON CONFLICT (identity_uuid) DO UPDATE SET aura = EXCLUDED.aura";

    public static void invoke(Connection conn, UUID uuid, String aura) {
        try (PreparedStatement ps = conn.prepareStatement(SAVE_AURA)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, aura);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

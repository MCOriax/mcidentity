package io.gitlab.mcoriax.common.db.postgresql.utils;

import io.gitlab.mcoriax.api.model.AbilityType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CreateAbility {
    public static void invoke(Connection conn, String systemCode, String displayName, AbilityType type, boolean isGlobal) {
        String sql = "INSERT INTO identity_abilities (system_code, display_name, ability_type, is_global) VALUES (?, ?, ?::ability_type_enum, ?) ON CONFLICT (system_code) DO NOTHING";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, systemCode);
            ps.setString(2, displayName);
            ps.setString(3, type.name());
            ps.setBoolean(4, isGlobal);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

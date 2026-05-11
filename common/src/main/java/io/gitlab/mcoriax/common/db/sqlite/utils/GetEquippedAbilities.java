package io.gitlab.mcoriax.common.db.sqlite.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GetEquippedAbilities {
    public static Map<Integer, String> invoke(Connection conn, UUID identityUuid) {
        String sql = "SELECT a.system_code, ia.slot_number FROM identity_abilities ia " +
                     "JOIN abilities a ON ia.ability_id = a.id WHERE ia.identity_uuid = ?";
        Map<Integer, String> abilities = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, identityUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    abilities.put(rs.getInt("slot_number"), rs.getString("system_code"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return abilities;
    }
}

package io.gitlab.mcoriax.common.db.sqlite.utils;

import io.gitlab.mcoriax.api.model.EquippedAbility;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GetEquippedAbilities {
    public static Map<Integer, EquippedAbility> invoke(Connection conn, UUID identityUuid) {
        String sql = "SELECT a.system_code, ia.slot_number, ia.model_id FROM identity_equipped_abilities ia " +
                     "JOIN identity_abilities a ON ia.ability_id = a.id WHERE ia.identity_uuid = ?";
        Map<Integer, EquippedAbility> identity_abilities = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, identityUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    identity_abilities.put(rs.getInt("slot_number"), new EquippedAbility(
                        rs.getString("system_code"),
                        rs.getString("model_id")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return identity_abilities;
    }
}

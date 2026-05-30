package io.gitlab.mcoriax.common.db.postgresql.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class SetEquippedAbility {
    public static void invoke(Connection conn, UUID identityUuid, int slot, String abilityCode, String modelId) {
        if (abilityCode == null) {
            String sql = "DELETE FROM identity_abilities WHERE identity_uuid = ? AND slot_number = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, identityUuid.toString());
                ps.setInt(2, slot);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            String sql = "INSERT INTO identity_abilities (identity_uuid, ability_id, slot_number, model_id) " +
                         "VALUES (?, (SELECT id FROM abilities WHERE system_code = ?), ?, ?) " +
                         "ON CONFLICT (identity_uuid, slot_number) DO UPDATE SET ability_id = EXCLUDED.ability_id, model_id = EXCLUDED.model_id";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, identityUuid.toString());
                ps.setString(2, abilityCode);
                ps.setInt(3, slot);
                ps.setString(4, modelId);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

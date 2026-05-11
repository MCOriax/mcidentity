package io.gitlab.mcoriax.common.db.sqlite.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class SetRune {
    public static void invoke(Connection conn, UUID identityUuid, int slot, String runeCode) {
        if (runeCode == null) {
            String sql = "DELETE FROM identity_runes WHERE identity_uuid = ? AND slot_number = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, identityUuid.toString());
                ps.setInt(2, slot);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            String sql = "INSERT OR REPLACE INTO identity_runes (identity_uuid, rune_id, slot_number) " +
                         "VALUES (?, (SELECT id FROM runes WHERE system_code = ?), ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, identityUuid.toString());
                ps.setString(2, runeCode);
                ps.setInt(3, slot);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

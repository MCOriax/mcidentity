package io.github.mcoriax.common.db.sqlite.utils;

import io.github.mcoriax.common.db.StatKeys;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class AddStat {
    public static void invoke(Connection conn, UUID identityUuid, String statKey, int amount) {
        if (!StatKeys.isValid(statKey)) return;

        String sql = String.format("UPDATE identity_stats SET %s = %s + ? WHERE identity_uuid = ?", statKey, statKey);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setString(2, identityUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

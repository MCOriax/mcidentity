package io.gitlab.mcoriax.common.db.postgresql.utils;

import io.gitlab.mcoriax.api.model.Rune;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GetRunes {
    public static Collection<Rune> invoke(Connection conn) {
        List<Rune> runes = new ArrayList<>();
        String sql = "SELECT system_code, display_name FROM runes";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String code = rs.getString("system_code");
                String name = rs.getString("display_name");
                runes.add(new Rune() {
                    @Override public String getSystemCode() { return code; }
                    @Override public String getDisplayName() { return name; }
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return runes;
    }

    public static Map<Integer, String> invoke(Connection conn, UUID identityUuid) {
        String sql = "SELECT r.system_code, ir.slot_number FROM identity_runes ir " +
                     "JOIN runes r ON ir.rune_id = r.id WHERE ir.identity_uuid = ?";
        Map<Integer, String> runes = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, identityUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    runes.put(rs.getInt("slot_number"), rs.getString("system_code"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return runes;
    }
}

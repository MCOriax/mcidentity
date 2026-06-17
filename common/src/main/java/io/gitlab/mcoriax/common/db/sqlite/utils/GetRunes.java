package io.gitlab.mcoriax.common.db.sqlite.utils;

import io.gitlab.mcoriax.api.model.Rune;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

public class GetRunes {
    public static Collection<Rune> invoke(Connection conn) {
        List<Rune> identity_runes = new ArrayList<>();
        String sql = "SELECT system_code, display_name FROM identity_runes";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String code = rs.getString("system_code");
                String name = rs.getString("display_name");
                identity_runes.add(new Rune() {
                    @Override public String getSystemCode() { return code; }
                    @Override public String getDisplayName() { return name; }
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return identity_runes;
    }

    public static Map<Integer, String> invoke(Connection conn, UUID identityUuid) {
        String sql = "SELECT r.system_code, ir.slot_number FROM identity_equipped_runes ir " +
                     "JOIN identity_runes r ON ir.rune_id = r.id WHERE ir.identity_uuid = ?";
        Map<Integer, String> identity_runes = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, identityUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    identity_runes.put(rs.getInt("slot_number"), rs.getString("system_code"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return identity_runes;
    }
}

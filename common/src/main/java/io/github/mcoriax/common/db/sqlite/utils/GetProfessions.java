package io.github.mcoriax.common.db.sqlite.utils;

import io.github.mcoriax.api.model.Profession;
import io.github.mcoriax.common.MCIdentityManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class GetProfessions {
    public static Collection<Profession> invoke(Connection conn) {
        List<Profession> identity_professions = new ArrayList<>();
        String sql = "SELECT system_code, display_name FROM identity_professions";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String code = rs.getString("system_code");
                String name = rs.getString("display_name");
                identity_professions.add(new Profession() {
                    @Override public String getSystemCode() { return code; }
                    @Override public String getDisplayName() { return name; }
                    @Override public Map<String, Object> getExpConfig() {
                        return MCIdentityManager.getProfessionExpConfig(code);
                    }
                    @Override public Map<String, Integer> getBaseStats() {
                        return MCIdentityManager.getProfessionBaseStats(code);
                    }
                    @Override public String getStatsRandom() {
                        return MCIdentityManager.getProfessionStatsRandom(code);
                    }
                    @Override public Map<Integer, String> getSkins() {
                        return MCIdentityManager.getProfessionSkins(code);
                    }
                    @Override public void setSkins(Map<Integer, String> skins) {
                        MCIdentityManager.setProfessionSkins(code, skins);
                    }
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return identity_professions;
    }
}

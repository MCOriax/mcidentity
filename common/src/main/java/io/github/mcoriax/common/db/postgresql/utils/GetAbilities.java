package io.github.mcoriax.common.db.postgresql.utils;

import io.github.mcoriax.api.model.Ability;
import io.github.mcoriax.api.model.AbilityType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GetAbilities {
    public static Collection<Ability> invoke(Connection conn) {
        List<Ability> identity_abilities = new ArrayList<>();
        String sql = "SELECT system_code, display_name, ability_type, is_global FROM identity_abilities";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String code = rs.getString("system_code");
                String name = rs.getString("display_name");
                AbilityType type = AbilityType.valueOf(rs.getString("ability_type"));
                boolean global = rs.getBoolean("is_global");
                identity_abilities.add(new Ability() {
                    @Override public String getSystemCode() { return code; }
                    @Override public String getDisplayName() { return name; }
                    @Override public AbilityType getAbilityType() { return type; }
                    @Override public boolean isGlobal() { return global; }
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return identity_abilities;
    }
}

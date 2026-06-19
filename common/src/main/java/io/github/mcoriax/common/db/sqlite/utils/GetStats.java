package io.github.mcoriax.common.db.sqlite.utils;

import io.github.mcoriax.api.model.IdentityStats;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class GetStats {
    private static final String SELECT_STATS = "SELECT energy, health, food, strength, defense, wisdom, luck, agility, resilience FROM identity_stats WHERE identity_uuid = ?";

    public static Optional<IdentityStats> invoke(Connection conn, UUID identityUuid) {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_STATS)) {
            ps.setString(1, identityUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int energy = rs.getInt("energy");
                    int health = rs.getInt("health");
                    int food = rs.getInt("food");
                    int strength = rs.getInt("strength");
                    int defense = rs.getInt("defense");
                    int wisdom = rs.getInt("wisdom");
                    int luck = rs.getInt("luck");
                    int agility = rs.getInt("agility");
                    int resilience = rs.getInt("resilience");
                    return Optional.of(new IdentityStats() {
                        @Override public int getEnergy() { return energy; }
                        @Override public int getHealth() { return health; }
                        @Override public int getFood() { return food; }
                        @Override public int getStrength() { return strength; }
                        @Override public int getDefense() { return defense; }
                        @Override public int getWisdom() { return wisdom; }
                        @Override public int getLuck() { return luck; }
                        @Override public int getAgility() { return agility; }
                        @Override public int getResilience() { return resilience; }
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}

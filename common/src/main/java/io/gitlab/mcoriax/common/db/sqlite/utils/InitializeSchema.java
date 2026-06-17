package io.gitlab.mcoriax.common.db.sqlite.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class InitializeSchema {
    public static void invoke(Connection conn) {
        String[] schema = {
            "CREATE TABLE IF NOT EXISTS identity_professions (id INTEGER PRIMARY KEY AUTOINCREMENT, system_code VARCHAR(255) UNIQUE NOT NULL, display_name VARCHAR(255) NOT NULL)",
            "CREATE TABLE IF NOT EXISTS identity_abilities (id INTEGER PRIMARY KEY AUTOINCREMENT, system_code VARCHAR(255) UNIQUE NOT NULL, display_name VARCHAR(255) NOT NULL, ability_type VARCHAR(50) NOT NULL, is_global BOOLEAN DEFAULT 0)",
            "CREATE TABLE IF NOT EXISTS identity_runes (id INTEGER PRIMARY KEY AUTOINCREMENT, system_code VARCHAR(255) UNIQUE NOT NULL, display_name VARCHAR(255) NOT NULL)",
            "CREATE TABLE IF NOT EXISTS identity_keys (uuid VARCHAR(36) PRIMARY KEY, profession_id INT NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (profession_id) REFERENCES identity_professions(id))",
            "CREATE TABLE IF NOT EXISTS identity_properties (identity_uuid VARCHAR(36) PRIMARY KEY, current_level INT DEFAULT 1, exp INT DEFAULT 0, stat_points INT DEFAULT 0, head_text TEXT DEFAULT NULL, inventory_contents TEXT DEFAULT NULL, enderchest_contents TEXT DEFAULT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_stats (identity_uuid VARCHAR(36) PRIMARY KEY, energy INT DEFAULT 100, health INT DEFAULT 20, food INT DEFAULT 20, strength INT DEFAULT 0, defense INT DEFAULT 0, wisdom INT DEFAULT 0, luck INT DEFAULT 0, agility INT DEFAULT 0, resilience INT DEFAULT 0, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_equipped_abilities (identity_uuid VARCHAR(36) NOT NULL, ability_id INT NOT NULL, slot_number INT NOT NULL, custom_hotkey VARCHAR(50), model_id VARCHAR(255) DEFAULT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (identity_uuid, ability_id, slot_number), UNIQUE (identity_uuid, slot_number), UNIQUE (identity_uuid, ability_id), FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE, FOREIGN KEY (ability_id) REFERENCES identity_abilities(id))",
            "CREATE TABLE IF NOT EXISTS identity_equipped_runes (identity_uuid VARCHAR(36) NOT NULL, rune_id INT NOT NULL, slot_number INT NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (identity_uuid, rune_id, slot_number), UNIQUE (identity_uuid, slot_number), FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE, FOREIGN KEY (rune_id) REFERENCES identity_runes(id))",
            "CREATE TABLE IF NOT EXISTS identity_sessions (player_uuid VARCHAR(36) PRIMARY KEY, identity_uuid VARCHAR(36) UNIQUE, content_key TEXT, equipped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_cosmetics (identity_uuid VARCHAR(36) PRIMARY KEY, skin TEXT DEFAULT NULL, aura VARCHAR(255) DEFAULT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS identity_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid VARCHAR(36) NOT NULL, identity_uuid VARCHAR(36) NOT NULL, action_type VARCHAR(50) NOT NULL, action_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (identity_uuid) REFERENCES identity_keys(uuid) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS archived_identity_keys (uuid VARCHAR(36) PRIMARY KEY, profession_id INT NOT NULL, archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS archived_identity_properties (identity_uuid VARCHAR(36) PRIMARY KEY, current_level INT, exp INT, stat_points INT, head_text TEXT, inventory_contents TEXT, enderchest_contents TEXT, archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS archived_identity_stats (identity_uuid VARCHAR(36) PRIMARY KEY, energy INT, health INT, food INT, strength INT, defense INT, wisdom INT, luck INT, agility INT, resilience INT, archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS archived_identity_cosmetics (identity_uuid VARCHAR(36) PRIMARY KEY, skin TEXT DEFAULT NULL, aura VARCHAR(255) DEFAULT NULL, archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "CREATE TABLE IF NOT EXISTS archived_identity_equipped_abilities (identity_uuid VARCHAR(36), ability_id INT, slot_number INT, custom_hotkey VARCHAR(50), model_id VARCHAR(255) DEFAULT NULL, archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (identity_uuid, ability_id, slot_number))",
            "CREATE TABLE IF NOT EXISTS archived_identity_equipped_runes (identity_uuid VARCHAR(36), rune_id INT, slot_number INT, archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (identity_uuid, rune_id, slot_number))",
            "CREATE TABLE IF NOT EXISTS archived_identity_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid VARCHAR(36), identity_uuid VARCHAR(36), action_type VARCHAR(50), action_time TIMESTAMP, archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
        };

        for (String sql : schema) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

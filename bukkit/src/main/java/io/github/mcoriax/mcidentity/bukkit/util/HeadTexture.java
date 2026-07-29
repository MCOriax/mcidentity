package io.github.mcoriax.mcidentity.bukkit.util;

import io.github.mcoriax.mcidentity.common.MCIdentityManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Utility class containing custom head textures (Base64) used in the plugin's GUIs.
 * Provides a selection of default textures for different professions.
 * Fetches values from the plugin's configuration files.
 */
public final class HeadTexture {

    private HeadTexture() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static FileConfiguration getConfig() {
        return (FileConfiguration) MCIdentityManager.getTexturesConfig();
    }

    /**
     * Texture for the Back button (to return to the main Identity GUI).
     */
    public static String getBackButton(JavaPlugin plugin) {
        String texture = getConfig().getString("gui.buttons.back");
        if (texture == null) {
            plugin.getLogger().log(Level.WARNING, "Back button texture missing in textures.yml! Using fallback.");
            return "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGNlYzgwN2RjYzE0MzYzMzRmZDRkYzlhYjM0OTM0MmY2YzUyYzllN2IyYmYzNDY3MTJkYjcyYTBkNmQ3YTQifX19";
        }
        return texture;
    }

    /**
     * Default texture for unidentified (random) identities.
     */
    public static String getUnidentified(JavaPlugin plugin) {
        String texture = getConfig().getString("gui.buttons.unidentified");
        if (texture == null) {
            plugin.getLogger().log(Level.WARNING, "Unidentified texture missing in textures.yml! Using fallback.");
            return "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWQ5Y2M1OGFkMjVhMWFiMTZkMzZiYjVkNmQ0OTNjOGY1ODk4YzJiZjMwMmI2NGUzMjU5MjFjNDFjMzU4NjcifX19";
        }
        return texture;
    }

    /**
     * Fetches default textures for a specific profession.
     */
    public static List<String> getProfessionDefaults(JavaPlugin plugin, String professionCode) {
        Map<Integer, String> skins = MCIdentityManager.getProfessionSkins(professionCode);
        if (skins.isEmpty()) {
            plugin.getLogger().log(Level.WARNING, "No skins found for profession: " + professionCode);
            return Collections.emptyList();
        }

        return new ArrayList<>(skins.values());
    }
}

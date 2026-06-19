package io.github.mcoriax.api.model;

import java.util.Map;

/**
 * Represents a Profession type.
 */
public interface Profession {
    /**
     * Gets the unique system code.
     * @return The system code.
     */
    String getSystemCode();

    /**
     * Gets the display name.
     * @return The display name.
     */
    String getDisplayName();

    /**
     * Gets the custom EXP gain configurations.
     * @return A map of action key to EXP value/range string or nested condition maps.
     */
    Map<String, Object> getExpConfig();

    /**
     * Gets the base stats for this profession.
     * @return A map of stat names to their base values.
     */
    Map<String, Integer> getBaseStats();

    /**
     * Gets the stats randomization configuration.
     * @return The randomization config string (e.g., "100" or "100~120").
     */
    String getStatsRandom();

    /**
     * Gets the available skins for this profession.
     * @return A map of skin ID to texture string.
     */
    Map<Integer, String> getSkins();

    /**
     * Sets the available skins for this profession.
     * @param skins A map of skin ID to texture string.
     */
    void setSkins(Map<Integer, String> skins);
}

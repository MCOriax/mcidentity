package io.gitlab.mcoriax.api.model;

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
     * @return A map of action key to EXP value/range string.
     */
    Map<String, String> getExpConfig();

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
}

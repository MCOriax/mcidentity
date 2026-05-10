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
}

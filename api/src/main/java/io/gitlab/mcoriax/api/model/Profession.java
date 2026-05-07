package io.gitlab.mcoriax.api.model;

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
}

package io.gitlab.mcoriax.api.model;

/**
 * Represents a Sigil type.
 */
public interface Sigil {
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

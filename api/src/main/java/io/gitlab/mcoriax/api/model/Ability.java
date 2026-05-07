package io.gitlab.mcoriax.api.model;

/**
 * Represents an Ability type.
 */
public interface Ability {
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
     * Gets the activation type of the ability.
     * @return The ability type.
     */
    AbilityType getAbilityType();

    /**
     * Checks if the ability is global.
     * @return True if global.
     */
    boolean isGlobal();
}

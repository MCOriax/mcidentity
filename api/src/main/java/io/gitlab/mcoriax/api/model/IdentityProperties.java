package io.gitlab.mcoriax.api.model;

/**
 * General progression attributes for an identity.
 */
public interface IdentityProperties {
    /**
     * Gets the current level of the identity.
     * @return The level.
     */
    int getLevel();

    /**
     * Gets the current experience points of the identity.
     * @return The experience points.
     */
    int getExp();

    /**
     * Gets the unspent stat points available for upgrading.
     * @return The stat points.
     */
    int getStatPoints();

    /**
     * Gets the custom texture (Base64 string) for the identity skull.
     * @return The head texture string.
     */
    String getHeadText();
}

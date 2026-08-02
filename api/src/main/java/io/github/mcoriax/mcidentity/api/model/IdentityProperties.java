package io.github.mcoriax.mcidentity.api.model;

/**
 * General progression attributes for an identity.
 */
public interface IdentityProperties {
    /**
     * Gets the player chosen alias of the identity.
     *
     * <p>An alias is optional, contains only alphanumeric characters, and is
     * unique across every identity on the network. Uniqueness is compared
     * without regard to case, while the value returned here keeps the casing
     * the player originally supplied. Aliases are never written through
     * {@link io.github.mcoriax.mcidentity.api.db.IMCIdentityDB#updateProperties};
     * they are claimed through the dedicated alias operation so the uniqueness
     * check and the write stay atomic.</p>
     * @return The alias, or null when the identity has none.
     */
    String getAlias();

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

    /**
     * Gets the serialized inventory contents.
     * @return The Base64 serialized inventory string.
     */
    String getInventoryContents();

    /**
     * Sets the serialized inventory contents.
     * @param inventoryContents The Base64 serialized inventory string.
     */
    void setInventoryContents(String inventoryContents);

    /**
     * Gets the serialized enderchest contents.
     * @return The Base64 serialized enderchest string.
     */
    String getEnderchestContents();

    /**
     * Sets the serialized enderchest contents.
     * @param enderchestContents The Base64 serialized enderchest string.
     */
    void setEnderchestContents(String enderchestContents);
}

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

    /**
     * Gets the skin cosmetic (Base64 string).
     * @return The skin texture string.
     */
    String getSkin();

    /**
     * Sets the skin cosmetic (Base64 string).
     * @param skin The skin texture string.
     */
    void setSkin(String skin);

    /**
     * Gets the aura cosmetic system code.
     * @return The aura code.
     */
    String getAura();

    /**
     * Sets the aura cosmetic system code.
     * @param aura The aura code.
     */
    void setAura(String aura);

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

package io.github.mcoriax.mcidentity.api.model;

/**
 * The nine numerical attributes of a player's one character.
 *
 * <p>Keyed by the player's own UUID. The stats and the unspent stat points
 * belong to the player, whatever profession they wear; levelling any
 * profession adds stat points here.</p>
 */
public interface PlayerStats {

    /**
     * Gets the stat points the player has not spent yet.
     * @return The unspent stat points.
     */
    int getStatPoints();

    /**
     * Gets the energy points.
     * @return The energy.
     */
    int getEnergy();

    /**
     * Gets the health points.
     * @return The health.
     */
    int getHealth();

    /**
     * Gets the strength points.
     * @return The strength.
     */
    int getStrength();

    /**
     * Gets the defense points.
     * @return The defense.
     */
    int getDefense();

    /**
     * Gets the wisdom points.
     * @return The wisdom.
     */
    int getWisdom();

    /**
     * Gets the luck points.
     * @return The luck.
     */
    int getLuck();

    /**
     * Gets the agility points.
     * @return The agility.
     */
    int getAgility();

    /**
     * Gets the resilience points.
     * @return The resilience.
     */
    int getResilience();

    /**
     * Gets the food points.
     * @return The food.
     */
    int getFood();
}

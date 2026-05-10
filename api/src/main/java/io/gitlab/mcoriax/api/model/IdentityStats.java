package io.gitlab.mcoriax.api.model;

/**
 * Combat and survival attributes for an identity.
 */
public interface IdentityStats {
    /**
     * Gets the energy (MP) of the identity.
     * @return The energy value.
     */
    int getEnergy();

    /**
     * Gets the maximum health of the identity.
     * @return The health value.
     */
    int getHealth();

    /**
     * Gets the base physical damage and weight capacity.
     * @return The strength value.
     */
    int getStrength();

    /**
     * Gets the physical damage mitigation value.
     * @return The defense value.
     */
    int getDefense();

    /**
     * Gets the ability scaling, healing, and magical output.
     * @return The wisdom value.
     */
    int getWisdom();

    /**
     * Gets the critical hit chance and loot drop rates.
     * @return The luck value.
     */
    int getLuck();

    /**
     * Gets the movement speed, attack speed, and evasion.
     * @return The agility value.
     */
    int getAgility();

    /**
     * Gets the resistance to status effects and crowd control.
     * @return The resilience value.
     */
    int getResilience();

    /**
     * Gets the food capacity for regeneration logic.
     * @return The food value.
     */
    int getFood();
}

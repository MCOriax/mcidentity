package io.github.mcoriax.mcidentity.common.model;

import io.github.mcoriax.mcidentity.api.model.IdentityStats;

/**
 * Immutable implementation of {@link IdentityStats} carrying all nine stats.
 *
 * <p>Replaces the anonymous stats views the storage backends and the base
 * identity registrar each built from their own nine locals.</p>
 *
 * @param energy The energy stat.
 * @param health The health stat.
 * @param food The food stat.
 * @param strength The strength stat.
 * @param defense The defense stat.
 * @param wisdom The wisdom stat.
 * @param luck The luck stat.
 * @param agility The agility stat.
 * @param resilience The resilience stat.
 */
public record IdentityStatsRecord(int energy, int health, int food, int strength, int defense,
                                  int wisdom, int luck, int agility, int resilience) implements IdentityStats {

    /** {@inheritDoc} */
    @Override
    public int getEnergy() {
        return energy;
    }

    /** {@inheritDoc} */
    @Override
    public int getHealth() {
        return health;
    }

    /** {@inheritDoc} */
    @Override
    public int getFood() {
        return food;
    }

    /** {@inheritDoc} */
    @Override
    public int getStrength() {
        return strength;
    }

    /** {@inheritDoc} */
    @Override
    public int getDefense() {
        return defense;
    }

    /** {@inheritDoc} */
    @Override
    public int getWisdom() {
        return wisdom;
    }

    /** {@inheritDoc} */
    @Override
    public int getLuck() {
        return luck;
    }

    /** {@inheritDoc} */
    @Override
    public int getAgility() {
        return agility;
    }

    /** {@inheritDoc} */
    @Override
    public int getResilience() {
        return resilience;
    }
}

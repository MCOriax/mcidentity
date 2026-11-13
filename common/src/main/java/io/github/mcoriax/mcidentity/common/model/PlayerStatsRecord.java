package io.github.mcoriax.mcidentity.common.model;

import io.github.mcoriax.mcidentity.api.model.PlayerStats;

/**
 * Immutable {@link PlayerStats} of a player's one character.
 *
 * @param statPoints The unspent stat points.
 * @param energy     The energy points.
 * @param health     The health points.
 * @param food       The food points.
 * @param strength   The strength points.
 * @param defense    The defense points.
 * @param wisdom     The wisdom points.
 * @param luck       The luck points.
 * @param agility    The agility points.
 * @param resilience The resilience points.
 */
public record PlayerStatsRecord(int statPoints, int energy, int health, int food, int strength, int defense,
                                int wisdom, int luck, int agility, int resilience) implements PlayerStats {

    /**
     * Copies any {@link PlayerStats} into a record.
     * @param source The stats to copy.
     * @return The record.
     */
    public static PlayerStatsRecord copyOf(PlayerStats source) {
        return new PlayerStatsRecord(source.getStatPoints(), source.getEnergy(), source.getHealth(),
            source.getFood(), source.getStrength(), source.getDefense(), source.getWisdom(),
            source.getLuck(), source.getAgility(), source.getResilience());
    }

    /**
     * Returns a copy with a different number of unspent stat points.
     * @param statPoints The new unspent stat points.
     * @return The updated record.
     */
    public PlayerStatsRecord withStatPoints(int statPoints) {
        return new PlayerStatsRecord(statPoints, energy, health, food, strength, defense,
            wisdom, luck, agility, resilience);
    }

    @Override
    public int getStatPoints() {
        return statPoints;
    }

    @Override
    public int getEnergy() {
        return energy;
    }

    @Override
    public int getHealth() {
        return health;
    }

    @Override
    public int getFood() {
        return food;
    }

    @Override
    public int getStrength() {
        return strength;
    }

    @Override
    public int getDefense() {
        return defense;
    }

    @Override
    public int getWisdom() {
        return wisdom;
    }

    @Override
    public int getLuck() {
        return luck;
    }

    @Override
    public int getAgility() {
        return agility;
    }

    @Override
    public int getResilience() {
        return resilience;
    }
}

package io.github.mcoriax.api.model;

/**
 * Represents the activation type of an ability.
 */
public enum AbilityType {
    /**
     * Ability must be explicitly triggered by the player.
     */
    ACTIVE,
    /**
     * Ability provides passive effects without manual activation.
     */
    PASSIVE
}

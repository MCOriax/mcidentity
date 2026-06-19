package io.github.mcoriax.api.model;

import java.util.Map;
import java.util.UUID;

/**
 * An immutable record representing an active identity session in RAM.
 */
public record ActiveIdentitySession(
    UUID playerUuid,
    UUID identityUuid,
    IdentityKey identityKey,
    IdentityProperties properties,
    IdentityStats stats,
    String contentKey,
    double currentHealth,
    double currentEnergy,
    double currentFood,
    Map<Integer, EquippedAbility> equippedAbilities
) {

    /**
     * Returns a copy of this session with the given properties.
     * @param properties The new properties.
     * @return The derived session.
     */
    public ActiveIdentitySession withProperties(IdentityProperties properties) {
        return new ActiveIdentitySession(playerUuid, identityUuid, identityKey, properties, stats,
            contentKey, currentHealth, currentEnergy, currentFood, equippedAbilities);
    }

    /**
     * Returns a copy of this session with the given stats.
     * @param stats The new stats.
     * @return The derived session.
     */
    public ActiveIdentitySession withStats(IdentityStats stats) {
        return new ActiveIdentitySession(playerUuid, identityUuid, identityKey, properties, stats,
            contentKey, currentHealth, currentEnergy, currentFood, equippedAbilities);
    }

    /**
     * Returns a copy of this session with the given current health.
     * @param currentHealth The new current health.
     * @return The derived session.
     */
    public ActiveIdentitySession withCurrentHealth(double currentHealth) {
        return new ActiveIdentitySession(playerUuid, identityUuid, identityKey, properties, stats,
            contentKey, currentHealth, currentEnergy, currentFood, equippedAbilities);
    }

    /**
     * Returns a copy of this session with the given current energy.
     * @param currentEnergy The new current energy.
     * @return The derived session.
     */
    public ActiveIdentitySession withCurrentEnergy(double currentEnergy) {
        return new ActiveIdentitySession(playerUuid, identityUuid, identityKey, properties, stats,
            contentKey, currentHealth, currentEnergy, currentFood, equippedAbilities);
    }

    /**
     * Returns a copy of this session with the given current food.
     * @param currentFood The new current food.
     * @return The derived session.
     */
    public ActiveIdentitySession withCurrentFood(double currentFood) {
        return new ActiveIdentitySession(playerUuid, identityUuid, identityKey, properties, stats,
            contentKey, currentHealth, currentEnergy, currentFood, equippedAbilities);
    }

    /**
     * Returns a copy of this session with the given equipped abilities.
     * @param equippedAbilities The new equipped abilities.
     * @return The derived session.
     */
    public ActiveIdentitySession withEquippedAbilities(Map<Integer, EquippedAbility> equippedAbilities) {
        return new ActiveIdentitySession(playerUuid, identityUuid, identityKey, properties, stats,
            contentKey, currentHealth, currentEnergy, currentFood, equippedAbilities);
    }
}

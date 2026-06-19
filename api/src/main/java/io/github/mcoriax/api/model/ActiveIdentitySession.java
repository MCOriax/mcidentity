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
) {}

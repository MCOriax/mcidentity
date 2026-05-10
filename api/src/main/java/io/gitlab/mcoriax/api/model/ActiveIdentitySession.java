package io.gitlab.mcoriax.api.model;

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
    double currentHealth,
    double currentEnergy,
    double currentFood
) {}

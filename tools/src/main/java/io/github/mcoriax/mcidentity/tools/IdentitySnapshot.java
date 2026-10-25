package io.github.mcoriax.mcidentity.tools;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A read-only copy of a player's active identity, as plain data.
 *
 * @param identityUuid The identity's UUID.
 * @param professionCode The profession system code.
 * @param professionName The profession display name.
 * @param alias The claimed alias, or null when none is claimed.
 * @param baseIdentity Whether the identity is one of the player's base identities.
 * @param level The current level.
 * @param maxLevel The profession's level cap; {@link Integer#MAX_VALUE} means no cap.
 * @param exp The EXP gathered towards the next level.
 * @param expPerLevel The EXP one level costs.
 * @param statPoints The unspent stat points.
 * @param stats The stat values by stat name, in display order.
 * @param currentHealth The live health value.
 * @param currentEnergy The live energy value.
 * @param currentFood The live food value.
 */
public record IdentitySnapshot(
    String identityUuid,
    String professionCode,
    String professionName,
    String alias,
    boolean baseIdentity,
    int level,
    int maxLevel,
    int exp,
    int expPerLevel,
    int statPoints,
    Map<String, Integer> stats,
    double currentHealth,
    double currentEnergy,
    double currentFood
) {

    /**
     * Copies the stats so the snapshot cannot change after it is taken.
     */
    public IdentitySnapshot {
        stats = stats == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(stats));
    }
}

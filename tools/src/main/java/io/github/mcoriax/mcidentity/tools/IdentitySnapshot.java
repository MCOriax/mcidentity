package io.github.mcoriax.mcidentity.tools;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A read-only copy of a player's character and worn profession, as plain data.
 *
 * @param professionCode The worn profession's system code.
 * @param professionName The worn profession's display name.
 * @param baseProfession Whether the worn profession is the base {@code player} profession.
 * @param level The worn profession's level.
 * @param maxLevel The profession's level cap; {@link Integer#MAX_VALUE} means no cap.
 * @param exp The EXP gathered towards the next level.
 * @param expPerLevel The EXP one level costs.
 * @param statPoints The player's unspent stat points.
 * @param stats The player's stat values by stat name, in display order.
 * @param currentHealth The live health value.
 * @param currentEnergy The live energy value.
 * @param currentFood The live food value.
 */
public record IdentitySnapshot(
    String professionCode,
    String professionName,
    boolean baseProfession,
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

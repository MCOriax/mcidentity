package io.github.mcoriax.common.db;

import java.util.Set;

/**
 * Central allow-list of valid identity stat identifiers.
 *
 * <p>Stat keys are interpolated directly into SQL identifiers (column names) by
 * the relational backends, where they cannot be bound as prepared-statement
 * parameters. They must therefore be validated against a fixed allow-list before
 * use. Keeping that allow-list in a single place guarantees every backend
 * enforces the same set and prevents the security control from drifting between
 * implementations.</p>
 */
public final class StatKeys {

    /**
     * The immutable set of stat identifiers that may be used as column or field names.
     */
    private static final Set<String> VALID = Set.of(
        "energy", "health", "food", "strength", "defense",
        "wisdom", "luck", "agility", "resilience"
    );

    private StatKeys() {
    }

    /**
     * Returns whether the given key is a known, safe stat identifier.
     * @param key The stat key to validate.
     * @return true if the key is allow-listed, false if it is null or unknown.
     */
    public static boolean isValid(String key) {
        return key != null && VALID.contains(key);
    }
}

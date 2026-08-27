package io.github.mcoriax.mcidentity.common.db;

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

    /**
     * Returns every allow-listed stat identifier.
     *
     * <p>Backends that must splice a stat identifier into statement text build
     * that text once per member of this set, at class-initialization time. The
     * identifier they concatenate is therefore always one of the literals
     * declared above, and a caller's string is only ever used to look the
     * finished statement up.</p>
     *
     * @return The immutable allow-list of stat identifiers.
     */
    public static Set<String> all() {
        return VALID;
    }
}

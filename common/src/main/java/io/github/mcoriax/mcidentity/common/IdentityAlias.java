package io.github.mcoriax.mcidentity.common;

import java.util.Locale;

/**
 * The shared rules for identity aliases.
 *
 * <p>An alias is the optional, player chosen name of a single identity. It
 * contains only alphanumeric characters, is at most {@link #MAX_LENGTH}
 * characters long, and is unique across every identity known to the backend.
 * Uniqueness is compared without regard to case, so "Alex" and "alex" are the
 * same alias, while the value actually stored keeps the casing the player
 * supplied.</p>
 *
 * <p>Every backend and command path validates through this class, so the rule
 * lives in exactly one place; the comparison form produced by
 * {@link #comparable(String)} is what the backends match on.</p>
 */
public final class IdentityAlias {

    /**
     * The maximum number of characters an alias may contain, matching the
     * column width used by every storage backend.
     */
    public static final int MAX_LENGTH = 32;

    /**
     * Prevents instantiation of this utility holder.
     */
    private IdentityAlias() {
    }

    /**
     * Checks whether a value may be stored as an alias.
     * @param alias The candidate alias.
     * @return true when the value is non-null, between 1 and
     *         {@link #MAX_LENGTH} characters, and alphanumeric throughout.
     */
    public static boolean isValid(String alias) {
        if (alias == null || alias.isEmpty() || alias.length() > MAX_LENGTH) {
            return false;
        }
        for (int index = 0; index < alias.length(); index++) {
            char character = alias.charAt(index);
            boolean alphanumeric = (character >= 'a' && character <= 'z')
                || (character >= 'A' && character <= 'Z')
                || (character >= '0' && character <= '9');
            if (!alphanumeric) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts an alias to the form uniqueness is compared on. Aliases are
     * alphanumeric, so lower-casing in {@link Locale#ROOT} is a faithful
     * case-insensitive fold with no locale surprises.
     * @param alias The alias to fold.
     * @return The comparison form, or null when the input was null.
     */
    public static String comparable(String alias) {
        return alias != null ? alias.toLowerCase(Locale.ROOT) : null;
    }
}

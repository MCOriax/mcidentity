package io.gitlab.mcoriax.bukkit.util;

/**
 * Builds plain (legacy) message strings from a prefix/value/suffix shape.
 * <p>
 * Targets the plain spigot-api, so messages are assembled as legacy strings
 * (optionally carrying {@link org.bukkit.ChatColor} codes supplied by the
 * caller). The former translation keys are retained in the signatures for call
 * compatibility but resolve to their server-side fallback text.
 * </p>
 */
public final class MessageComponents {
    private MessageComponents() {}

    /**
     * Builds {@code prefix + value + suffix} as a plain string.
     *
     * @param prefixKey      legacy translation key (unused; kept for compatibility)
     * @param prefixFallback prefix text
     * @param value          the value rendered between prefix and suffix
     * @param suffixKey      legacy translation key (unused; kept for compatibility)
     * @param suffixFallback suffix text
     * @return the assembled string
     */
    public static String value(String prefixKey, String prefixFallback, Object value, String suffixKey, String suffixFallback) {
        return prefixFallback + String.valueOf(value) + suffixFallback;
    }

    /**
     * Builds {@code prefix + first + middle + second + suffix} as a plain string.
     *
     * @param prefixKey      legacy translation key (unused; kept for compatibility)
     * @param prefixFallback prefix text
     * @param firstValue     first value
     * @param middleKey      legacy translation key (unused; kept for compatibility)
     * @param middleFallback middle text
     * @param secondValue    second value
     * @param suffixKey      legacy translation key (unused; kept for compatibility)
     * @param suffixFallback suffix text
     * @return the assembled string
     */
    public static String values(
            String prefixKey,
            String prefixFallback,
            Object firstValue,
            String middleKey,
            String middleFallback,
            Object secondValue,
            String suffixKey,
            String suffixFallback
    ) {
        return prefixFallback + String.valueOf(firstValue) + middleFallback + String.valueOf(secondValue) + suffixFallback;
    }
}

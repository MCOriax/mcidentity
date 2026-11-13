package io.github.mcoriax.mcidentity.common;

/**
 * The built-in {@code player} base profession.
 *
 * <p>The base profession is what a player wears when they wear nothing else, so
 * a player is never without a profession. It is registered in memory, never
 * from a YAML file, and it cannot be removed.</p>
 *
 * <p>Its progress levels without limit from passive playtime experience: every
 * online player gains one experience point per second on the base profession,
 * whatever profession they wear, and it takes {@link #EXP_PER_LEVEL}
 * experience to level up, one level per day of play time.</p>
 */
public final class BaseProfession {

    /**
     * The profession system code of the base profession.
     */
    public static final String CODE = "player";

    /**
     * The display name of the base profession.
     */
    public static final String DISPLAY_NAME = "Player";

    /**
     * The base profession has no level cap.
     */
    public static final int MAX_LEVEL = Integer.MAX_VALUE;

    /**
     * Experience needed per level of the base profession: one level per day of
     * play time at one experience point per second.
     */
    public static final int EXP_PER_LEVEL = 86_400;

    /**
     * Stat points a player gains for every level of any profession.
     */
    public static final int STAT_POINTS_PER_LEVEL = 100;

    private BaseProfession() {
    }

    /**
     * Checks whether a profession code names the base profession.
     * @param professionCode The profession's system code; may be null.
     * @return true for {@code player}, ignoring case.
     */
    public static boolean isBaseProfession(String professionCode) {
        return CODE.equalsIgnoreCase(professionCode);
    }
}

package io.github.mcoriax.common;

import java.util.UUID;

/**
 * The built-in "player" base profession every player permanently owns.
 *
 * <p>The base profession is created automatically on a player's first join and
 * is the fallback state whenever another identity is unequipped or dropped, so
 * a player is never without a profession. Its identity row shares the player's
 * own UUID, which binds it to the player forever: it is never deleted from the
 * database and it is never converted into an identity key item.</p>
 *
 * <p>It levels without limit at exactly {@link #EXP_PER_LEVEL} EXP per level,
 * fed by the passive one-EXP-per-second gain every online player receives
 * regardless of which identity is currently equipped. It may equip skins and
 * auras but is restricted from runes and abilities.</p>
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
     * EXP required per level: 86,400 = one level per day of passive gain.
     */
    public static final int EXP_PER_LEVEL = 86_400;

    /**
     * Stat points granted per level, matching the regular leveling reward.
     */
    public static final int STAT_POINTS_PER_LEVEL = 100;

    private BaseProfession() {
    }

    /**
     * Returns the base identity UUID for a player. The base identity is keyed
     * by the player's own UUID, which is what binds it permanently to them.
     * @param playerUuid The UUID of the player.
     * @return The UUID of the player's base identity.
     */
    public static UUID identityUuid(UUID playerUuid) {
        return playerUuid;
    }

    /**
     * Checks whether an identity UUID is the given player's base identity.
     * @param playerUuid The UUID of the player.
     * @param identityUuid The UUID of the identity.
     * @return True when the identity is the player's base identity.
     */
    public static boolean isBaseIdentity(UUID playerUuid, UUID identityUuid) {
        return playerUuid != null && playerUuid.equals(identityUuid);
    }

    /**
     * Checks whether a profession system code is the base profession.
     * @param professionCode The profession system code.
     * @return True when the code names the base profession.
     */
    public static boolean isBaseProfession(String professionCode) {
        return CODE.equalsIgnoreCase(professionCode);
    }
}

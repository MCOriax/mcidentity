package io.github.mcoriax.common;

import java.nio.charset.StandardCharsets;
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
 * <p>Beyond the primary identity, players may unlock additional independent
 * "player" identities (alternative identities), indexed 1..n. Their identity
 * UUIDs are derived deterministically from the player's UUID and the index via
 * {@link #identityUuid(UUID, int)}, so they fit the same {@code VARCHAR(36)}
 * identity columns and can always be recomputed without a lookup table.</p>
 *
 * <p>Base identities level without limit from passive playtime EXP. The
 * primary identity gains one EXP per second whenever the player is online —
 * regardless of which identity is currently equipped — and levels at
 * {@link #EXP_PER_LEVEL} EXP per level. An alternative identity gains its one
 * EXP per second only while it is the currently equipped identity, and levels
 * at the faster {@link #ALTERNATIVE_EXP_PER_LEVEL}. The base profession may
 * equip skins and auras but is restricted from runes and abilities.</p>
 */
public final class BaseProfession {

    /**
     * The profession system code of the base profession.
     */
    public static final String CODE = "player";

    /**
     * The index of the primary base identity, whose UUID is the player's own.
     */
    public static final int PRIMARY_INDEX = 0;

    /**
     * The display name of the base profession.
     */
    public static final String DISPLAY_NAME = "Player";

    /**
     * The base profession has no level cap.
     */
    public static final int MAX_LEVEL = Integer.MAX_VALUE;

    /**
     * EXP required per level on the primary identity: 86,400 = one level per
     * day of passive gain.
     */
    public static final int EXP_PER_LEVEL = 86_400;

    /**
     * EXP required per level on an alternative identity: 3,600 = one level
     * per hour of passive gain while the identity is equipped.
     */
    public static final int ALTERNATIVE_EXP_PER_LEVEL = 3_600;

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
     * Returns the base identity UUID for a player at the given index. Index
     * {@link #PRIMARY_INDEX} is the primary identity and keeps the player's
     * own UUID; indexes 1..n are alternative identities whose UUIDs are
     * derived deterministically from the player UUID and the index under the
     * {@code mcidentity:sub:} namespace, so the same pair always maps to the
     * same identity row and the name can never collide with identifiers
     * produced elsewhere.
     * @param playerUuid The UUID of the player.
     * @param index The base identity index, 0 for the primary identity.
     * @return The UUID of the base identity at that index.
     */
    public static UUID identityUuid(UUID playerUuid, int index) {
        if (index <= PRIMARY_INDEX) {
            return playerUuid;
        }
        return UUID.nameUUIDFromBytes(("mcidentity:sub:" + playerUuid + ":" + index).getBytes(StandardCharsets.UTF_8));
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
     * Returns the EXP required per level for one of a player's base
     * identities. The primary identity (whose UUID equals the player's own)
     * levels at {@link #EXP_PER_LEVEL}; every alternative identity levels at
     * the faster {@link #ALTERNATIVE_EXP_PER_LEVEL}.
     * @param playerUuid The UUID of the player.
     * @param identityUuid The UUID of the base identity being leveled.
     * @return The EXP threshold per level for that identity.
     */
    public static int expPerLevel(UUID playerUuid, UUID identityUuid) {
        return isBaseIdentity(playerUuid, identityUuid) ? EXP_PER_LEVEL : ALTERNATIVE_EXP_PER_LEVEL;
    }

    /**
     * Finds which base identity index an identity UUID belongs to, checking
     * the primary identity and the first {@code unlockedSlots} alternative
     * identities.
     * @param playerUuid The UUID of the player.
     * @param identityUuid The UUID of the identity.
     * @param unlockedSlots The number of unlocked alternative identities.
     * @return The matching index (0 for primary, 1..n for alternatives), or
     *         -1 when the identity is not one of the player's base identities.
     */
    public static int identityIndex(UUID playerUuid, UUID identityUuid, int unlockedSlots) {
        if (playerUuid == null || identityUuid == null) {
            return -1;
        }
        if (playerUuid.equals(identityUuid)) {
            return PRIMARY_INDEX;
        }
        for (int index = 1; index <= unlockedSlots; index++) {
            if (identityUuid.equals(identityUuid(playerUuid, index))) {
                return index;
            }
        }
        return -1;
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

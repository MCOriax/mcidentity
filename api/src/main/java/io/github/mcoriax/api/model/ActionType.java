package io.github.mcoriax.api.model;

/**
 * Represents the type of action performed on an identity key.
 */
public enum ActionType {
    /**
     * Key was equipped by a player.
     */
    EQUIP,
    /**
     * Key was unequipped by a player.
     */
    UNEQUIP,
    /**
     * Key was dropped upon player death.
     */
    DEATH_DROP,
    /**
     * Key was removed by an administrator.
     */
    ADMIN_REMOVE
}

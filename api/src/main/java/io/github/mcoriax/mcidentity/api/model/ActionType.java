package io.github.mcoriax.mcidentity.api.model;

/**
 * The action recorded in the profession audit log.
 */
public enum ActionType {
    /**
     * A player put a profession on.
     */
    EQUIP,
    /**
     * A player took a profession off and went back to the base profession.
     */
    UNEQUIP
}

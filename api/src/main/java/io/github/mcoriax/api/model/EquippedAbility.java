package io.github.mcoriax.api.model;

/**
 * Represents an equipped ability.
 *
 * @param abilityCode The ability identifier, mapping to the MythicMobs
 *                    ability (skill) ID that executes when the ability is
 *                    triggered.
 * @param abilitySkin The skin identifier, mapping to the model ID rendered
 *                    for the ability; may be null or empty when the ability
 *                    uses the default skin.
 */
public record EquippedAbility(String abilityCode, String abilitySkin) {

    /**
     * Gets the skin to render, substituting the default skin ID when no
     * specific skin is set.
     * @return The skin ID, never null or empty.
     */
    public String abilitySkinOrDefault() {
        return abilitySkin == null || abilitySkin.isEmpty() ? Ability.DEFAULT_SKIN_ID : abilitySkin;
    }
}

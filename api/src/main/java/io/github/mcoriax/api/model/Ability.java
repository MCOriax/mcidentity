package io.github.mcoriax.api.model;

/**
 * Represents an Ability type.
 *
 * <p>Ability behaviour (activation, cooldowns, damage) is owned by MythicMobs:
 * the ability code is the MythicMobs ability (skill) ID, and the plugin only
 * stores which ability is registered and which model skin renders it.</p>
 */
public interface Ability {

    /**
     * The skin ID substituted when an ability has no specific skin set.
     */
    String DEFAULT_SKIN_ID = "default";

    /**
     * Gets the unique ability code, mapping to the MythicMobs ability ID.
     * @return The ability code.
     */
    String getAbilityCode();

    /**
     * Gets the display name.
     * @return The display name.
     */
    String getDisplayName();

    /**
     * Gets the skin identifier, mapping to the model ID rendered for the
     * ability.
     * @return The skin ID, or null when the ability uses the default skin.
     */
    String getAbilitySkin();

    /**
     * Gets the skin to render, substituting the default skin ID when no
     * specific skin is set.
     * @return The skin ID, never null or empty.
     */
    default String getAbilitySkinOrDefault() {
        String skin = getAbilitySkin();
        return skin == null || skin.isEmpty() ? DEFAULT_SKIN_ID : skin;
    }
}

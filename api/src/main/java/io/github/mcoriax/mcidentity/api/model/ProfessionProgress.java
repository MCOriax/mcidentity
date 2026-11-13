package io.github.mcoriax.mcidentity.api.model;

/**
 * How far a player has levelled one profession.
 *
 * <p>Keyed by the player's own UUID and the profession's system code. A player
 * keeps the progress of every profession they have worn, and picks it up again
 * when they wear that profession again. The base {@code player} profession also
 * gains passive playtime experience whatever the player wears.</p>
 */
public interface ProfessionProgress {

    /**
     * Gets the profession level.
     * @return The level, starting at 1.
     */
    int getLevel();

    /**
     * Gets the experience gathered towards the next level.
     * @return The experience.
     */
    int getExp();
}

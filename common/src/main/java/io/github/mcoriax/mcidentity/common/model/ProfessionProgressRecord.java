package io.github.mcoriax.mcidentity.common.model;

import io.github.mcoriax.mcidentity.api.model.ProfessionProgress;

/**
 * Immutable {@link ProfessionProgress} of one profession of one player.
 *
 * @param level The profession level, starting at 1.
 * @param exp   The experience towards the next level.
 */
public record ProfessionProgressRecord(int level, int exp) implements ProfessionProgress {

    /**
     * The progress of a profession the player has never worn.
     * @return Level 1 with no experience.
     */
    public static ProfessionProgressRecord starting() {
        return new ProfessionProgressRecord(1, 0);
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public int getExp() {
        return exp;
    }
}

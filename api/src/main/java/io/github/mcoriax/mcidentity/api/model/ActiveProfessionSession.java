package io.github.mcoriax.mcidentity.api.model;

import java.util.UUID;

/**
 * The RAM snapshot of an online player: the profession they wear, the progress
 * of that profession, their stats, and the live health, energy and food values.
 *
 * <p>Keyed by the player UUID. Immutable; every change produces a new record.</p>
 *
 * @param playerUuid     The player's own UUID.
 * @param professionCode The system code of the profession worn; {@code player} when none.
 * @param progress       The level and experience of the worn profession.
 * @param stats          The player's stats.
 * @param currentHealth  The live health value.
 * @param currentEnergy  The live energy value.
 * @param currentFood    The live food value.
 */
public record ActiveProfessionSession(
    UUID playerUuid,
    String professionCode,
    ProfessionProgress progress,
    PlayerStats stats,
    double currentHealth,
    double currentEnergy,
    double currentFood
) {

    /**
     * Returns a copy wearing another profession, with that profession's progress.
     * @param professionCode The profession worn from now on.
     * @param progress The progress of that profession.
     * @return The updated session.
     */
    public ActiveProfessionSession withProfession(String professionCode, ProfessionProgress progress) {
        return new ActiveProfessionSession(playerUuid, professionCode, progress, stats,
            currentHealth, currentEnergy, currentFood);
    }

    /**
     * Returns a copy with new progress for the worn profession.
     * @param progress The new progress.
     * @return The updated session.
     */
    public ActiveProfessionSession withProgress(ProfessionProgress progress) {
        return new ActiveProfessionSession(playerUuid, professionCode, progress, stats,
            currentHealth, currentEnergy, currentFood);
    }

    /**
     * Returns a copy with new stats.
     * @param stats The new stats.
     * @return The updated session.
     */
    public ActiveProfessionSession withStats(PlayerStats stats) {
        return new ActiveProfessionSession(playerUuid, professionCode, progress, stats,
            currentHealth, currentEnergy, currentFood);
    }

    /**
     * Returns a copy with a new live health value.
     * @param currentHealth The new health.
     * @return The updated session.
     */
    public ActiveProfessionSession withCurrentHealth(double currentHealth) {
        return new ActiveProfessionSession(playerUuid, professionCode, progress, stats,
            currentHealth, currentEnergy, currentFood);
    }

    /**
     * Returns a copy with a new live energy value.
     * @param currentEnergy The new energy.
     * @return The updated session.
     */
    public ActiveProfessionSession withCurrentEnergy(double currentEnergy) {
        return new ActiveProfessionSession(playerUuid, professionCode, progress, stats,
            currentHealth, currentEnergy, currentFood);
    }

    /**
     * Returns a copy with a new live food value.
     * @param currentFood The new food.
     * @return The updated session.
     */
    public ActiveProfessionSession withCurrentFood(double currentFood) {
        return new ActiveProfessionSession(playerUuid, professionCode, progress, stats,
            currentHealth, currentEnergy, currentFood);
    }
}

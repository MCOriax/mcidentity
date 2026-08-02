package io.github.mcoriax.mcidentity.common;

import io.github.mcoriax.mcidentity.api.db.IMCIdentityDB;
import io.github.mcoriax.mcidentity.api.model.IdentityKey;
import io.github.mcoriax.mcidentity.api.model.IdentityStats;
import io.github.mcoriax.mcidentity.common.model.MutableIdentityProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Grants players alternative base identities and registers the rows behind them.
 *
 * <p><strong>Internal.</strong> Package-private on purpose: this is the
 * implementation behind {@link MCIdentityProvider#addBaseProfession(UUID, String, int)},
 * extracted so the provider stays a thin delegating facade instead of also
 * carrying the profession-resolution and row-creation logic.</p>
 *
 * <p>Every player owns one permanent primary base identity whose UUID equals
 * their own. Beyond that they may unlock alternatives, each keyed by a
 * deterministic UUID derived from the player and a slot index (see
 * {@link BaseProfession#identityUuid(UUID, int)}). Registration is idempotent:
 * an index that already has an identity row is left untouched, so re-running a
 * grant never disturbs an existing alternative.</p>
 */
final class BaseIdentityRegistrar {

    /**
     * The backend the identity rows are created in.
     */
    private final IMCIdentityDB database;

    /**
     * Creates a registrar writing to the given backend.
     * @param database The storage backend.
     */
    BaseIdentityRegistrar(IMCIdentityDB database) {
        this.database = database;
    }

    /**
     * Adds alternative base identity slots to a player and makes sure every
     * reachable slot has an identity row behind it.
     * @param playerUuid The UUID of the player.
     * @param professionCode The profession every new alternative should take,
     *                       or {@code null}/{@code "random"} for a random
     *                       non-player profession.
     * @param amount The number of alternative identity slots to add.
     * @return A future containing the new unlocked slot count.
     */
    CompletableFuture<Integer> grant(UUID playerUuid, String professionCode, int amount) {
        if (professionCode != null && !professionCode.equalsIgnoreCase("random")
                && BaseProfession.isBaseProfession(professionCode)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                "The player profession is reserved for the primary identity and cannot be assigned to an alternative identity"));
        }
        return database.addBaseIdentitySlots(playerUuid, amount).thenCompose(total -> {
            CompletableFuture<Void> registrations = CompletableFuture.completedFuture(null);
            for (int index = 1; index <= total; index++) {
                final UUID identityUuid = BaseProfession.identityUuid(playerUuid, index);
                registrations = registrations.thenCompose(v -> ensureRegistered(identityUuid, professionCode));
            }
            return registrations.thenApply(v -> total);
        });
    }

    /**
     * Creates an alternative base identity row for the given deterministic UUID
     * when it does not exist yet, using the resolved profession's initial
     * properties and stats.
     * @param identityUuid The deterministic identity UUID.
     * @param professionCode The requested profession, or null/"random" for a
     *                       random non-player profession.
     * @return A future that completes once the identity exists.
     */
    private CompletableFuture<Void> ensureRegistered(UUID identityUuid, String professionCode) {
        return database.getIdentityKey(identityUuid).thenCompose(optKey -> {
            if (optKey.isPresent()) {
                return CompletableFuture.completedFuture(null);
            }

            final String resolvedCode;
            try {
                resolvedCode = resolveProfession(professionCode);
            } catch (RuntimeException ex) {
                return CompletableFuture.failedFuture(ex);
            }

            IdentityKey key = new IdentityKey() {
                @Override public UUID getUniqueId() { return identityUuid; }
                @Override public String getProfessionCode() { return resolvedCode; }
            };
            MutableIdentityProperties props = new MutableIdentityProperties(null, 1, 0, 0, null, null, null);

            return database.createIdentity(key, props, startingStats(resolvedCode));
        });
    }

    /**
     * Resolves the profession an alternative base identity should take. A
     * concrete code is lower-cased and returned as-is; {@code null},
     * {@code "random"}, or blank resolves to a random registered profession.
     * The {@code player} profession is never returned: it is rejected as an
     * explicit choice and excluded from the random pool, so alternatives can
     * never be the base player profession.
     * @param professionCode The requested profession, or null/"random".
     * @return The resolved concrete profession code.
     */
    private static String resolveProfession(String professionCode) {
        if (professionCode != null && !professionCode.isBlank()
                && !professionCode.equalsIgnoreCase("random")) {
            if (BaseProfession.isBaseProfession(professionCode)) {
                throw new IllegalArgumentException(
                    "The player profession cannot be assigned to an alternative identity");
            }
            return professionCode.toLowerCase();
        }
        List<String> loadedProfCodes = new ArrayList<>(MCIdentityManager.getRegisteredProfessions().keySet());
        loadedProfCodes.remove(BaseProfession.CODE);
        if (loadedProfCodes.isEmpty()) {
            throw new IllegalStateException(
                "No non-player professions are registered to assign to an alternative identity");
        }
        return loadedProfCodes.get(ThreadLocalRandom.current().nextInt(loadedProfCodes.size()));
    }

    /**
     * Builds the starting stats of a new identity from its profession's
     * configured base stats, falling back to the vanilla-ish defaults for any
     * stat the profession does not declare.
     * @param professionCode The resolved profession code.
     * @return The starting stats view.
     */
    private static IdentityStats startingStats(String professionCode) {
        Map<String, Integer> baseStats = MCIdentityManager.getProfessionBaseStats(professionCode);
        final int energyVal = baseStats.getOrDefault("energy", 100);
        final int healthVal = baseStats.getOrDefault("health", 20);
        final int foodVal = baseStats.getOrDefault("food", 20);
        final int strengthVal = baseStats.getOrDefault("strength", 0);
        final int defenseVal = baseStats.getOrDefault("defense", 0);
        final int wisdomVal = baseStats.getOrDefault("wisdom", 0);
        final int luckVal = baseStats.getOrDefault("luck", 0);
        final int agilityVal = baseStats.getOrDefault("agility", 0);
        final int resilienceVal = baseStats.getOrDefault("resilience", 0);

        return new IdentityStats() {
            @Override public int getEnergy() { return energyVal; }
            @Override public int getHealth() { return healthVal; }
            @Override public int getFood() { return foodVal; }
            @Override public int getStrength() { return strengthVal; }
            @Override public int getDefense() { return defenseVal; }
            @Override public int getWisdom() { return wisdomVal; }
            @Override public int getLuck() { return luckVal; }
            @Override public int getAgility() { return agilityVal; }
            @Override public int getResilience() { return resilienceVal; }
        };
    }
}

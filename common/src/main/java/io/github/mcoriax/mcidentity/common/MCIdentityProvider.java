package io.github.mcoriax.mcidentity.common;

import io.github.mcoriax.mcidentity.api.IdentityProvider;
import io.github.mcoriax.mcidentity.api.db.IMCIdentityDB;
import io.github.mcoriax.mcidentity.api.model.*;
import io.github.mcoriax.mcidentity.common.model.MutableIdentityProperties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Global provider for the Identity API.
 * Acts as a central hub for all identity-related database and service operations.
 */
public class MCIdentityProvider implements IdentityProvider {

    /**
     * The singleton instance of the identity provider.
     */
    public static MCIdentityProvider instance;
    /**
     * The instance of the identity database.
     */
    private final IMCIdentityDB database;

    /**
     * Initializes the identity provider with a database.
     * @param db The database implementation to use.
     */
    public MCIdentityProvider(IMCIdentityDB db) {
        this.database = Objects.requireNonNull(db, "Database cannot be null");
        instance = this;
    }

    /**
     * Gets whether the identity database has been initialized.
     * @return True if initialized.
     */
    public static boolean hasDatabase() {
        return instance != null && instance.database != null;
    }

    // --- Database Operations ---

    /**
     * Creates a new profession type.
     */
    public CompletableFuture<Void> createProfession(String systemCode, String displayName) {
        return database.createProfession(systemCode.toLowerCase(), displayName);
    }

    /**
     * Creates a new ability type.
     */
    public CompletableFuture<Void> createAbility(String abilityCode, String displayName, String abilitySkin) {
        return database.createAbility(abilityCode.toLowerCase(), displayName, abilitySkin);
    }

    /**
     * Retrieves all available professions.
     */
    public CompletableFuture<Collection<Profession>> getProfessions() {
        return database.getProfessions();
    }

    /**
     * Retrieves all available abilities.
     */
    public CompletableFuture<Collection<Ability>> getAbilities() {
        return database.getAbilities();
    }

    /**
     * Retrieves all available runes.
     */
    public CompletableFuture<Collection<Rune>> getRunes() {
        return database.getRunes();
    }

    /**
     * Creates a new identity key.
     */
    public CompletableFuture<Void> createIdentity(IdentityKey key, IdentityProperties properties, IdentityStats stats) {
        if (key.getProfessionCode() == null || key.getProfessionCode().equalsIgnoreCase("random")) {
            return getProfessions().thenCompose(professions -> {
                List<String> loadedProfCodes = new ArrayList<>(MCIdentityManager.getRegisteredProfessions().keySet());
                loadedProfCodes.remove(BaseProfession.CODE);
                if (loadedProfCodes.isEmpty()) {
                    return CompletableFuture.failedFuture(new IllegalStateException("No professions loaded in memory to resolve random identity"));
                }
                
                String pickedCode = loadedProfCodes.get(ThreadLocalRandom.current().nextInt(loadedProfCodes.size()));
                
                IdentityKey resolvedKey = new IdentityKey() {
                    @Override public UUID getUniqueId() { return key.getUniqueId(); }
                    @Override public String getProfessionCode() { return pickedCode; }
                };
                return database.createIdentity(resolvedKey, properties, stats);
            });
        }
        return database.createIdentity(key, properties, stats);
    }

    /**
     * Retrieves an identity key by its UUID.
     */
    @Override
    public CompletableFuture<Optional<IdentityKey>> getIdentityKey(UUID uuid) {
        return database.getIdentityKey(uuid);
    }

    /**
     * Retrieves properties for an identity.
     */
    @Override
    public CompletableFuture<Optional<IdentityProperties>> getProperties(UUID identityUuid) {
        return database.getProperties(identityUuid);
    }

    /**
     * Retrieves stats for an identity.
     */
    @Override
    public CompletableFuture<Optional<IdentityStats>> getStats(UUID identityUuid) {
        return database.getStats(identityUuid);
    }

    /**
     * Retrieves a specific stat for an identity.
     */
    @Override
    public CompletableFuture<Integer> getStat(UUID identityUuid, String statKey) {
        return database.getStat(identityUuid, statKey.toLowerCase());
    }

    /**
     * Retrieves equipped abilities for an identity.
     */
    @Override
    public CompletableFuture<Map<Integer, EquippedAbility>> getEquippedAbilities(UUID identityUuid) {
        return database.getEquippedAbilities(identityUuid);
    }

    /**
     * Updates an equipped ability for an identity.
     */
    public CompletableFuture<Void> setEquippedAbility(UUID identityUuid, int slot, String abilityCode, String abilitySkin) {
        String code = lower(abilityCode);
        return writeThenSyncSession(database.setEquippedAbility(identityUuid, slot, code, abilitySkin), identityUuid, session -> {
            Map<Integer, EquippedAbility> abilities = new HashMap<>(session.equippedAbilities());
            if (code == null) {
                abilities.remove(slot);
            } else {
                abilities.put(slot, new EquippedAbility(code, abilitySkin));
            }
            MCIdentityManager.updateEquippedAbilities(session.playerUuid(), abilities);
        });
    }

    /**
     * Retrieves runes for an identity.
     */
    @Override
    public CompletableFuture<Map<Integer, String>> getRunes(UUID identityUuid) {
        return database.getRunes(identityUuid);
    }

    /**
     * Updates a rune for an identity.
     */
    public CompletableFuture<Void> setRune(UUID identityUuid, int slot, String runeCode) {
        return database.setRune(identityUuid, slot, lower(runeCode));
    }

    /**
     * Gets the active identity for a player.
     */
    @Override
    public CompletableFuture<Optional<UUID>> getActiveIdentity(UUID playerUuid) {
        return database.getActiveIdentity(playerUuid);
    }

    /**
     * Gets the profession system code of the player's currently active identity
     * from the RAM session cache. Synchronous and RAM-only, so it is safe to call
     * on hot paths such as region profession whitelist/blacklist checks.
     */
    @Override
    public Optional<String> getActiveProfession(UUID playerUuid) {
        return MCIdentityManager.getSession(playerUuid)
            .map(ActiveIdentitySession::identityKey)
            .map(IdentityKey::getProfessionCode);
    }

    /**
     * Gets the full session info for a player.
     */
    public CompletableFuture<Optional<IdentitySession>> getSession(UUID playerUuid) {
        return database.getSession(playerUuid);
    }

    /**
     * Sets the active identity for a player.
     */
    public CompletableFuture<Void> setActiveIdentity(UUID playerUuid, UUID identityUuid, String contentKey) {
        return database.setActiveIdentity(playerUuid, identityUuid, contentKey).thenCompose(v -> {
            if (identityUuid == null) {
                MCIdentityManager.removeSession(playerUuid);
                return CompletableFuture.completedFuture(null);
            } else {
                return loadSessionToRam(playerUuid, identityUuid, contentKey);
            }
        });
    }

    /**
     * Updates the content key for a player's session in the database and RAM.
     * @param playerUuid The UUID of the player.
     * @param contentKey The new serialized item stack string.
     * @return A CompletableFuture that completes when updated.
     */
    public CompletableFuture<Void> updateSessionContentKey(UUID playerUuid, String contentKey) {
        return database.updateSessionContentKey(playerUuid, contentKey).thenAccept(v ->
            MCIdentityManager.getSession(playerUuid).ifPresent(session ->
                MCIdentityManager.cacheSession(session.withContentKey(contentKey))));
    }

    /**
     * Gets the number of unlocked alternative base "player" identities a
     * player owns. The primary base identity is always available and is not
     * counted here.
     * @param playerUuid The UUID of the player.
     * @return A CompletableFuture containing the unlocked slot count.
     */
    public CompletableFuture<Integer> getBaseIdentitySlots(UUID playerUuid) {
        return database.getBaseIdentitySlots(playerUuid);
    }

    /**
     * Grants a player additional alternative base identities, each assigned a
     * random registered profession. The {@code player} profession is reserved
     * for the primary identity and is never chosen for an alternative.
     * @param playerUuid The UUID of the player.
     * @param amount The number of alternative identity slots to add.
     * @return A CompletableFuture containing the new unlocked slot count.
     */
    public CompletableFuture<Integer> addBaseProfession(UUID playerUuid, int amount) {
        return addBaseProfession(playerUuid, null, amount);
    }

    /**
     * Grants a player additional alternative base identities of a specific
     * profession.
     * <p>
     * The unlocked slot count is incremented in the database, and an identity
     * row is registered for every newly reachable alternative index using its
     * deterministic UUID (see {@link BaseProfession#identityUuid(UUID, int)}),
     * so the identities exist and are equippable as soon as they are granted.
     * Indexes that already have an identity row are left untouched, which makes
     * the registration idempotent. Each newly created identity takes the given
     * {@code professionCode}; when it is {@code null} or {@code "random"} a
     * random registered profession is chosen instead. The {@code player}
     * profession (see {@link BaseProfession#CODE}) is reserved for the primary
     * identity: it is rejected when requested explicitly and is always excluded
     * from the random pool.
     * </p>
     * @param playerUuid The UUID of the player.
     * @param professionCode The profession every new alternative should take,
     *                       or {@code null}/{@code "random"} for a random
     *                       non-player profession.
     * @param amount The number of alternative identity slots to add.
     * @return A CompletableFuture containing the new unlocked slot count.
     */
    public CompletableFuture<Integer> addBaseProfession(UUID playerUuid, String professionCode, int amount) {
        if (professionCode != null && !professionCode.equalsIgnoreCase("random")
                && BaseProfession.isBaseProfession(professionCode)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                "The player profession is reserved for the primary identity and cannot be assigned to an alternative identity"));
        }
        return database.addBaseIdentitySlots(playerUuid, amount).thenCompose(total -> {
            CompletableFuture<Void> registrations = CompletableFuture.completedFuture(null);
            for (int index = 1; index <= total; index++) {
                final UUID identityUuid = BaseProfession.identityUuid(playerUuid, index);
                registrations = registrations.thenCompose(v -> ensureBaseIdentityRegistered(identityUuid, professionCode));
            }
            return registrations.thenApply(v -> total);
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
    private static String resolveAlternativeProfession(String professionCode) {
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
     * Creates an alternative base identity row for the given deterministic UUID
     * when it does not exist yet, using the resolved profession's initial
     * properties and stats.
     * @param identityUuid The deterministic identity UUID.
     * @param professionCode The requested profession, or null/"random" for a
     *                       random non-player profession.
     * @return A CompletableFuture that completes once the identity exists.
     */
    private CompletableFuture<Void> ensureBaseIdentityRegistered(UUID identityUuid, String professionCode) {
        return database.getIdentityKey(identityUuid).thenCompose(optKey -> {
            if (optKey.isPresent()) {
                return CompletableFuture.completedFuture(null);
            }

            final String resolvedCode;
            try {
                resolvedCode = resolveAlternativeProfession(professionCode);
            } catch (RuntimeException ex) {
                return CompletableFuture.failedFuture(ex);
            }

            IdentityKey key = new IdentityKey() {
                @Override public UUID getUniqueId() { return identityUuid; }
                @Override public String getProfessionCode() { return resolvedCode; }
            };
            MutableIdentityProperties props = new MutableIdentityProperties(1, 0, 0, null, null, null, null, null);

            Map<String, Integer> baseStats = MCIdentityManager.getProfessionBaseStats(resolvedCode);
            final int energyVal = baseStats.getOrDefault("energy", 100);
            final int healthVal = baseStats.getOrDefault("health", 20);
            final int foodVal = baseStats.getOrDefault("food", 20);
            final int strengthVal = baseStats.getOrDefault("strength", 0);
            final int defenseVal = baseStats.getOrDefault("defense", 0);
            final int wisdomVal = baseStats.getOrDefault("wisdom", 0);
            final int luckVal = baseStats.getOrDefault("luck", 0);
            final int agilityVal = baseStats.getOrDefault("agility", 0);
            final int resilienceVal = baseStats.getOrDefault("resilience", 0);

            IdentityStats stats = new IdentityStats() {
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

            return database.createIdentity(key, props, stats);
        });
    }

    /**
     * Logs an action for an identity.
     */
    @Override
    public CompletableFuture<Void> logAction(UUID playerUuid, UUID identityUuid, ActionType actionType) {
        return database.logAction(playerUuid, identityUuid, actionType);
    }

    /**
     * Updates properties for an identity.
     */
    @Override
    public CompletableFuture<Void> updateProperties(UUID identityUuid, IdentityProperties properties) {
        return writeThenSyncSession(database.updateProperties(identityUuid, properties), identityUuid,
            session -> MCIdentityManager.updateProperties(session.playerUuid(), properties));
    }

    /**
     * Updates stats for an identity.
     */
    public CompletableFuture<Void> updateStats(UUID identityUuid, IdentityStats stats) {
        return writeThenSyncSession(database.updateStats(identityUuid, stats), identityUuid,
            session -> MCIdentityManager.updateStats(session.playerUuid(), stats));
    }

    /**
     * Loads an identity session into RAM.
     * <p>
     * The key, properties, cosmetics, equipped abilities, and stats are
     * independent lookups, so they are fetched concurrently and combined once
     * all five complete. Nothing is cached unless the key, properties, and
     * stats all exist.
     * </p>
     * @param playerUuid The UUID of the player.
     * @param identityUuid The UUID of the identity.
     * @param contentKey The serialized identity item.
     * @return A CompletableFuture that completes when loaded.
     */
    public CompletableFuture<Void> loadSessionToRam(UUID playerUuid, UUID identityUuid, String contentKey) {
        CompletableFuture<Optional<IdentityKey>> keyFuture = getIdentityKey(identityUuid);
        CompletableFuture<Optional<IdentityProperties>> propsFuture = getProperties(identityUuid);
        CompletableFuture<Map<String, String>> cosmeticsFuture = getCosmetics(identityUuid);
        CompletableFuture<Map<Integer, EquippedAbility>> abilitiesFuture = getEquippedAbilities(identityUuid);
        CompletableFuture<Optional<IdentityStats>> statsFuture = getStats(identityUuid);

        return CompletableFuture.allOf(keyFuture, propsFuture, cosmeticsFuture, abilitiesFuture, statsFuture)
            .thenAccept(ignored -> {
                Optional<IdentityKey> optKey = keyFuture.join();
                Optional<IdentityProperties> optProps = propsFuture.join();
                Optional<IdentityStats> optStats = statsFuture.join();
                if (optKey.isEmpty() || optProps.isEmpty() || optStats.isEmpty()) return;

                IdentityProperties props = optProps.get();
                Map<String, String> cosmetics = cosmeticsFuture.join();
                props.setSkin(cosmetics.get("skin"));
                props.setAura(cosmetics.get("aura"));

                IdentityStats stats = optStats.get();
                MCIdentityManager.cacheSession(new ActiveIdentitySession(
                    playerUuid,
                    identityUuid,
                    optKey.get(),
                    props,
                    stats,
                    contentKey,
                    stats.getHealth(),
                    stats.getEnergy(),
                    stats.getFood(),
                    abilitiesFuture.join()
                ));
            });
    }

    /**
     * Adds a value to a specific stat.
     */
    @Override
    public CompletableFuture<Void> addStat(UUID identityUuid, String statKey, int amount) {
        return database.addStat(identityUuid, statKey.toLowerCase(), amount);
    }

    /**
     * Retrieves cosmetics for an identity.
     */
    public CompletableFuture<Map<String, String>> getCosmetics(UUID uuid) {
        return database.getCosmetics(uuid);
    }

    /**
     * Saves skin cosmetic for an identity.
     */
    public CompletableFuture<Void> saveSkin(UUID uuid, String skin) {
        return database.saveSkin(uuid, skin);
    }

    /**
     * Saves aura cosmetic for an identity.
     */
    public CompletableFuture<Void> saveAura(UUID uuid, String aura) {
        return database.saveAura(uuid, aura);
    }
    
    /**
     * Gets the inventory contents for an identity.
     * @param identityUuid The UUID of the identity.
     * @return A CompletableFuture containing the serialized inventory string.
     */
    public CompletableFuture<String> getIdentityInventory(UUID identityUuid) {
        return getProperties(identityUuid).thenApply(opt -> opt.map(IdentityProperties::getInventoryContents).orElse(null));
    }

    /**
     * Gets the enderchest contents for an identity.
     * @param identityUuid The UUID of the identity.
     * @return A CompletableFuture containing the serialized enderchest string.
     */
    public CompletableFuture<String> getIdentityEnderchest(UUID identityUuid) {
        return getProperties(identityUuid).thenApply(opt -> opt.map(IdentityProperties::getEnderchestContents).orElse(null));
    }

    /**
     * Updates the inventory and enderchest contents for an identity.
     * Pass null to leave a specific storage unmodified.
     * @param identityUuid The UUID of the identity.
     * @param inventoryContents The new serialized inventory string (or null to skip).
     * @param enderchestContents The new serialized enderchest string (or null to skip).
     * @return A CompletableFuture that completes when updated.
     */
    public CompletableFuture<Void> updateIdentityStorage(UUID identityUuid, String inventoryContents, String enderchestContents) {
        return getProperties(identityUuid).thenCompose(optProps -> {
            if (optProps.isPresent()) {
                MutableIdentityProperties newProps = MutableIdentityProperties.copyOf(optProps.get());
                if (inventoryContents != null) {
                    newProps.setInventoryContents(inventoryContents);
                }
                if (enderchestContents != null) {
                    newProps.setEnderchestContents(enderchestContents);
                }
                return updateProperties(identityUuid, newProps);
            }
            return CompletableFuture.completedFuture(null);
        });
    }

    /**
     * Gracefully shuts down the identity provider and its database.
     */
    public void shutdown() {
        if (database != null) {
            database.shutdown();
        }
    }

    // --- RAM State Utility Proxy Methods ---

    /**
     * Checks if a player is on swap cooldown.
     * @param playerUuid The UUID of the player.
     * @return True if on cooldown.
     */
    public boolean isOnSwapCooldown(UUID playerUuid) {
        return MCIdentityManager.isOnSwapCooldown(playerUuid);
    }

    /**
     * Sets the swap cooldown for a player.
     * @param playerUuid The UUID of the player.
     */
    public void setSwapCooldown(UUID playerUuid) {
        MCIdentityManager.setSwapCooldown(playerUuid);
    }

    /**
     * Clears the swap cooldown for a player.
     * @param playerUuid The UUID of the player.
     */
    public void clearSwapCooldown(UUID playerUuid) {
        MCIdentityManager.clearSwapCooldown(playerUuid);
    }

    /**
     * Gets the remaining swap cooldown in milliseconds.
     * @param playerUuid The UUID of the player.
     * @return Remaining milliseconds.
     */
    public long getRemainingSwapCooldownMillis(UUID playerUuid) {
        return MCIdentityManager.getRemainingSwapCooldownMillis(playerUuid);
    }

    /**
     * Formats a duration in milliseconds to a human-readable string.
     * @param millis The duration in milliseconds.
     * @return The formatted string.
     */
    public String formatTime(long millis) {
        return MCIdentityManager.formatTime(millis);
    }

    /**
     * Checks if a player has a pending equipment operation.
     * @param playerUuid The UUID of the player.
     * @return True if pending.
     */
    public boolean isEquipPending(UUID playerUuid) {
        return MCIdentityManager.isEquipPending(playerUuid);
    }

    /**
     * Sets whether a player has a pending equipment operation.
     * @param playerUuid The UUID of the player.
     * @param pending True to set as pending.
     */
    public void setEquipPending(UUID playerUuid, boolean pending) {
        MCIdentityManager.setEquipPending(playerUuid, pending);
    }

    // --- Internal helpers ---

    /**
     * Runs a database write and, once it completes, applies the given sync action to the
     * cached session sharing the identity (if one is loaded in RAM).
     * @param databaseWrite The database write future.
     * @param identityUuid The UUID of the identity whose cached session should be synced.
     * @param sync The action applied to the cached session.
     * @return A CompletableFuture that completes once the write and sync are done.
     */
    private CompletableFuture<Void> writeThenSyncSession(CompletableFuture<Void> databaseWrite, UUID identityUuid,
                                                         Consumer<ActiveIdentitySession> sync) {
        return databaseWrite.thenAccept(v -> MCIdentityManager.getSessionByIdentity(identityUuid).ifPresent(sync));
    }

    /**
     * Lower-cases a system code, tolerating null values.
     * @param value The value to lower-case.
     * @return The lower-cased value, or null if the input was null.
     */
    private static String lower(String value) {
        return value != null ? value.toLowerCase() : null;
    }
}

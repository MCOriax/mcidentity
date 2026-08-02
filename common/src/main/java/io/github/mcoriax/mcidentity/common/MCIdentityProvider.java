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
     * The singleton instance of the identity provider — the handle every
     * consumer, in this plugin and in third-party plugins, goes through.
     *
     * <p>Assigned by the constructor and never cleared. Declared
     * {@code volatile} because it is installed on the main thread during plugin
     * enable but read from asynchronous database callbacks and, on Folia, from
     * region threads.</p>
     */
    public static volatile MCIdentityProvider instance;

    /**
     * The storage backend every persistence call is delegated to.
     *
     * <p>Deliberately private: no module outside {@code common} ever holds an
     * {@link IMCIdentityDB}, so the concrete backends stay unreachable and this
     * facade is the only way to persistence.</p>
     */
    private final IMCIdentityDB database;

    /**
     * The alternative base identity granting logic, kept out of this facade.
     */
    private final BaseIdentityRegistrar baseIdentities;

    /**
     * Initializes the identity provider with a database and installs it as the
     * singleton {@link #instance}.
     * @param db The database implementation to use.
     */
    public MCIdentityProvider(IMCIdentityDB db) {
        this.database = Objects.requireNonNull(db, "Database cannot be null");
        this.baseIdentities = new BaseIdentityRegistrar(this.database);
        instance = this;
    }

    /**
     * Builds the storage backend named by {@code database.type} and installs a
     * provider around it as {@link #instance}.
     *
     * <p>This is the sanctioned way to bring storage up: the backend itself is
     * never handed out, so no caller can hold one and bypass this facade. Call
     * {@link #initializeStorage()} afterwards to create the schema.</p>
     *
     * @param type The configured database type — {@code sqlite}, {@code mysql},
     *             {@code postgresql}, {@code mongodb}, or {@code central}.
     * @param options The connection settings of that type's configuration section.
     * @return The installed provider, which is also {@link #instance}.
     * @throws IllegalArgumentException When the type is unknown.
     */
    public static MCIdentityProvider create(String type, Map<String, Object> options) {
        return new MCIdentityProvider(DatabaseFactory.create(type, options));
    }

    /**
     * Gets whether the identity database has been initialized.
     * @return True if initialized.
     */
    public static boolean hasDatabase() {
        return instance != null && instance.database != null;
    }

    // --- Storage lifecycle ---

    /**
     * Creates the storage schema, or verifies the central server is reachable.
     * @return A CompletableFuture that completes when storage is ready, and
     *         completes exceptionally when it is not so the caller can retry.
     */
    public CompletableFuture<Void> initializeStorage() {
        return database.initializeSchema();
    }

    /**
     * Reports whether storage lives behind the MCIdentity central server rather
     * than in a database this process reaches directly.
     *
     * <p>Lets the platform layer decide whether cold-boot login protection is
     * needed without naming — or importing — a concrete backend.</p>
     * @return True when the central server backs this provider.
     */
    public boolean isRemoteStorage() {
        return DatabaseFactory.isRemote(database);
    }

    /**
     * Probes whether storage is reachable, waking a cold-started central server
     * deployment in the process.
     *
     * <p>The returned future always completes: it resolves to {@code false} on
     * any failure rather than completing exceptionally, and resolves to
     * {@code true} immediately for a local backend, which has nothing to wake.</p>
     * @return A future resolving to whether storage answered.
     */
    public CompletableFuture<Boolean> pingStorage() {
        return DatabaseFactory.ping(database);
    }

    // --- Database Operations ---

    /**
     * Creates a new profession type.
     */
    public CompletableFuture<Void> createProfession(String systemCode, String displayName) {
        return database.createProfession(systemCode.toLowerCase(), displayName);
    }

    /**
     * Retrieves all available professions.
     */
    public CompletableFuture<Collection<Profession>> getProfessions() {
        return database.getProfessions();
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
     * Resolves the identity currently holding an alias.
     * <p>
     * Aliases are unique across every identity and are matched without regard
     * to case, so any casing of a claimed alias resolves to the same identity.
     * </p>
     * @param alias The alias to resolve.
     * @return A CompletableFuture containing the identity UUID, or empty when
     *         the alias is unclaimed or malformed.
     */
    @Override
    public CompletableFuture<Optional<UUID>> getIdentityByAlias(String alias) {
        return database.getIdentityByAlias(alias);
    }

    /**
     * Claims an alias for an identity.
     * <p>
     * The alias must contain only alphanumeric characters and must not already
     * be held by another identity, compared without regard to case. It is
     * stored with the casing supplied here. On success the cached RAM session
     * for the identity, if any, is refreshed so it reports the new alias
     * immediately.
     * </p>
     * @param identityUuid The UUID of the identity.
     * @param alias The alias to claim, or null to release the current one.
     * @return A CompletableFuture containing true when the alias was applied,
     *         and false when it is malformed or already taken.
     */
    @Override
    public CompletableFuture<Boolean> setIdentityAlias(UUID identityUuid, String alias) {
        if (alias != null && !IdentityAlias.isValid(alias)) {
            return CompletableFuture.completedFuture(false);
        }
        return database.setAlias(identityUuid, alias).thenApply(applied -> {
            if (applied) {
                MCIdentityManager.getSessionByIdentity(identityUuid).ifPresent(session ->
                    MCIdentityManager.updateProperties(session.playerUuid(),
                        MutableIdentityProperties.withAlias(session.properties(), alias)));
            }
            return applied;
        });
    }

    /**
     * Retrieves a specific stat for an identity.
     */
    @Override
    public CompletableFuture<Integer> getStat(UUID identityUuid, String statKey) {
        return database.getStat(identityUuid, statKey.toLowerCase());
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
        return baseIdentities.grant(playerUuid, professionCode, amount);
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
     * The key, properties, and stats are independent lookups, so they are
     * fetched concurrently and combined once all three complete.
     * Nothing is cached unless the key, properties, and stats all exist.
     * </p>
     * @param playerUuid The UUID of the player.
     * @param identityUuid The UUID of the identity.
     * @param contentKey The serialized identity item.
     * @return A CompletableFuture that completes when loaded.
     */
    public CompletableFuture<Void> loadSessionToRam(UUID playerUuid, UUID identityUuid, String contentKey) {
        CompletableFuture<Optional<IdentityKey>> keyFuture = getIdentityKey(identityUuid);
        CompletableFuture<Optional<IdentityProperties>> propsFuture = getProperties(identityUuid);
        CompletableFuture<Optional<IdentityStats>> statsFuture = getStats(identityUuid);

        return CompletableFuture.allOf(keyFuture, propsFuture, statsFuture)
            .thenAccept(ignored -> {
                Optional<IdentityKey> optKey = keyFuture.join();
                Optional<IdentityProperties> optProps = propsFuture.join();
                Optional<IdentityStats> optStats = statsFuture.join();
                if (optKey.isEmpty() || optProps.isEmpty() || optStats.isEmpty()) return;

                IdentityProperties props = optProps.get();

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
                    stats.getFood()
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

}

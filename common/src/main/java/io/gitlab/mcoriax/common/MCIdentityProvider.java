package io.gitlab.mcoriax.common;

import io.gitlab.mcoriax.api.IdentityProvider;
import io.gitlab.mcoriax.api.db.IMCIdentityDB;
import io.gitlab.mcoriax.api.model.*;
import io.gitlab.mcoriax.api.model.ActiveIdentitySession;
import io.gitlab.mcoriax.common.MCIdentityManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

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
     * Initializes the database schema.
     */
    public CompletableFuture<Void> initializeSchema() {
        return database.initializeSchema();
    }

    /**
     * Creates a new profession type.
     */
    public CompletableFuture<Void> createProfession(String systemCode, String displayName) {
        return database.createProfession(systemCode.toLowerCase(), displayName);
    }

    /**
     * Creates a new ability type.
     */
    public CompletableFuture<Void> createAbility(String systemCode, String displayName, AbilityType type, boolean isGlobal) {
        return database.createAbility(systemCode.toLowerCase(), displayName, type, isGlobal);
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
    public CompletableFuture<Void> setEquippedAbility(UUID identityUuid, int slot, String abilityCode, String modelId) {
        return database.setEquippedAbility(identityUuid, slot, abilityCode != null ? abilityCode.toLowerCase() : null, modelId)
            .thenAccept(v -> {
                MCIdentityManager.getSessionByIdentity(identityUuid).ifPresent(session -> {
                    Map<Integer, EquippedAbility> abilities = new java.util.HashMap<>(session.equippedAbilities());
                    if (abilityCode == null) {
                        abilities.remove(slot);
                    } else {
                        abilities.put(slot, new EquippedAbility(abilityCode.toLowerCase(), modelId));
                    }
                    MCIdentityManager.updateEquippedAbilities(session.playerUuid(), abilities);
                });
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
        return database.setRune(identityUuid, slot, runeCode != null ? runeCode.toLowerCase() : null);
    }

    /**
     * Gets the active identity for a player.
     */
    @Override
    public CompletableFuture<Optional<UUID>> getActiveIdentity(UUID playerUuid) {
        return database.getActiveIdentity(playerUuid);
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
        return database.updateSessionContentKey(playerUuid, contentKey).thenAccept(v -> {
            MCIdentityManager.getSession(playerUuid).ifPresent(session -> {
                MCIdentityManager.cacheSession(new ActiveIdentitySession(
                    session.playerUuid(),
                    session.identityUuid(),
                    session.identityKey(),
                    session.properties(),
                    session.stats(),
                    contentKey,
                    session.currentHealth(),
                    session.currentEnergy(),
                    session.currentFood(),
                    session.equippedAbilities()
                ));
            });
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
        return database.updateProperties(identityUuid, properties).thenAccept(v -> {
            MCIdentityManager.getSessionByIdentity(identityUuid).ifPresent(session -> {
                MCIdentityManager.updateProperties(session.playerUuid(), properties);
            });
        });
    }

    /**
     * Updates stats for an identity.
     */
    public CompletableFuture<Void> updateStats(UUID identityUuid, IdentityStats stats) {
        return database.updateStats(identityUuid, stats).thenAccept(v -> {
            MCIdentityManager.getSessionByIdentity(identityUuid).ifPresent(session -> {
                MCIdentityManager.updateStats(session.playerUuid(), stats);
            });
        });
    }

    /**
     * Loads an identity session into RAM.
     * @param playerUuid The UUID of the player.
     * @param identityUuid The UUID of the identity.
     * @param contentKey The serialized identity item.
     * @return A CompletableFuture that completes when loaded.
     */
    public CompletableFuture<Void> loadSessionToRam(UUID playerUuid, UUID identityUuid, String contentKey) {
        return getIdentityKey(identityUuid).thenCompose(optKey -> {
            if (optKey.isEmpty()) return CompletableFuture.completedFuture(null);
            IdentityKey key = optKey.get();

            return getProperties(identityUuid).thenCompose(optProps -> {
                if (optProps.isEmpty()) return CompletableFuture.completedFuture(null);
                IdentityProperties props = optProps.get();

                return getCosmetics(identityUuid).thenCompose(cosmetics -> {
                    props.setSkin(cosmetics.get("skin"));
                    props.setAura(cosmetics.get("aura"));

                    return getEquippedAbilities(identityUuid).thenCompose(equippedAbilities -> {
                        return getStats(identityUuid).thenAccept(optStats -> {
                            if (optStats.isPresent()) {
                                IdentityStats stats = optStats.get();
                                MCIdentityManager.cacheSession(new ActiveIdentitySession(
                                    playerUuid,
                                    identityUuid,
                                    key,
                                    props,
                                    stats,
                                    contentKey,
                                    stats.getHealth(),
                                    stats.getEnergy(),
                                    stats.getFood(),
                                    equippedAbilities
                                ));
                            }
                        });
                    });
                });
            });
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
                IdentityProperties props = optProps.get();
                IdentityProperties newProps = new IdentityProperties() {
                    private String skin = props.getSkin();
                    private String aura = props.getAura();
                    private String inv = inventoryContents != null ? inventoryContents : props.getInventoryContents();
                    private String ec = enderchestContents != null ? enderchestContents : props.getEnderchestContents();
                    
                    @Override public int getLevel() { return props.getLevel(); }
                    @Override public int getExp() { return props.getExp(); }
                    @Override public int getStatPoints() { return props.getStatPoints(); }
                    @Override public String getHeadText() { return props.getHeadText(); }
                    @Override public String getSkin() { return skin; }
                    @Override public void setSkin(String skin) { this.skin = skin; }
                    @Override public String getAura() { return aura; }
                    @Override public void setAura(String aura) { this.aura = aura; }
                    @Override public String getInventoryContents() { return inv; }
                    @Override public void setInventoryContents(String inventoryContents) { this.inv = inventoryContents; }
                    @Override public String getEnderchestContents() { return ec; }
                    @Override public void setEnderchestContents(String enderchestContents) { this.ec = enderchestContents; }
                };
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
}

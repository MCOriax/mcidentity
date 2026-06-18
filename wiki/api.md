# Developer API Usage

[← Back to README](../README.md)

Third-party developers can integrate with the identity system using the `io.gitlab.mcoriax.common.MCIdentityProvider.java` class. Below are the available functions and their usage.

> Looking for events? See [Bukkit Events & Integration](bukkit.md).

## Identity Provider Functions

<details>
<summary><code>createProfession(String systemCode, String displayName)</code></summary>

- **Description**: Creates a new profession type in the database.
- **Parameters**:
    - `systemCode`: Unique identifier for the profession.
    - `displayName`: Human-readable name.
- **Return Type**: `CompletableFuture<Void>`
</details>

<details>
<summary><code>createAbility(String systemCode, String displayName, AbilityType type, boolean isGlobal)</code></summary>

- **Description**: Creates a new ability type.
- **Parameters**:
    - `systemCode`: Unique identifier for the ability.
    - `displayName`: Human-readable name.
    - `type`: `ACTIVE` or `PASSIVE`.
    - `isGlobal`: Whether the ability is available globally.
- **Return Type**: `CompletableFuture<Void>`
</details>

<details>
<summary><code>getProfessions()</code></summary>

- **Description**: Retrieves all available professions.
- **Return Type**: `CompletableFuture<Collection<Profession>>`
</details>

<details>
<summary><code>getAbilities()</code></summary>

- **Description**: Retrieves all available abilities.
- **Return Type**: `CompletableFuture<Collection<Ability>>`
</details>

<details>
<summary><code>getRunes()</code></summary>

- **Description**: Retrieves all available runes.
- **Return Type**: `CompletableFuture<Collection<Rune>>`
</details>

<details>
<summary><code>createIdentity(IdentityKey key, IdentityProperties properties, IdentityStats stats)</code></summary>

- **Description**: Creates a new identity record with initial properties and stats.
- **Parameters**:
    - `key`: The identity key (UUID, Profession).
    - `properties`: Level, Exp, etc.
    - `stats`: Health, Energy, Strength, etc.
- **Return Type**: `CompletableFuture<Void>`
</details>

<details>
<summary><code>getIdentityKey(UUID uuid)</code></summary>

- **Description**: Retrieves an identity key by its unique UUID.
- **Parameters**:
    - `uuid`: The UUID of the identity.
- **Return Type**: `CompletableFuture<Optional<IdentityKey>>`
</details>

<details>
<summary><code>getProperties(UUID identityUuid)</code></summary>

- **Description**: Retrieves current properties (Level, Exp, etc.) for an identity.
- **Parameters**:
    - `identityUuid`: The UUID of the identity.
- **Return Type**: `CompletableFuture<Optional<IdentityProperties>>`
</details>

<details>
<summary><code>getStats(UUID identityUuid)</code></summary>

- **Description**: Retrieves current stats (Health, Energy, etc.) for an identity.
- **Parameters**:
    - `identityUuid`: The UUID of the identity.
- **Return Type**: `CompletableFuture<Optional<IdentityStats>>`
</details>

<details>
<summary><code>getStat(UUID identityUuid, String statKey)</code></summary>

- **Description**: Retrieves the value of a specific stat.
- **Parameters**:
    - `identityUuid`: The UUID of the identity.
    - `statKey`: The internal code of the stat (e.g., "strength").
- **Return Type**: `CompletableFuture<Integer>`
</details>

<details>
<summary><code>getEquippedAbilities(UUID identityUuid)</code></summary>

- **Description**: Retrieves a map of equipped abilities.
- **Parameters**:
    - `identityUuid`: The UUID of the identity.
- **Return Type**: `CompletableFuture<Map<Integer, String>>` (Slot number -> Ability Code)
</details>

<details>
<summary><code>setEquippedAbility(UUID identityUuid, int slot, String abilityCode)</code></summary>

- **Description**: Equips or clears an ability in a specific slot.
- **Parameters**:
    - `identityUuid`: The UUID of the identity.
    - `slot`: The slot number.
    - `abilityCode`: The ability code to equip (null to clear).
- **Return Type**: `CompletableFuture<Void>`
</details>

<details>
<summary><code>getRunes(UUID identityUuid)</code></summary>

- **Description**: Retrieves a map of equipped runes.
- **Parameters**:
    - `identityUuid`: The UUID of the identity.
- **Return Type**: `CompletableFuture<Map<Integer, String>>` (Slot number -> Rune Code)
</details>

<details>
<summary><code>setRune(UUID identityUuid, int slot, String runeCode)</code></summary>

- **Description**: Sets or clears a rune in a specific slot.
- **Parameters**:
    - `identityUuid`: The UUID of the identity.
    - `slot`: The slot number.
    - `runeCode`: The rune code to set (null to clear).
- **Return Type**: `CompletableFuture<Void>`
</details>

<details>
<summary><code>getActiveIdentity(UUID playerUuid)</code></summary>

- **Description**: Gets the UUID of the identity currently equipped by a player.
- **Parameters**:
    - `playerUuid`: The UUID of the player.
- **Return Type**: `CompletableFuture<Optional<UUID>>`
</details>

<details>
<summary><code>getSession(UUID playerUuid)</code></summary>

- **Description**: Retrieves full session info for a player (Identity UUID and serialized item content).
- **Parameters**:
    - `playerUuid`: The UUID of the player.
- **Return Type**: `CompletableFuture<Optional<IdentitySession>>`
</details>

<details>
<summary><code>setActiveIdentity(UUID playerUuid, UUID identityUuid, String contentKey)</code></summary>

- **Description**: Sets a player's active identity and manages session caching.
- **Parameters**:
    - `playerUuid`: The UUID of the player.
    - `identityUuid`: The identity to equip (null to unequip).
    - `contentKey`: Base64 serialized item stack of the identity key.
- **Return Type**: `CompletableFuture<Void>`
</details>

<details>
<summary><code>logAction(UUID playerUuid, UUID identityUuid, ActionType actionType)</code></summary>

- **Description**: Logs an identity-related action (EQUIP, UNEQUIP, etc.) to the database.
- **Parameters**:
    - `playerUuid`: The actor's UUID.
    - `identityUuid`: The identity's UUID.
    - `actionType`: The type of action performed.
- **Return Type**: `CompletableFuture<Void>`
</details>

<details>
<summary><code>updateProperties(UUID identityUuid, IdentityProperties properties)</code></summary>

- **Description**: Persists updated properties to the database and refreshes the RAM cache if active.
- **Parameters**:
    - `identityUuid`: The identity to update.
    - `properties`: The new property values.
- **Return Type**: `CompletableFuture<Void>`
</details>

<details>
<summary><code>updateStats(UUID identityUuid, IdentityStats stats)</code></summary>

- **Description**: Persists updated stats to the database and refreshes the RAM cache if active.
- **Parameters**:
    - `identityUuid`: The identity to update.
    - `stats`: The new stat values.
- **Return Type**: `CompletableFuture<Void>`
</details>

<details>
<summary><code>addStat(UUID identityUuid, String statKey, int amount)</code></summary>

- **Description**: Atomically increments a specific stat in the database.
- **Parameters**:
    - `identityUuid`: The identity to update.
    - `statKey`: The stat code (e.g., "defense").
    - `amount`: The amount to add (can be negative).
- **Return Type**: `CompletableFuture<Void>`
</details>

<details>
<summary><code>getCosmetics(UUID uuid)</code></summary>

- **Description**: Retrieves cosmetics (skin, aura) for an identity.
- **Parameters**:
    - `uuid`: The UUID of the identity.
- **Return Type**: `CompletableFuture<Map<String, String>>`
</details>

<details>
<summary><code>saveSkin(UUID uuid, String skin)</code></summary>

- **Description**: Saves the skin cosmetic for an identity.
- **Parameters**:
    - `uuid`: The UUID of the identity.
    - `skin`: Base64 skin texture.
- **Return Type**: `CompletableFuture<Void>`
</details>

<details>
<summary><code>saveAura(UUID uuid, String aura)</code></summary>

- **Description**: Saves the aura cosmetic for an identity.
- **Parameters**:
    - `uuid`: The UUID of the identity.
    - `aura`: Aura system code.
- **Return Type**: `CompletableFuture<Void>`
</details>

<details>
<summary><code>isOnSwapCooldown(UUID playerUuid)</code></summary>

- **Description**: Checks if a player is currently on identity swap cooldown (RAM-only).
- **Parameters**:
    - `playerUuid`: The UUID of the player.
- **Return Type**: `boolean`
</details>

<details>
<summary><code>setSwapCooldown(UUID playerUuid)</code></summary>

- **Description**: Starts the identity swap cooldown for a player (RAM-only).
- **Parameters**:
    - `playerUuid`: The UUID of the player.
- **Return Type**: `void`
</details>

<details>
<summary><code>clearSwapCooldown(UUID playerUuid)</code></summary>

- **Description**: Immediately clears the identity swap cooldown for a player (RAM-only).
- **Parameters**:
    - `playerUuid`: The UUID of the player.
- **Return Type**: `void`
</details>

<details>
<summary><code>getRemainingSwapCooldownMillis(UUID playerUuid)</code></summary>

- **Description**: Gets the remaining cooldown time in milliseconds (RAM-only).
- **Parameters**:
    - `playerUuid`: The UUID of the player.
- **Return Type**: `long`
</details>

<details>
<summary><code>formatTime(long millis)</code></summary>

- **Description**: Formats milliseconds into a human-readable string (e.g., "2m 30s").
- **Parameters**:
    - `millis`: Duration in milliseconds.
- **Return Type**: `String`
</details>

<details>
<summary><code>isEquipPending(UUID playerUuid)</code></summary>

- **Description**: Checks if a player has a pending identity equipment operation (RAM-only).
- **Parameters**:
    - `playerUuid`: The UUID of the player.
- **Return Type**: `boolean`
</details>

<details>
<summary><code>setEquipPending(UUID playerUuid, boolean pending)</code></summary>

- **Description**: Sets the pending equipment state for a player (RAM-only).
- **Parameters**:
    - `playerUuid`: The UUID of the player.
    - `pending`: True to set as pending, false to clear.
- **Return Type**: `void`
</details>

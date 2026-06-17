# MCOriax Identity Plugin

A robust and high-performance identity management system designed for Minecraft servers, with native support for the **Folia** regionalized multi-threading architecture.

## 🌟 Overview

The MCOriax Identity Plugin provides a complex "Identity" system where players can equip and manage different personas (Identities). Each identity is characterized by its **Profession**, and **Stats**. 

This system is built with performance and security as top priorities, utilizing asynchronous database operations and RAM caching to ensure a lag-free experience on high-population servers.

### Custom Profession EXP Gain
Server owners can customize how professions gain experience by defining action-based EXP configurations in YAML files within the `professions/` folder.

#### Creating a Custom Profession
1. Create a new YAML file in the `professions/` directory (e.g., `professions/warrior.yml`).
2. Define the display name and custom EXP gain rules.

#### Profession Configuration
New professions are created by adding a `.yml` file to the `professions/` folder. The plugin will automatically register these into the database upon the first execution.

**Key Requirements:**
- `system_code`: Must be unique and provided in **lowercase**.
- `display_name`: Human-readable name for the profession.

#### EXP Gain Syntax
You can define EXP gain for specific actions using several formats:

<details>
<summary>Simple Value</summary>

Award a specific amount or random range of EXP.
```yaml
receive_damage: 10 # Fixed
deal_damage: 10~20 # Random Range
```
</details>

<details>
<summary>Condition-Based (Numerical)</summary>

Award EXP based on numerical thresholds (e.g., damage amount). Supports range keys (e.g., `min-max` or `min~max`).
```yaml
deal_damage:
  "1-10": 5       # 1 to 10 damage awards 5 EXP
  "11~50": 10~20  # 11 to 50 damage awards random 10-20 EXP
  "51-100": 50    # 51 to 100 damage awards 50 EXP
```
</details>

<details>
<summary>Condition-Based (String)</summary>

Award EXP based on specific targets (e.g., mob types).
```yaml
kill_mob:
  zombie: 20      # Killing a zombie awards 20 EXP
  skeleton: 25    # Killing a skeleton awards 25 EXP
  ender_dragon: 1000~2000 # Boss kill awards random large EXP
```
</details>

<details>
<summary>Example Configuration (<code>professions/tanker.yml</code>)</summary>

```yaml
system_code: tanker
display_name: "Tanker"

level:
  max: 100
  exp_up: 10000

stats_random: "100~120"
stats:
  energy: 100
  health: 250
  strength: 0
  defense: 0
  wisdom: 0
  luck: 0
  agility: 0
  resilience: 0

receive_damage:
  "1-10": 5~10
  "11-50": 20~40
  "51-100": 60~100
  "101-1000": 150~300

skin:
  1: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTEzNWExMzUwMjc4MDNiZGMzMmUyNDFjZmNkOTNhZWJjNzNjZTBhZWViZjEzY2ZlODI2ZDk2NGY2ZTcyZTQ3In19fQ=="
  2: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjE3OTJlYWQ4MzE0OWQ3MTI5YjBmNmFhZGQ4OWMxMmUwZTc3OWNmZmY0YWFiMTI2YTZhMzhiY2FjZDQ2YTljNiJ9fX0="
  3: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODMxMGZlMjRlNDUyZDI3MTI5ZGQzMjFkYTFjZTk2MWNhODkxMDMwMTlmYzQ2N2JlNzZjMGZiZjg1YzEyMTc2OSJ9fX0="
  4: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ4NmUxMjNhMGEzODAzYmQ0ZDYyOTQ1MGZmMDY2YjUwOTAxOGZmMWZlN2IzZGI2MjA0N2VjYTcyOTc3MTk1MCJ9fX0="
  5: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjI0NzgxNGJmNzg4Mzg1YmUwOGU2YzRmMTg1MjRiMTVmZGYxNTNmM2MzMzg5ZDcwZDA2ZDc3ODU4MWRkZDIwOCJ9fX0="
  6: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGNjY2E0ZjAyMjdiYTJkMjcyMDcwMTM4MmQ2MzZmOGZkZDkzOTEzMWQ1YjU2YTVlNzNhNmZkYmIyNGZkOThhYSJ9fX0="
  7: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjQyMmMwMmZlMThkMjllZDJlMzU1NGZlYmFiN2VmNGUzZjE3Yzg1YTZjYzM3OWFlYzVkODlhZWMyMzNiNzhjYiJ9fX0="
  8: "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTljNTg4YThiNjJhZmZkNzQ2NDNlMGQ2NjQxYmRhM2EwNzg0YTMwNGM1NzljMDY3ZmE0OTVlMmM0M2VjOTQ2MiJ9fX0="
```

</details>

#### Profession Configuration Keys
- **`stats:`**: Defines the starting attributes for the profession.
  - `energy`: Maximum energy.
  - `health`: Maximum health.
  - `food`: Maximum food capacity. Health regeneration consumes **10 food points per 1% HP regenerated**. Regeneration halts when the identity's current food drops below 10% of this capacity.
  - `strength`, `defense`, `wisdom`, `luck`, `agility`, `resilience`: Primary attributes.
- **`stats_random:`**: Defines the total amount of bonus points that will be randomly distributed across the 6 primary attributes (strength, defense, wisdom, luck, agility, resilience) when a new identity is created.
- **`skin:`**: Defines available Base64 skin textures for this profession. When a new identity of this profession is created, a skin will be chosen randomly from this list.

Supported action keys include:
- `receive_damage`: Triggered when the player takes damage.
- `deal_damage`: Triggered when the player damages another entity.
- `kill_mob`: Triggered when the player kills an entity.

### Key Features
- **Interactive GUI System**: Multi-tier virtual menus for managing identities, runes, and abilities.
- **Dynamic Data Binding**: Ability and Rune GUIs fetch real-time data from the database, showing equipped items or placeholders.
- **Regionalized Threading**: Fully compatible with Folia's `RegionScheduler` and `GlobalRegionScheduler`.
- **Database Connection Pooling**: High-performance data access using **HikariCP**.
- **Dual Database Support**: Supports both **SQLite** (for easy setup) and **PostgreSQL** (for production environments).
- **Extensible API**: Provides a central `MCIdentityProvider` for third-party plugins to interact with the identity system.
- **Secure Commands**: Role-based permissions for administrative actions.
- **Visual Identity Keys**: Identities are represented as physical items in the game with custom colored display names.
- **Unbound Identities**: Identity items created via `/identity get` are initially "unbound". They lack a unique ID and database entry. Both the unique ID and the identity's initial stats are generated and registered in the database only when the item is first used.

---

## Database Schema

The plugin maintains several tables to store identity data.

### Main Tables
- **`identity_keys`**: Primary registry of identities and their professions.
- **`identity_properties`**: Stores level, experience, and base head texture.
- **`identity_stats`**: Stores all numerical attributes (energy, health, strength, etc.).
- **`identity_cosmetics`**: Stores active **skin** (Base64) and **aura** (system code) for each identity.
- **`identity_equipped_abilities`**: Maps equipped abilities to specific slots.
- **`identity_equipped_runes`**: Maps equipped runes to specific slots.
- **`identity_sessions`**: Tracks which identity is currently equipped by which player.

### Archive Tables
Archived versions of all main tables (prefixed with `archived_`) are used to store data when an identity is deleted or modified significantly, ensuring data recovery is possible.

---

## 🛠 Developer API Usage

Third-party developers can integrate with the identity system using the `io.gitlab.mcoriax.common.MCIdentityProvider.java` class. Below are the available functions and their usage.

### Identity Provider Functions

<details>
<summary><code>initializeSchema()</code></summary>

- **Description**: Initializes the database schema (creates necessary tables).
- **Return Type**: `CompletableFuture<Void>`
</details>

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

### Custom Bukkit Events

The plugin provides several custom events located in the `io.gitlab.mcoriax.bukkit.event` package. All events extend `MCIdentityEvent` and support Folia's asynchronous context.

<details>
<summary><code>MCIdentityEvent</code></summary>

- **Description**: The abstract base class for all identity events.
- **Support**: Automatically handles asynchronous state based on `Bukkit.isPrimaryThread()`.

```java
public abstract class MCIdentityEvent extends Event {
    protected MCIdentityEvent() {
        super(!Bukkit.isPrimaryThread());
    }
}
```
</details>

<details>
<summary><code>IdentityCreationEvent</code></summary>

- **Description**: Triggered when a new identity record is created in the database.
- **Data**: `IdentityKey`, `IdentityProperties`, `IdentityStats`.

```java
public class IdentityCreationEvent extends MCIdentityEvent {
    // Getters for IdentityKey, IdentityProperties, IdentityStats
}
```
</details>

<details>
<summary><code>IdentityEquipEvent</code></summary>

- **Description**: Triggered when a player equips an identity.
- **Data**: `Player`, `UUID (identity)`, `ItemStack`.
- **Cancellable**: Yes.

```java
public class IdentityEquipEvent extends MCIdentityEvent implements Cancellable {
    // Getters for Player, UUID, ItemStack
}
```
</details>

<details>
<summary><code>IdentityUnequipEvent</code></summary>

- **Description**: Triggered when a player unequips an identity.
- **Data**: `Player`, `UUID (identity)`.
- **Cancellable**: Yes.

```java
public class IdentityUnequipEvent extends MCIdentityEvent implements Cancellable {
    // Getters for Player, UUID
}
```
</details>

<details>
<summary><code>IdentityDeathEvent</code></summary>

- **Description**: Triggered when a player with an active identity dies and drops their identity key.
- **Data**: `Player`, `UUID (identity)`, `ItemStack (dropped item)`, `boolean (drop)`.

```java
public class IdentityDeathEvent extends MCIdentityEvent {
    // Getters for Player, UUID, ItemStack
    // isDrop() and setDrop(boolean) to control dropping behavior
}
```
</details>

<details>
<summary><code>IdentityRuneSetEvent</code></summary>

- **Description**: Triggered when an identity's rune is set or cleared.
- **Data**: `Player`, `UUID (identity)`, `int (slot)`, `String (rune code)`.
- **Cancellable**: Yes.

```java
public class IdentityRuneSetEvent extends MCIdentityEvent implements Cancellable {
    // Getters for Player, UUID, Slot, RuneCode
}
```
</details>

<details>
<summary><code>IdentityAbilitySetEvent</code></summary>

- **Description**: Triggered when an identity's ability is set or cleared.
- **Data**: `Player`, `UUID (identity)`, `int (slot)`, `String (ability code)`.
- **Cancellable**: Yes.

```java
public class IdentityAbilitySetEvent extends MCIdentityEvent implements Cancellable {
    // Getters for Player, UUID, Slot, AbilityCode
}
```
</details>

<details>
<summary><code>IdentitySkinSetEvent</code></summary>

- **Description**: Triggered when an identity's skin cosmetic is set or cleared.
- **Data**: `Player`, `UUID (identity)`, `String (skin)`.
- **Cancellable**: Yes.

```java
public class IdentitySkinSetEvent extends MCIdentityEvent implements Cancellable {
    // Getters for Player, UUID, Skin
}
```
</details>

<details>
<summary><code>IdentityAuraSetEvent</code></summary>

- **Description**: Triggered when an identity's aura cosmetic is set or cleared.
- **Data**: `Player`, `UUID (identity)`, `String (aura)`.
- **Cancellable**: Yes.

```java
public class IdentityAuraSetEvent extends MCIdentityEvent implements Cancellable {
    // Getters for Player, UUID, Aura
}
```
</details>

<details>
<summary><code>IdentityStatAddEvent</code></summary>

- **Description**: Triggered when a specific stat is added/incremented for an identity.
- **Data**: `Player (nullable)`, `UUID (identity)`, `String (stat key)`, `int (amount)`.
- **Cancellable**: Yes.

```java
public class IdentityStatAddEvent extends MCIdentityEvent implements Cancellable {
    // Getters for Player, UUID, StatKey, Amount
}
```
</details>

---

## 🎮 GUI System

The plugin features a robust, multi-tier GUI system for a seamless player experience.

### Main Identity GUI (1-Row)
Accessed via `/identity` without arguments. It provides:
- **Slot 2**: Opens the **Ability GUI**.
- **Slot 4**: Interactive slot to equip/unequip your **Identity Key**.
- **Slot 6**: Opens the **Rune GUI**.
- **Slot 8**: Opens the **Cosmetic GUI**.

### Cosmetic GUI (1-Row)
Provides a menu to manage visual aspects of the identity:
- **Slot 4**: Displays a **Skin** placeholder.
- **Slot 7**: Displays an **Aura** placeholder.
- **Security**: This menu is only accessible if the player has an active identity equipped.

### Ability & Rune GUIs (6-Rows)
These GUIs are dynamically bound to the database:
- **Fetching**: Upon opening, the plugin fetches the currently equipped abilities/runes for the active identity.
- **Placeholders**: Empty slots are shown as **Red Stained Glass Panes**.
- **Equipped Items**: Equipped abilities/runes are shown as **Lime Stained Glass Panes** with their respective display names.
- **Security**: These menus are only accessible if the player has an active identity equipped.

---

## 🧪 Testing

The plugin includes a suite of unit tests for the `common` module to ensure data integrity and logic correctness.

### Running Tests
To run the unit tests, use the following Gradle command:
```bash
./gradlew :common:test
```
The tests cover:
- Core Managers (`MCIdentityManager`, `MCIdentityProvider`)
- Database Providers (SQLite, PostgreSQL, MongoDB)
- Data serialization and session management logic.

---

## 🚀 Installation & Setup

1. Place the plugin JAR in your server's `plugins/` folder.
2. Start the server once to generate the default configuration.
3. Configure your database type in `config.yml`:
   ```yaml
   database:
     type: "sqlite" # or "postgresql"
     sqlite:
       file: "mcidentity.db"
     postgresql:
       host: "localhost"
       port: 5432
       database: "mcidentity"
       username: "postgres"
       password: "password"
   ```
4. Restart the server.

## 📜 Commands

- `/identity`: Open the main Identity GUI (1-row compact menu).
- `/identity help`: Show the help message with all available commands.
- `/identity ability get <system code>`: Admin tool to get an ability item. You can then give this to a player.
- `/identity rune get <system code>`: Admin tool to get a rune item.
- `/identity get <profession|random> [texture]`: Admin tool to give a new identity key. You can provide a profession code or "random" to have one assigned upon first use. You can optionally provide a Base64 texture string.
- `/identity check`: Inspect the identity key in your hand or your currently equipped identity.
- `/identity unuse`: Unequip your current identity.
- `/identity stats get`: View your remaining unspent stat points.
- `/identity stats add <stat_type> <amount>`: Spend your stat points to upgrade specific attributes (e.g., strength, defense).

## 🔑 Permissions

- `mcidentity.admin`: Required for all administrative sub-commands including `/identity ability get`.

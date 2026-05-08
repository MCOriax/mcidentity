# MCOriax Identity Plugin

A robust and high-performance identity management system designed for Minecraft servers, with native support for the **Folia** regionalized multi-threading architecture.

## 🌟 Overview

The MCOriax Identity Plugin provides a complex "Identity" system where players can equip and manage different personas (Identities). Each identity is characterized by its **Sigil**, **Profession**, and **Stats**. 

This system is built with performance and security as top priorities, utilizing asynchronous database operations and RAM caching to ensure a lag-free experience on high-population servers.

### Key Features
- **Interactive GUI System**: Multi-tier virtual menus for managing identities, runes, and abilities.
- **Dynamic Data Binding**: Ability and Rune GUIs fetch real-time data from the database, showing equipped items or placeholders.
- **Regionalized Threading**: Fully compatible with Folia's `RegionScheduler` and `GlobalRegionScheduler`.
- **Database Connection Pooling**: High-performance data access using **HikariCP**.
- **Dual Database Support**: Supports both **SQLite** (for easy setup) and **PostgreSQL** (for production environments).
- **Extensible API**: Provides a central `MCIdentityProvider` for third-party plugins to interact with the identity system.
- **Secure Commands**: Role-based permissions for administrative actions.
- **Visual Identity Keys**: Identities are represented as physical items in the game with custom colored display names.

---

## 🛠 Developer API Usage

Third-party developers can integrate with the identity system using the `io.gitlab.mcoriax.common.MCIdentityProvider.java` class. Below are the available functions and their usage.

### Identity Provider Functions

<details>
<summary><code>getDatabase()</code></summary>

- **Description**: Gets the current identity database implementation.
- **Return Type**: `IMCIdentityDB`
- **Throws**: `IllegalStateException` if the database has not been initialized.
</details>

<details>
<summary><code>initializeSchema()</code></summary>

- **Description**: Initializes the database schema (creates necessary tables).
- **Return Type**: `CompletableFuture<Void>`
</details>

<details>
<summary><code>createSigil(String systemCode, String displayName)</code></summary>

- **Description**: Creates a new sigil type in the database.
- **Parameters**:
    - `systemCode`: Unique identifier for the sigil.
    - `displayName`: Human-readable name.
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
<summary><code>getSigils()</code></summary>

- **Description**: Retrieves all available sigils.
- **Return Type**: `CompletableFuture<Collection<Sigil>>`
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
    - `key`: The identity key (UUID, Sigil, Profession).
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
- **Data**: `Player`, `UUID (identity)`, `ItemStack (dropped item)`.

```java
public class IdentityDeathEvent extends MCIdentityEvent {
    // Getters for Player, UUID, ItemStack
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

---

## 🎮 GUI System

The plugin features a robust, multi-tier GUI system for a seamless player experience.

### Main Identity GUI (1-Row)
Accessed via `/identity` without arguments. It provides:
- **Slot 2**: Opens the **Ability GUI**.
- **Slot 4**: Interactive slot to equip/unequip your **Identity Key**.
- **Slot 6**: Opens the **Rune GUI**.

### Ability & Rune GUIs (6-Rows)
These GUIs are dynamically bound to the database:
- **Fetching**: Upon opening, the plugin fetches the currently equipped abilities/runes for the active identity.
- **Placeholders**: Empty slots are shown as **Red Stained Glass Panes**.
- **Equipped Items**: Equipped abilities/runes are shown as **Lime Stained Glass Panes** with their respective display names.
- **Security**: These menus are only accessible if the player has an active identity equipped.

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
- `/identity create <sigil|profession|ability> <name>`: Admin tool to create types.
- `/identity create <profession> <sigil>`: Admin tool to give a new identity key.
- `/identity check`: Inspect the identity key in your hand or your currently equipped identity.
- `/identity unuse`: Unequip your current identity.

## 🔑 Permissions

- `mcidentity.admin`: Required for all `create` sub-commands.

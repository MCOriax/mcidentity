# Bukkit Events & Integration

[← Back to README](../README.md)

The plugin provides several custom events located in the `io.github.mcoriax.mcidentity.bukkit.event` package. All events extend `MCIdentityEvent` and support Folia's asynchronous context. The event system works across all supported platforms — **SpigotMC**, **PaperMC**, and **Folia**.

## Custom Bukkit Events

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

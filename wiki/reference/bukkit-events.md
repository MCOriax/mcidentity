# Bukkit Events & Integration

[← Back to README](../../README.md)

The plugin provides custom events in the `io.github.mcoriax.mcidentity.bukkit.event` package. All events extend `MCIdentityEvent` and support Folia's asynchronous context. The event system works across all supported platforms — **SpigotMC**, **PaperMC**, and **Folia**.

## Custom Bukkit Events

<details>
<summary><code>MCIdentityEvent</code></summary>

- **Description**: The abstract base class for every MCIdentity event.
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
<summary><code>MCProfessionEquipEvent</code></summary>

- **Description**: Fired before a player puts on a profession, with `/identity wear` or the profession GUI. Nothing has changed yet when it fires.
- **Data**: `Player`, `String previousProfession` (the base `player` profession when they wore nothing), `String professionCode`.
- **Cancellable**: Yes. Cancelling keeps the current profession.

```java
public class MCProfessionEquipEvent extends MCIdentityEvent implements Cancellable {
    public Player getPlayer();
    public String getPreviousProfession();
    public String getProfessionCode();
}
```
</details>

<details>
<summary><code>MCProfessionUnequipEvent</code></summary>

- **Description**: Fired before a player takes off their profession with `/identity unwear` or the profession GUI, returning to the base `player` profession.
- **Data**: `Player`, `String professionCode` (the profession being taken off).
- **Cancellable**: Yes. Cancelling keeps the profession on.

```java
public class MCProfessionUnequipEvent extends MCIdentityEvent implements Cancellable {
    public Player getPlayer();
    public String getProfessionCode();
}
```
</details>

<details>
<summary><code>MCIdentityStatAddEvent</code></summary>

- **Description**: Fired when a stat is added to a player.
- **Data**: `Player` (nullable), `UUID playerUuid`, `String statKey`, `int amount`.
- **Cancellable**: Yes.

```java
public class MCIdentityStatAddEvent extends MCIdentityEvent implements Cancellable {
    public UUID getPlayerUuid();
    public String getStatKey();
    public int getAmount();
}
```
</details>

## Keying your own data

Personal data never moves when a profession changes: it stays under
`account_type = "player"` and `account_uuid = <player UUID>`. Listen to the profession
events only to react to the change itself (for example to refresh a profession
restricted feature), never to rebind data.

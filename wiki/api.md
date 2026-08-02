# Developer API Usage

[← Back to README](../README.md)

This guide explains how to integrate a **new custom plugin** (for example an
economy, bank, or membership system) with **MCIdentity**. The identity system
is exposed through the pure‑Java `api`/`common` modules and the Bukkit event
layer in `bukkit`, all published to GitHub Packages.

> Looking for the full event catalog? See [Bukkit Events & Integration](bukkit.md).

**Contents**

1. [Dependency Import](#1-dependency-import)
2. [API Usage](#2-api-usage)
3. [Bukkit Integration](#3-bukkit-integration)
4. [Architecture Warning — one identity is one account](#4-architecture-warning--one-identity-is-one-account)

---

## 1. Dependency Import

MCIdentity's library modules are published to the **GitHub Packages** Maven
registry for the repository `MCOriax/mcidentity`. GitHub Packages **always
requires authentication**, even to read public packages, so consumers must
supply a GitHub username and a Personal Access Token (classic) that has the
`read:packages` scope.

### 1.1 Configure the repository and credentials

Never hard‑code credentials in `build.gradle`. Read them from environment
variables or from your **`~/.gradle/gradle.properties`** (outside the project):

```properties
# ~/.gradle/gradle.properties
gpr.user=your-github-username
gpr.key=ghp_your_personal_access_token_with_read_packages
```

```groovy
// build.gradle
repositories {
    mavenCentral()

    maven {
        name = "MCIdentityGitHubPackages"
        url = uri("https://maven.pkg.github.com/MCOriax/mcidentity")
        credentials {
            // Falls back to the GITHUB_ACTOR / GITHUB_TOKEN env vars in CI.
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key")  ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
```

### 1.2 Declare the three dependencies (`compileOnly`)

MCIdentity is present on the server at runtime, so you compile against it but do
**not** bundle/shade it — use the `compileOnly` configuration for all three
artifacts. The published coordinates are group `io.github.mcoriax`, version
`4.0.0`:

```groovy
// build.gradle
dependencies {
    // MCIdentity — provided by the server at runtime, so compileOnly only.
    compileOnly 'io.github.mcoriax:mcidentity-api:4.0.0'      // interfaces & models
    compileOnly 'io.github.mcoriax:mcidentity-common:4.0.0'   // MCIdentityProvider + data layer
    compileOnly 'io.github.mcoriax:mcidentity-bukkit:4.0.0'   // Bukkit events & helpers

    // Your own server platform API (example)
    compileOnly 'org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT'
}
```

> **Changed:** these artifactIds used to be the bare module names `api`,
> `common`, and `bukkit`. Every plugin in the `io.github.mcoriax` group published
> under those same coordinates, so nothing could depend on this plugin and an
> add-on at once. The artifactId now matches the jar file name — for example
> `mcidentity-api-4.0.0.jar` resolves as `io.github.mcoriax:mcidentity-api`. The
> add-ons follow the same scheme: `mcidentity-skill-api`, `mcidentity-rune-api`,
> `mcidentity-cosmetic-api`, and so on.

### 1.3 Load order

So MCIdentity initializes before your plugin, declare it as a dependency in your
own `plugin.yml`:

```yaml
name: MyBankPlugin
main: com.example.bank.BankPlugin
version: 1.0.0
api-version: '1.20'
depend: [MCIdentity]        # use softdepend if MCIdentity is optional
```

---

## 2. API Usage

All identity data flows through
`io.github.mcoriax.mcidentity.common.MCIdentityProvider`, a singleton exposed as a public
static field and initialized by MCIdentity when its database comes up.

```java
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;

if (!MCIdentityProvider.hasDatabase()) {
    // MCIdentity has no database configured yet — do not touch the API.
    return;
}
MCIdentityProvider provider = MCIdentityProvider.instance;
```

> **Threading.** Every database operation returns a `CompletableFuture<…>` and
> completes on an asynchronous thread. Do the identity work inside the future's
> callbacks, but hop back to the server thread (Bukkit scheduler, or the
> region/global scheduler on Folia) before calling any thread‑confined Bukkit
> API such as inventories or entities.

> **One way in.** `MCIdentityProvider` (persistence) and `MCIdentityManager`
> (RAM state) are the only supported entry points. The storage backend is
> private to the provider and is never handed out, so there is no supported way
> to obtain an `IMCIdentityDB`, a `SQLiteDB`, or an `MCIdentityCentralServer`
> and call it directly. Anything those types can do is exposed on the provider;
> if something you need is missing, that is a gap in the facade rather than an
> invitation to reach past it.
>
> `MCIdentityManager.createDatabase(...)` is **deprecated** for the same reason:
> it returned a raw backend. Server owners are unaffected — the plugin wires
> storage itself — and any code that called it should move to
> `MCIdentityProvider.create(type, options)`, which builds the same backend and
> installs it as `MCIdentityProvider.instance`.

### 2.1 Fetch — read the active identity's data

`getActiveIdentity(playerUuid)` resolves the **identity UUID** the player is
currently playing as; the per‑identity data is then keyed by that identity UUID.

```java
import io.github.mcoriax.mcidentity.api.model.IdentityProperties;
import java.util.UUID;

UUID playerUuid = player.getUniqueId();

provider.getActiveIdentity(playerUuid).thenAccept(optIdentity -> {
    if (optIdentity.isEmpty()) {
        return;                          // player has no identity equipped
    }
    UUID identityUuid = optIdentity.get();

    provider.getProperties(identityUuid).thenAccept(optProps ->
        optProps.ifPresent(props -> {
            int level = props.getLevel();
            int exp   = props.getExp();
            // ... react to this identity's level/exp
        }));

    // A single stat value (see StatKeys for the allow-list, e.g. "strength"):
    provider.getStat(identityUuid, "strength")
            .thenAccept(strength -> { /* ... */ });
});
```

The full session (player UUID, active identity UUID, serialized key item) is
available via `getSession(playerUuid)`:

```java
import io.github.mcoriax.mcidentity.api.model.IdentitySession;

provider.getSession(playerUuid).thenAccept(optSession ->
    optSession.ifPresent(session -> {
        UUID identityUuid = session.getIdentityUniqueId();
        String contentKey = session.getContentKey();
    }));
```

### 2.2 Modify — persist changes to an identity

`IdentityProperties` values are snapshots. For storage fields use
`MutableIdentityProperties.copyOf(...)` and its setters; for `level`, `exp`, or
`statPoints`, build a fresh value and call `updateProperties`:

```java
import io.github.mcoriax.mcidentity.common.model.MutableIdentityProperties;

provider.getProperties(identityUuid).thenCompose(optProps -> {
    if (optProps.isEmpty()) {
        return java.util.concurrent.CompletableFuture.completedFuture(null);
    }
    IdentityProperties current = optProps.get();

    // Level up this identity, carrying the remaining fields over unchanged.
    MutableIdentityProperties leveled = new MutableIdentityProperties(
        current.getAlias(),       // alias (claimed separately, see below)
        current.getLevel() + 1,   // level
        0,                        // exp (reset)
        current.getStatPoints() + 5,
        current.getHeadText(),
        current.getInventoryContents(),
        current.getEnderchestContents()
    );
    return provider.updateProperties(identityUuid, leveled);
});

// The alias has its own operation: updateProperties never writes it, so the
// uniqueness check and the write always happen together. The alias must be
// alphanumeric, is unique across every identity, and that uniqueness ignores
// case — the future resolves to false when the name is malformed or taken.
// This call still renames or releases; it is /identity alias that claims a
// name once and leaves the base "player" identity alone.
provider.setIdentityAlias(identityUuid, "Alex").thenAccept(applied -> {
    if (!applied) { /* the alias is already held by another identity */ }
});
provider.getIdentityByAlias("alex").thenAccept(optIdentity -> { /* resolves "Alex" too */ });

// Atomic stat change (safe under concurrency), negative to subtract:
provider.addStat(identityUuid, "defense", 10);

// Per-identity storage — the inventory and ender chest are isolated per
// identity. Pass null for a storage you do not want to touch.
provider.updateIdentityStorage(identityUuid, base64Inventory, base64EnderChest);
provider.getIdentityInventory(identityUuid).thenAccept(base64 -> { /* restore inventory */ });
provider.getIdentityEnderchest(identityUuid).thenAccept(base64 -> { /* restore ender chest */ });
```

### 2.3 Manage — active identity, actions, and cooldowns

```java
import io.github.mcoriax.mcidentity.api.model.ActionType;

// Set (or clear, with null) the player's active identity + serialized key item.
provider.setActiveIdentity(playerUuid, identityUuid, base64KeyItem);

// Audit log — ActionType is EQUIP, UNEQUIP, DEATH_DROP, or ADMIN_REMOVE.
provider.logAction(playerUuid, identityUuid, ActionType.EQUIP);

// Swap cooldown helpers (RAM-only, synchronous):
if (!provider.isOnSwapCooldown(playerUuid)) {
    provider.setSwapCooldown(playerUuid);
}
```

### 2.4 Check the active profession — region whitelist / blacklist

`getActiveProfession(playerUuid)` returns the profession system code of the
player's currently active identity. Unlike the data getters above it is
**synchronous and RAM‑only** (backed by the active session cache), so it is safe
to call on hot paths such as a region‑entry check. It returns an empty
`Optional` when the player has no active session cached (offline, or between
identity swaps).

```java
import java.util.Optional;
import java.util.Set;

// Only these professions may enter the guarded region (whitelist).
private static final Set<String> ALLOWED = Set.of("guard", "fighter", "archer");

boolean mayEnter(UUID playerUuid) {
    Optional<String> profession = provider.getActiveProfession(playerUuid);
    // No active identity → deny (choose your own default for the "no identity" case).
    return profession.map(code -> ALLOWED.contains(code.toLowerCase()))
                     .orElse(false);
}

// Blacklist variant — everyone except merchants may enter:
private static final Set<String> DENIED = Set.of("merchant");

boolean isBlocked(UUID playerUuid) {
    return provider.getActiveProfession(playerUuid)
                   .map(code -> DENIED.contains(code.toLowerCase()))
                   .orElse(false);
}
```

> Profession codes are stored lower‑cased; compare case‑insensitively as shown.
> For an authoritative/offline lookup instead of the RAM cache, chain
> `getActiveIdentity(playerUuid)` → `getIdentityKey(identityUuid)` →
> `getProfessionCode()`.

<details>
<summary><strong>Full <code>MCIdentityProvider</code> method reference</strong></summary>

<details>
<summary><code>createProfession(String systemCode, String displayName)</code></summary>

- **Description**: Creates a new profession type in the database.
- **Parameters**:
    - `systemCode`: Unique identifier for the profession.
    - `displayName`: Human-readable name.
- **Return Type**: `CompletableFuture<Void>`
</details>

<details>
<summary><code>getProfessions()</code></summary>

- **Description**: Retrieves all available professions.
- **Return Type**: `CompletableFuture<Collection<Profession>>`
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

- **Description**: Retrieves current properties (alias, level, exp, etc.) for an identity.
- **Parameters**:
    - `identityUuid`: The UUID of the identity.
- **Return Type**: `CompletableFuture<Optional<IdentityProperties>>`
</details>

<details>
<summary><code>getIdentityByAlias(String alias)</code></summary>

- **Description**: Resolves the identity currently holding an alias. The lookup ignores case, so any casing of a claimed alias resolves to the same identity.
- **Parameters**:
    - `alias`: The alias to resolve.
- **Return Type**: `CompletableFuture<Optional<UUID>>`
</details>

<details>
<summary><code>setIdentityAlias(UUID identityUuid, String alias)</code></summary>

- **Description**: Claims an alias for an identity, or releases the current one when `alias` is `null`. The alias may contain only alphanumeric characters, is at most 32 characters long, and must not already be held by another identity — compared without regard to case. It is stored with the casing supplied here.
- **Parameters**:
    - `identityUuid`: The UUID of the identity.
    - `alias`: The alias to claim, or `null` to release.
- **Return Type**: `CompletableFuture<Boolean>` — `true` when applied, `false` when the alias is malformed or already taken.
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
<summary><code>getActiveIdentity(UUID playerUuid)</code></summary>

- **Description**: Gets the UUID of the identity currently equipped by a player.
- **Parameters**:
    - `playerUuid`: The UUID of the player.
- **Return Type**: `CompletableFuture<Optional<UUID>>`
</details>

<details>
<summary><code>getActiveProfession(UUID playerUuid)</code></summary>

- **Description**: Gets the profession system code of the player's currently active identity. Synchronous and RAM-only (reads the active session cache), so it is safe on hot paths such as region profession whitelist/blacklist checks. Empty when no active session is cached.
- **Parameters**:
    - `playerUuid`: The UUID of the player.
- **Return Type**: `Optional<String>`
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
<summary><code>updateSessionContentKey(UUID playerUuid, String contentKey)</code></summary>

- **Description**: Rewrites only the serialized identity item stored on a player's active session, leaving the equipped identity unchanged, and refreshes the RAM cache if active.
- **Parameters**:
    - `playerUuid`: The UUID of the player.
    - `contentKey`: The new Base64 serialized item stack.
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
<summary><code>getIdentityInventory(UUID identityUuid)</code></summary>

- **Description**: Reads the serialized inventory contents stored for an identity. Each identity keeps its own isolated inventory.
- **Parameters**:
    - `identityUuid`: The UUID of the identity.
- **Return Type**: `CompletableFuture<String>` (Base64 inventory, or null when unset)
</details>

<details>
<summary><code>getIdentityEnderchest(UUID identityUuid)</code></summary>

- **Description**: Reads the serialized ender chest contents stored for an identity. Each identity keeps its own isolated ender chest.
- **Parameters**:
    - `identityUuid`: The UUID of the identity.
- **Return Type**: `CompletableFuture<String>` (Base64 ender chest, or null when unset)
</details>

<details>
<summary><code>updateIdentityStorage(UUID identityUuid, String inventoryContents, String enderchestContents)</code></summary>

- **Description**: Persists the inventory and/or ender chest contents for an identity, carrying the other properties over unchanged. Pass null for a storage to leave it untouched.
- **Parameters**:
    - `identityUuid`: The UUID of the identity.
    - `inventoryContents`: The new Base64 inventory, or null to skip.
    - `enderchestContents`: The new Base64 ender chest, or null to skip.
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

</details>

---

## 3. Bukkit Integration

MCIdentity fires custom Bukkit events from the
`io.github.mcoriax.mcidentity.bukkit.event` package. Every event extends
`MCIdentityEvent` (which extends `org.bukkit.event.Event`) and works across
**SpigotMC**, **PaperMC**, and **Folia**. The two events most relevant to an
economy/membership plugin are `MCIdentityEquipEvent` and
`MCIdentityUnequipEvent` — the exact moments to load and unload the data bound
to an identity.

### 3.1 Implement a listener

```java
package com.example.bank;

import io.github.mcoriax.mcidentity.bukkit.event.MCIdentityEquipEvent;
import io.github.mcoriax.mcidentity.bukkit.event.MCIdentityUnequipEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

public class IdentityBankListener implements Listener {

    private final BankService bank;

    public IdentityBankListener(BankService bank) {
        this.bank = bank;
    }

    // Fired when a player equips an identity. Load THAT identity's account.
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEquip(MCIdentityEquipEvent event) {
        Player player       = event.getPlayer();
        UUID   identityUuid = event.getIdentityUuid();   // bind data to THIS uuid
        // event.getIdentityItem() is the key ItemStack being equipped.

        bank.loadAccount(identityUuid);
    }

    // Fired when a player unequips an identity. Persist & unload its account.
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onUnequip(MCIdentityUnequipEvent event) {
        Player player       = event.getPlayer();
        UUID   identityUuid = event.getIdentityUuid();

        bank.saveAndUnload(identityUuid);
    }
}
```

Register it in your plugin's `onEnable`:

```java
getServer().getPluginManager().registerEvents(
        new IdentityBankListener(bankService), this);
```

**Cancelling.** Both events implement `Cancellable`. Calling
`event.setCancelled(true)` vetoes the equip/unequip — useful if, for example, a
membership must be active before an identity can be worn.

**Folia / async safety.** `MCIdentityEvent` marks itself asynchronous whenever
it is fired off the primary thread (`super(!Bukkit.isPrimaryThread())`). Keep
your handlers thread‑safe and, when you must call main‑thread‑only Bukkit API,
schedule back onto the appropriate scheduler instead of calling it directly.

### 3.2 Event reference — append this to your event docs

> The block below documents the two events in the same style as the other
> entries and can be appended to your API/event documentation. The complete
> catalog (creation, death, stat add, and the pre‑equip veto event) lives in
> [Bukkit Events & Integration](bukkit.md).

<details>
<summary><code>MCIdentityEquipEvent</code></summary>

- **Package**: `io.github.mcoriax.mcidentity.bukkit.event`
- **Description**: Fired when a player equips an identity.
- **Cancellable**: Yes (`setCancelled(true)` blocks the equip).
- **Constructor**: `MCIdentityEquipEvent(Player player, UUID identityUuid, ItemStack identityItem)`
- **Accessors**:
    - `getPlayer()` → `Player` — the player equipping the identity.
    - `getIdentityUuid()` → `UUID` — the identity being equipped (bind your data to this).
    - `getIdentityItem()` → `ItemStack` — the identity key item.
    - `isCancelled()` / `setCancelled(boolean)`
</details>

<details>
<summary><code>MCIdentityUnequipEvent</code></summary>

- **Package**: `io.github.mcoriax.mcidentity.bukkit.event`
- **Description**: Fired when a player unequips an identity.
- **Cancellable**: Yes (`setCancelled(true)` blocks the unequip).
- **Constructor**: `MCIdentityUnequipEvent(Player player, UUID identityUuid)`
- **Accessors**:
    - `getPlayer()` → `Player` — the player unequipping the identity.
    - `getIdentityUuid()` → `UUID` — the identity being unequipped.
    - `isCancelled()` / `setCancelled(boolean)`
</details>

---

## 4. Architecture Warning — one identity is one account

> [!WARNING]
> **Each identity is an entirely separate character. Bind your data to the
> Identity UUID, never to the base Player UUID.**

A single player owns **many** identities, and each identity has its own distinct
UUID (its `IdentityKey`). Only one identity is "active" at a time, and switching
identities is a full context switch — as if the player logged into a completely
different account. Treat every identity UUID as a separate account.

**Do this — key data by the Identity UUID:**

```java
// CORRECT: the balance belongs to the identity, not the human behind it.
UUID identityUuid = event.getIdentityUuid();          // from equip/unequip events
bank.setBalance(identityUuid, 1000);
```

**Never do this — keying by the Player UUID:**

```java
// WRONG: every identity this player owns would share ONE balance,
// membership, or rank — collapsing distinct characters into one account.
bank.setBalance(player.getUniqueId(), 1000);
```

Consequences of keying by `Player#getUniqueId()`:

- Bank balances, membership statuses, ranks, and cooldowns leak across
  characters that are meant to be independent.
- Money or perks earned on one identity appear on all of the player's other
  identities.
- Per‑character progression and economy balancing become impossible.

**Rules for third‑party plugins:**

1. Resolve the current identity with
   `MCIdentityProvider.instance.getActiveIdentity(playerUuid)` (or read
   `event.getIdentityUuid()` from the events above) and use that UUID as the
   primary key for **all** of your stored data.
2. Load a character's data on `MCIdentityEquipEvent` and flush/unload it on
   `MCIdentityUnequipEvent`.
3. Even the default state is its own identity with its own UUID — there is no
   "no identity" account to fall back to the player UUID for. If no identity is
   active, hold your operation until one is equipped.

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

### 1.2 Declare the three dependencies (`compileOnly`, version `1.0.0`)

MCIdentity is present on the server at runtime, so you compile against it but do
**not** bundle/shade it — use the `compileOnly` configuration for all three
artifacts. The published coordinates are group `io.github.mcoriax`, version
`1.0.0`:

```groovy
// build.gradle
dependencies {
    // MCIdentity — provided by the server at runtime, so compileOnly only.
    compileOnly 'io.github.mcoriax:api:1.0.0'      // interfaces & models
    compileOnly 'io.github.mcoriax:common:1.0.0'   // MCIdentityProvider + data layer
    compileOnly 'io.github.mcoriax:bukkit:1.0.0'   // Bukkit events & helpers

    // Your own server platform API (example)
    compileOnly 'org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT'
}
```

> The jar file names are `mcidentity-api-1.0.0.jar`, `mcidentity-common-1.0.0.jar`,
> and `mcidentity-bukkit-1.0.0.jar` (set via `archivesName`), but the resolvable
> **Maven artifactIds** are `api`, `common`, and `bukkit` as shown above.

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
`io.github.mcoriax.common.MCIdentityProvider`, a singleton exposed as a public
static field and initialized by MCIdentity when its database comes up.

```java
import io.github.mcoriax.common.MCIdentityProvider;

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

### 2.1 Fetch — read the active identity's data

`getActiveIdentity(playerUuid)` resolves the **identity UUID** the player is
currently playing as; the per‑identity data is then keyed by that identity UUID.

```java
import io.github.mcoriax.api.model.IdentityProperties;
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
import io.github.mcoriax.api.model.IdentitySession;

provider.getSession(playerUuid).thenAccept(optSession ->
    optSession.ifPresent(session -> {
        UUID identityUuid = session.getIdentityUniqueId();
        String contentKey = session.getContentKey();
    }));
```

### 2.2 Modify — persist changes to an identity

`IdentityProperties` values are snapshots. For cosmetic/storage fields use
`MutableIdentityProperties.copyOf(...)` and its setters; for `level`, `exp`, or
`statPoints`, build a fresh value and call `updateProperties`:

```java
import io.github.mcoriax.common.model.MutableIdentityProperties;

provider.getProperties(identityUuid).thenCompose(optProps -> {
    if (optProps.isEmpty()) {
        return java.util.concurrent.CompletableFuture.completedFuture(null);
    }
    IdentityProperties current = optProps.get();

    // Level up this identity, carrying the remaining fields over unchanged.
    MutableIdentityProperties leveled = new MutableIdentityProperties(
        current.getLevel() + 1,   // level
        0,                        // exp (reset)
        current.getStatPoints() + 5,
        current.getHeadText(),
        current.getSkin(),
        current.getAura(),
        current.getInventoryContents(),
        current.getEnderchestContents()
    );
    return provider.updateProperties(identityUuid, leveled);
});

// Atomic stat change (safe under concurrency), negative to subtract:
provider.addStat(identityUuid, "defense", 10);

// Cosmetics:
provider.saveSkin(identityUuid, base64SkinTexture);
provider.saveAura(identityUuid, "aura_flame");
```

### 2.3 Manage — active identity, actions, and cooldowns

```java
import io.github.mcoriax.api.model.ActionType;

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
<summary><code>createAbility(String abilityCode, String displayName, String abilitySkin)</code></summary>

- **Description**: Creates a new ability type.
- **Parameters**:
    - `abilityCode`: Unique identifier for the ability, mapping to the MythicMobs ability ID.
    - `displayName`: Human-readable name.
    - `abilitySkin`: The model ID rendered for the ability, or null for the default skin.
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
- **Return Type**: `CompletableFuture<Map<Integer, EquippedAbility>>` (Slot number -> `abilityCode` + `abilitySkin`)
</details>

<details>
<summary><code>setEquippedAbility(UUID identityUuid, int slot, String abilityCode, String abilitySkin)</code></summary>

- **Description**: Equips or clears an ability in a specific slot.
- **Parameters**:
    - `identityUuid`: The UUID of the identity.
    - `slot`: The slot number.
    - `abilityCode`: The ability code to equip (MythicMobs ability ID, null to clear).
    - `abilitySkin`: The skin model ID stored with the slot, or null for the default skin.
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

</details>

---

## 3. Bukkit Integration

MCIdentity fires custom Bukkit events from the
`io.github.mcoriax.bukkit.event` package. Every event extends
`MCIdentityEvent` (which extends `org.bukkit.event.Event`) and works across
**SpigotMC**, **PaperMC**, and **Folia**. The two events most relevant to an
economy/membership plugin are `MCIdentityEquipEvent` and
`MCIdentityUnequipEvent` — the exact moments to load and unload the data bound
to an identity.

### 3.1 Implement a listener

```java
package com.example.bank;

import io.github.mcoriax.bukkit.event.MCIdentityEquipEvent;
import io.github.mcoriax.bukkit.event.MCIdentityUnequipEvent;
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
> catalog (creation, death, rune/ability/skin/aura set, stat add, and the
> pre‑equip veto event) lives in [Bukkit Events & Integration](bukkit.md).

<details>
<summary><code>MCIdentityEquipEvent</code></summary>

- **Package**: `io.github.mcoriax.bukkit.event`
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

- **Package**: `io.github.mcoriax.bukkit.event`
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

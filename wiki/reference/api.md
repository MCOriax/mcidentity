# Developer API Usage

[← Back to README](../../README.md)

This guide explains how to integrate a **new custom plugin** (for example an
economy, bank, or membership system) with **MCIdentity**. The profession system
is exposed through the pure‑Java `api`/`common` modules and the Bukkit event
layer in `bukkit`, all published to GitHub Packages.

> Looking for the full event catalog? See [Bukkit Events & Integration](bukkit-events.md).

**Contents**

1. [Dependency Import](#1-dependency-import)
2. [API Usage](#2-api-usage)
3. [Bukkit Integration](#3-bukkit-integration)
4. [Keying your data — one player is one account](#4-keying-your-data--one-player-is-one-account)

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
`5.0.0`:

```groovy
// build.gradle
dependencies {
    // MCIdentity — provided by the server at runtime, so compileOnly only.
    compileOnly 'io.github.mcoriax:mcidentity-api:5.0.0'      // interfaces & models
    compileOnly 'io.github.mcoriax:mcidentity-common:5.0.0'   // MCIdentityProvider + data layer
    compileOnly 'io.github.mcoriax:mcidentity-bukkit:5.0.0'   // Bukkit events & helpers

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

All profession data flows through
`io.github.mcoriax.mcidentity.common.MCIdentityProvider`, a singleton exposed as a public
static field and initialized by MCIdentity when its database comes up. It implements
`io.github.mcoriax.mcidentity.api.IdentityProvider`.

```java
import io.github.mcoriax.mcidentity.common.MCIdentityProvider;

if (!MCIdentityProvider.hasDatabase()) {
    // MCIdentity has no database configured yet — do not touch the API.
    return;
}
MCIdentityProvider provider = MCIdentityProvider.instance;
```

> **Threading.** Every storage operation returns a `CompletableFuture<…>` and
> completes on an asynchronous thread. Hop back to the server thread (Bukkit
> scheduler, or the region or entity scheduler on Folia) before calling any
> thread confined Bukkit API.

Everything is keyed by the **player's own UUID**. There are no identity UUIDs.

### 2.1 Read the worn profession

```java
// Synchronous RAM read for an online player; empty when no session is loaded.
Optional<String> worn = provider.getActiveProfession(player.getUniqueId());

// Storage read, for an offline player too; empty means the base "player" profession.
provider.loadActiveProfession(playerUuid).thenAccept(code -> { /* ... */ });
```

`getActiveProfession` never touches the database, so it is safe on any thread and
cheap enough for event handlers (for example a region whitelist that only lets
miners in).

### 2.2 Read progress and stats

```java
provider.getProgress(playerUuid, "miner").thenAccept(opt -> opt.ifPresent(progress -> {
    int level = progress.getLevel();
    int exp = progress.getExp();
}));

provider.getStats(playerUuid).thenAccept(opt -> opt.ifPresent(stats -> {
    int points = stats.getStatPoints();
    int strength = stats.getStrength();
}));

provider.getStat(playerUuid, "luck").thenAccept(luck -> { /* ... */ });
```

### 2.3 Change things

| Method | Effect |
|---|---|
| `setActiveProfession(UUID, String)` | Stores the worn profession and refreshes the online session, loading that profession's progress (level 1 when never worn). Fires no event; `/identity wear` is the player facing path and fires `MCProfessionEquipEvent`. |
| `clearActiveProfession(UUID)` | Wears the base `player` profession. |
| `updateProgress(UUID, String, ProfessionProgress)` | Stores one profession's level and EXP. |
| `updateStats(UUID, PlayerStats)` | Stores the stat points and nine stats. |
| `addStat(UUID, String, int)` | Adds to one stat; unknown stat keys are ignored. |
| `logAction(UUID, String, ActionType)` | Appends an `EQUIP` or `UNEQUIP` audit row. |
| `createProfession(String, String)` / `getProfessions()` | The profession registry. |

Writes refresh the RAM session of an online player once storage confirms them.

### 2.4 Swap cooldown

`isOnSwapCooldown`, `setSwapCooldown`, `clearSwapCooldown` and
`getRemainingSwapCooldownMillis` expose the cooldown between profession changes,
configured with `profession_swap_cooldown`.

---

## 3. Bukkit Integration

Depend on `mcidentity-bukkit` and listen to the events in
`io.github.mcoriax.mcidentity.bukkit.event`:

```java
public final class MinerOnlyListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onWear(MCProfessionEquipEvent event) {
        if (event.getProfessionCode().equals("fighter") && inPeacefulZone(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("You cannot become a fighter here.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onUnwear(MCProfessionUnequipEvent event) {
        // event.getProfessionCode() is the profession being taken off.
    }
}
```

Both events fire before the change and are cancellable. The full catalogue is in
[Bukkit Events & Integration](bukkit-events.md).

---

## 4. Keying your data — one player is one account

A player is one account. Bind personal data to the generic account key
`(account_type, account_uuid)` with `account_type = "player"` and
`account_uuid = <player UUID>`. Other account types (a clan, a guild) keep their own
`account_type`.

Wearing a different profession never changes the account, so a wallet, a membership or
a backpack stays the same whatever the player wears. Read the worn profession only to
gate a feature (for example a skill only miners may cast), never to choose where data
is stored.
